# Same-account synchronization — 2026-10-21

Shopping lists, catalog names/categories/links, list entries, quantities and purchased
status synchronize through the existing Supabase Free project. This stage connects
devices signed in to the **same Google account**. Invitations and Owner/Admin sharing
between different accounts are still the next stage.

## Local data and retry behavior

The existing Room v5 entities, DAOs and repository remain in place. Auxiliary SQLite
tables map numeric local IDs to cloud UUIDs and keep a durable operation queue.
SQLite triggers capture mutations in the same transaction as DAO writes, including
deletions. Downloads suppress those triggers to avoid upload echoes. Existing data
is imported once with stable operation IDs. A lost response can be retried without
applying a quantity increment twice. Pending edits survive reopening the database.

Sync runs every five seconds **while the app is open**, using the expected account's
token. Offline changes remain local until connectivity returns and the app is open.
Account displays status and pending count. There is no background service.
Untouched empty-install defaults are replaced by existing cloud lists rather than
duplicated. Custom lists and existing products are retained.

Product photos stay on the phone. Downloads preserve existing local photo references;
photo files are not uploaded. Google sign-in and local account/database isolation
continue to use the previous implementation.

## Server model and conflicts

Canonical migration: `supabase/migrations/20261021_owner_sync.sql`.
Two private tables store current records/tombstones and acknowledged operation IDs.
Authenticated clients can read their own records; direct client writes are denied.
The RPC functions derive ownership from `auth.uid()`, validate field allowlists and
serialize each account's changes/cursor reads with a transaction lock. The APK has
only the existing publishable key, never a service-role key.

Independent field edits merge. Quantity changes use deltas; simultaneous increments
are retained. Edits to the same other field use the last change received by the server.
Quantities stay between 1 and Int.MAX_VALUE. Deletion wins over delayed edits. List
deletion tombstones its entries; catalog deletion clears references and retains list
snapshots. Operation IDs and tombstones currently remain stored; retention/compaction
needs a later design before broad public use.

Server limits: names/categories 512 characters, links 8192 characters, upload batches
100 operations/1 MiB. A rejected batch remains queued; the UI currently shows a generic
retry status. More detailed validation/error presentation remains a future improvement.
No paid service or billing change was enabled.

## Verification

- Debug APK, Android test APK and unit-test Gradle tasks passed.
- Note 20: 12 isolated instrumentation tests passed (account isolation 3, catalog 2,
  shopping lists 3, sync storage/retry 4), including migration checks.
- `supabase/tests/owner_sync.sql` passed against the actual service in a transaction
  that rolled back all fixture users/records. Verified RPC/RLS account isolation,
  denied direct writes, idempotent replay, quantity deltas, cascading deletion and
  protection against resurrection by delayed edits.
- Opt-in `LiveSyncTest` passed using two isolated Room databases against the actual
  authenticated service. Verified creation/replay, concurrent quantities, check state,
  rename and deletion convergence. Only its unique disposable list was removed; the
  user's real local database was never opened by this test.
- A private databases/files/preferences backup was saved before `adb install -r`.
  Direct instrumentation was used; the app was not uninstalled.
- The updated app was opened on the unlocked Note 20. Existing Home contents were
  visible; Account displayed **Up to date** for the owner's signed-in account.

The live test models two clients on one phone. Acceptance on two physical phones
remains pending. Invitations/cross-account sharing are not implemented.

Opt in to the live-service test explicitly (requires an existing signed-in session):

```powershell
adb shell am instrument -w -e syncLive true -e class com.example.familyapp.LiveSyncTest com.example.familyapp.test/androidx.test.runner.AndroidJUnitRunner
```

Normal test runs skip the live-service test.
