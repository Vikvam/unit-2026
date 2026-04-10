-- Fix: fully-qualify gen_random_bytes as extensions.gen_random_bytes.
--
-- Supabase installs pgcrypto into the `extensions` schema, not `public`.
-- The original `create_linking_code` function was declared with
-- `set search_path = public`, which excludes `extensions`, so the unqualified
-- call failed at runtime with:
--   function gen_random_bytes(integer) does not exist
--
-- Fully-qualifying keeps the restrictive search_path (important for
-- SECURITY DEFINER functions) while still resolving the extension function.

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

  loop
    v_attempt := v_attempt + 1;
    v_bytes := extensions.gen_random_bytes(6);
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
