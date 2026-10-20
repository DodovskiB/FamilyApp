# Accounts and sharing

Approved 2026-10-21. Preserve the working Kotlin/Compose/Room application.

## Product decisions

- Google sign-in, Supabase Free for the private pilot. No paid plan or billing activation.
- One Owner per list; invited members are Admins. No Viewer role.
- Both roles read all list items, add/edit/remove items, adjust quantities, check/uncheck,
  rename the list and clear purchased items. Only the Owner invites/removes members
  and deletes the whole list. Membership grants access only to the selected list.
- No arbitrary 20-member cap; provider quotas still apply. Do not promise unlimited free use.
- Start with in-app invitations addressed to the signed-in email. Optional manual sharing
  of an invitation through the Android share sheet; automatic email delivery is not enabled.
- A personal catalog is not automatically exposed to list members. Product information
  attached to a shared list, including photos/links, is available to its authorized members.
- Purchased items stay in a Purchased section. Unchecking restores an item to outstanding.
  Clear purchased is a confirmed long-press list action, available to both roles, and
  removes only purchased entries, never catalog products. Re-adding an already purchased
  product asks whether it is needed again and uses the newly selected quantity.
  These purchased-item changes are approved but not yet implemented.

## First implementation: authentication and local isolation

- `AccountRepository` uses Supabase Auth with Google through the system browser/custom
  tab and PKCE; callback is `com.example.familyapp://auth-callback`.
- URL and publishable/anon key come from ignored `local.properties`. No secret/service_role
  key or Google client secret belongs in the APK or Git. SDK logging is disabled.
- `SecureAuthStorage` encrypts sessions and PKCE verifiers with an Android Keystore key.
  Files are in noBackupFilesDir; invalid/corrupt ciphertext requires a fresh sign-in.
- Account/profile is in the hamburger menu. Sign-out clears local credentials even if
  remote revocation fails. Authenticated account data is hidden when signed out.
- The first authenticated account is associated with the existing `familyapp.db` locally.
  Another account gets its own database, named from a hash of its Supabase user UUID.
  Account selection does not copy, erase or upload the existing database or photos.
- Before backend configuration, existing local use remains available. Once data has an
  account owner, a sign-in is required to display it, even if configuration is later removed.
- A successful Google login is not proof of cloud synchronization. This phase does not
  implement remote list ownership, uploads, memberships or live updates.

## Cloud setup

1. Create a personal Free organization/project owned by the app owner, in an EU region.
2. In Google Cloud, create the FamilyApp OAuth application and a **Web application** client
   for the browser-based flow. The authorized redirect is the Supabase project callback
   `https://PROJECT_REF.supabase.co/auth/v1/callback`.
3. Configure the Google provider in Supabase using that client's ID and secret. Keep
   the client secret only in the provider configuration, never Android source/properties.
4. Add the exact mobile callback `com.example.familyapp://auth-callback` to allowed redirect URLs.
5. Configure the consent screen/testing audience as needed for the invited pilot accounts.
6. Add these settings to existing ignored local.properties, preserving sdk.dir:

   ```properties
   supabase.url=https://PROJECT_REF.supabase.co
   supabase.publishableKey=YOUR_PUBLISHABLE_KEY
   ```

7. Build, update-install, test sign-in, cancel, app restart, sign-out and account isolation.

## Next gates

1. Authenticate on the physical phone; verify preserved local lists and another account's isolation.
2. Add remote list/item data and server-enforced access rules with denial tests before uploading data.
3. Add one-time idempotent local-data import and synchronize the same account on two phones.
4. Implement invitation acceptance and Owner/Admin membership tests with distinct accounts.
5. Verify offline queues, retries, concurrent quantities/checks, deleted-item handling and revocation.
6. Share private photos/links, implement purchased cleanup and test with the small invited circle.

Supabase Free has limited storage/traffic/realtime usage and may pause inactive projects.
Switching to a paid plan requires the owner's agreement. No GitHub push without approval.

## Validation

`:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest` passed on
2026-10-21. The two account database-name unit tests passed (plus the existing example test).
The initial terminal directory-access problem was resolved after Android Studio sync.
Supabase Kotlin 3.2.6/Ktor 3.3.1 are pinned to remain compatible with the existing Kotlin
2.2 toolchain; SDK logs are disabled. Android minSdk remains 24 with core-library desugaring.

A validated private backup of the Note 20 app's databases/files/preferences was made
before update-installing. Both app/test APK update-installs succeeded. Direct instrumentation
on Note 20 passed **8 tests**: 3 account isolation/encrypted storage/callback tests,
2 catalog persistence/migration tests, and 3 shopping-list persistence/concurrency/migration
tests. These use isolated preferences, files and databases, not the owner's live records.
Google sign-in acceptance remains required before calling authentication complete.
Cloud synchronization is not implemented.
