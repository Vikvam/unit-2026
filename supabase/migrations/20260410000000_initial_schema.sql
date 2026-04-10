-- OpenJetTracks — initial Supabase schema
--
-- Replaces the in-memory Ktor sync server with a durable Postgres + Realtime backend.
--
-- Design notes:
--   * Anonymous Supabase auth issues one auth.users row per install.
--   * An `accounts` row represents "the thing that devices sync to". One install
--     (auth.uid()) joins exactly one account via `account_members`. Linking a
--     second device = moving its membership from its solo account to the target
--     account, discarding the now-orphaned solo account.
--   * `tracking_sessions` is one row per session (not one row per account), with
--     a partial unique index enforcing at most one active session per account.
--     This future-proofs session history: stopped rows are history rows, no
--     separate table or migration required later.
--   * Pause extension (targetEndAtMs += now - pausedAtMs) is computed client-side
--     and written in the resume UPDATE. This preserves correct offline semantics:
--     a device that resumed while disconnected knows the real resume time, which
--     a DB-side now() computation would not.
--   * All account/linking writes go through SECURITY DEFINER RPCs; the tables
--     have RLS enabled but no direct write policies, so clients cannot bypass
--     the invariants the RPCs enforce.

create extension if not exists pgcrypto;

-- ============================================================================
-- Tables
-- ============================================================================

create table accounts (
  id         uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now()
);

create table account_members (
  account_id uuid not null references accounts(id) on delete cascade,
  user_id    uuid not null references auth.users(id) on delete cascade,
  joined_at  timestamptz not null default now(),
  primary key (account_id, user_id)
);

create index account_members_user_id_idx on account_members(user_id);

create table linking_codes (
  code       text primary key,
  account_id uuid not null references accounts(id) on delete cascade,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  redeemed   boolean not null default false
);

create index linking_codes_account_id_idx on linking_codes(account_id);

create table tracking_sessions (
  id               uuid primary key default gen_random_uuid(),
  account_id       uuid not null references accounts(id) on delete cascade,
  started_at_ms    bigint not null,
  target_end_at_ms bigint not null,
  stopped_at_ms    bigint,
  paused_at_ms     bigint,
  label            text,
  updated_at       timestamptz not null default now()
);

-- At most one active session per account. Losing INSERTs in a race will hit
-- this and the client reconciles against the winner.
create unique index tracking_sessions_one_active_per_account
  on tracking_sessions(account_id)
  where stopped_at_ms is null;

-- History queries: "latest sessions for this account, newest first".
create index tracking_sessions_history_by_account
  on tracking_sessions(account_id, started_at_ms desc);

-- Auto-bump updated_at on UPDATE.
create or replace function set_updated_at() returns trigger
  language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create trigger tracking_sessions_set_updated_at
  before update on tracking_sessions
  for each row execute function set_updated_at();

-- ============================================================================
-- Row Level Security
-- ============================================================================

alter table accounts         enable row level security;
alter table account_members  enable row level security;
alter table linking_codes    enable row level security;
alter table tracking_sessions enable row level security;

-- account_members: the caller can see their own membership rows. Deliberately
-- narrow — this policy is the anchor that every other policy's subquery
-- resolves through, so it must not recursively re-query account_members.
create policy account_members_self_read on account_members
  for select using (user_id = auth.uid());

-- accounts: visible if the caller has a membership in it. Safe because the
-- subquery on account_members resolves through account_members_self_read.
create policy accounts_member_read on accounts
  for select using (
    exists (
      select 1 from account_members
      where account_members.account_id = accounts.id
        and account_members.user_id = auth.uid()
    )
  );

-- linking_codes: NO policies. RLS is enabled → default deny. Access only
-- through the SECURITY DEFINER create_linking_code / redeem_linking_code RPCs.

-- tracking_sessions: members of the owning account have full CRUD.
create policy tracking_sessions_member_select on tracking_sessions
  for select using (
    exists (
      select 1 from account_members
      where account_members.account_id = tracking_sessions.account_id
        and account_members.user_id = auth.uid()
    )
  );

create policy tracking_sessions_member_insert on tracking_sessions
  for insert with check (
    exists (
      select 1 from account_members
      where account_members.account_id = tracking_sessions.account_id
        and account_members.user_id = auth.uid()
    )
  );

create policy tracking_sessions_member_update on tracking_sessions
  for update using (
    exists (
      select 1 from account_members
      where account_members.account_id = tracking_sessions.account_id
        and account_members.user_id = auth.uid()
    )
  ) with check (
    exists (
      select 1 from account_members
      where account_members.account_id = tracking_sessions.account_id
        and account_members.user_id = auth.uid()
    )
  );

create policy tracking_sessions_member_delete on tracking_sessions
  for delete using (
    exists (
      select 1 from account_members
      where account_members.account_id = tracking_sessions.account_id
        and account_members.user_id = auth.uid()
    )
  );

-- Realtime: clients subscribe via postgres_changes. The supabase_realtime
-- publication needs tracking_sessions added so CDC events are emitted.
alter publication supabase_realtime add table tracking_sessions;

-- ============================================================================
-- RPCs
-- ============================================================================

-- ensure_account: idempotent bootstrap. Returns the caller's account_id,
-- creating a solo account on first call. Safe to call on every sign-in.
create or replace function ensure_account()
  returns uuid
  language plpgsql
  security definer
  set search_path = public
as $$
declare
  v_account_id uuid;
begin
  if auth.uid() is null then
    raise exception 'not authenticated' using errcode = '28000';
  end if;

  select account_id into v_account_id
  from account_members
  where user_id = auth.uid()
  limit 1;

  if v_account_id is not null then
    return v_account_id;
  end if;

  insert into accounts default values returning id into v_account_id;
  insert into account_members(account_id, user_id)
    values (v_account_id, auth.uid());

  return v_account_id;
end;
$$;

grant execute on function ensure_account() to authenticated;

-- create_linking_code: generates a 6-char code tied to the caller's account.
-- Codes expire in 5 minutes and are single-use.
--
-- Character set deliberately excludes visually ambiguous glyphs (0/O, 1/I/l)
-- so a user can read the code off one screen and type it into another.
create or replace function create_linking_code()
  returns text
  language plpgsql
  security definer
  set search_path = public
as $$
declare
  v_charset     constant text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';  -- 30 chars
  v_charset_len constant int  := 30;
  v_account_id  uuid;
  v_code        text;
  v_bytes       bytea;
  v_attempt     int := 0;
begin
  if auth.uid() is null then
    raise exception 'not authenticated' using errcode = '28000';
  end if;

  select account_id into v_account_id
  from account_members
  where user_id = auth.uid()
  limit 1;

  if v_account_id is null then
    raise exception 'caller has no account; call ensure_account() first'
      using errcode = 'P0001';
  end if;

  -- Collision is astronomically unlikely (30^6 = 729M codes, 5-min TTL) but
  -- retry a few times for correctness.
  loop
    v_attempt := v_attempt + 1;
    v_bytes := gen_random_bytes(6);
    v_code := '';
    for i in 0..5 loop
      v_code := v_code || substr(
        v_charset,
        1 + (get_byte(v_bytes, i) % v_charset_len),
        1
      );
    end loop;

    begin
      insert into linking_codes(code, account_id, expires_at)
        values (v_code, v_account_id, now() + interval '5 minutes');
      return v_code;
    exception when unique_violation then
      if v_attempt >= 5 then
        raise exception 'could not generate unique linking code';
      end if;
    end;
  end loop;
end;
$$;

grant execute on function create_linking_code() to authenticated;

-- redeem_linking_code: joins the caller to the code's account, discarding
-- the caller's previous solo account if it becomes orphaned.
--
-- Idempotent: if the caller is already a member of the target account, the
-- code is still consumed and the function returns the account id.
--
-- NOTE: any active session owned by the caller's previous solo account is
-- dropped via ON DELETE CASCADE when the orphan account is removed. The
-- client should warn the user before calling this if a local session is
-- running.
create or replace function redeem_linking_code(p_code text)
  returns uuid
  language plpgsql
  security definer
  set search_path = public
as $$
declare
  v_target_account_id uuid;
  v_caller_account_id uuid;
  v_normalized_code   text := upper(p_code);
begin
  if auth.uid() is null then
    raise exception 'not authenticated' using errcode = '28000';
  end if;

  -- Lock the code row so concurrent redemptions serialize.
  select account_id into v_target_account_id
  from linking_codes
  where code = v_normalized_code
    and not redeemed
    and expires_at > now()
  for update;

  if v_target_account_id is null then
    raise exception 'invalid or expired code' using errcode = 'P0001';
  end if;

  select account_id into v_caller_account_id
  from account_members
  where user_id = auth.uid()
  limit 1;

  -- Already linked → consume the code and return success.
  if v_caller_account_id = v_target_account_id then
    update linking_codes set redeemed = true where code = v_normalized_code;
    return v_target_account_id;
  end if;

  -- Detach caller from their current account.
  if v_caller_account_id is not null then
    delete from account_members
    where user_id = auth.uid()
      and account_id = v_caller_account_id;

    -- Drop the old account if no one else is using it. ON DELETE CASCADE on
    -- tracking_sessions and linking_codes cleans up any stale rows.
    delete from accounts
    where id = v_caller_account_id
      and not exists (
        select 1 from account_members where account_id = v_caller_account_id
      );
  end if;

  insert into account_members(account_id, user_id)
    values (v_target_account_id, auth.uid());

  update linking_codes set redeemed = true where code = v_normalized_code;

  return v_target_account_id;
end;
$$;

grant execute on function redeem_linking_code(text) to authenticated;
