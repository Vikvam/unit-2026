# Before Launch

Checklist of things to address before shipping the app to users. Items here
are intentionally deferred during development to keep iteration fast.

## Supabase

- [ ] **Enable Captcha on anonymous sign-ins.** Supabase Dashboard →
  Authentication → Sign In / Providers → Anonymous Sign-Ins. Protects
  against:
  - Scripted `signInAnonymously` loops inflating MAU billing.
  - Bots filling `auth.users` and the `accounts` table with garbage rows.
  Skipped during development because it adds friction to device testing.

## Build / dependencies

- [ ] **Remove the `kotlinx-datetime` force to `0.6.2` in root
  `build.gradle.kts`.** Added as a workaround for supabase-kt 3.1.4
  referencing `kotlinx.datetime.serializers.InstantIso8601Serializer`
  (removed in 0.7.x). Revisit when supabase-kt 3.2.x goes stable — 3.2.x
  migrated to `kotlin.time.Instant` and is compatible with kotlinx-datetime
  0.7.1, which Compose Material3 1.10.0-alpha05 wants anyway. Bumping
  supabase-kt lets Compose Material3's date picker API work without
  downgrading its transitives.
