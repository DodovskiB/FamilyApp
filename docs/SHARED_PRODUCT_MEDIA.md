# Shared product photos and links — 1.2.0-preview

List entries now own their optional photo and http/https URL. Every current Owner/Admin
of that list can view, add, replace or remove those attachments by long press, including
entries with no private catalog reference. No persistent action buttons were added.
The personal catalog stays private. Editing an entry does not edit a catalog row;
editing a catalog attachment propagates to that account's linked entries, whose
attachments then sync through the permissions of their respective lists.

## Storage and privacy

- Room v6 adds nullable linkUrl, photoFileName and photoRemotePath to entries. The
  v5 migration backfills linked catalog attachments without changing shopping state.
- Camera/gallery imports are normalized on the phone: metadata-free JPEG, at most
  1024 pixels on either side and 250 KiB (256000 bytes). Originals remain untouched.
  Legacy oversized app copies are compressed before upload; normalized bytes are reused.
- Supabase private bucket shopping-photos permits JPEG uploads up to 256000 bytes.
  Object names are owner UUID / list UUID / SHA256.jpg. Existing objects are immutable.
  Access follows current list membership, including explicitly merged list redirects.
- Account-scoped local download caches verify content hashes and can repair missing or
  corrupt cache files. Cached content is removed when no local row/queue references it.
- Separate durable field-level operations protect concurrent photo/link edits, replay,
  process restarts, and late upload acknowledgments. Unknown new targets defer to the
  next cycle; confirmed removals archive offline changes privately and never upload
  them as personal-owner data. The archive is not an extra user-facing history feature.
- Replays after a list merge compare item identity and image digest, so changed routing
  prefixes cannot turn an acknowledged operation into an ID-reuse failure.
- Referenced photos, including non-deleted archived purchase entries, are retained.
  Unreferenced uploads have a 24-hour grace period; cleanup runs through Storage API.
  Owner-deleted fixture/list objects can be reclaimed immediately. No history purge.

Supabase Free remains in use; no billing or extra service was enabled. A local phone
cache reduces repeat downloads but does not replace cloud storage for other members.
Free storage/egress quotas still apply. No new Google account was created.

## Backend and compatibility

Deployed: supabase/migrations/20261022020000_shared_media.sql. Existing list, quantity,
purchased and sharing RPCs remain compatible with older clients; those clients do not
show shared attachments until updated. Photo sharing works only while online; offline
edits queue durably and synchronize while the app is open after connectivity returns.

SQLite sync mapping triggers also receive an id-preserving upgrade: Room UPDATE OR
ABORT can override a nested INSERT OR IGNORE. Conditional insertion avoids that
constraint error without changing existing remote identities or outbox entries.

## Verification on 2026-10-22

- Debug/release builds and 3 unit tests pass.
- 7 shared-media storage checks: independent fields, upload race, authoritative clear,
  durable queue, deleted-entry archive, unknown-target deferral, noisy-photo compression,
  metadata removal, source preservation, isolated/repairable caches, repeat purchase,
  ordinary stale edits and explicit v5-to-v6 backfill (some checks cover several cases).
- 20 pre-existing storage/migration checks pass. After the trigger fix, 7 ordinary
  sync and 3 sharing checks pass, including media revocation/archive protection.
- 2 photo UI tests and 1 link UI test pass, including unlinked entry camera/gallery,
  removal before download and no accidental private catalog mutation.
- SQL rollback fixtures pass Owner/Admin/stranger, pending/revoked membership, direct
  table denial, replay, null mask, invalid link, missing image, independent clear,
  merged-list replay and deleted-entry non-resurrection. No real user data was altered.
- Opt-in LiveSharedMediaTest passes against Storage API with generated JPEGs and two
  disposable DBs: upload/download, duplicate upload, missing cache repair, offline
  independent photo/link edits, removal convergence and explicit fixture cleanup.
- Honor update-install and real account Up to date checked. Private pre/post backups
  confirm original fields unchanged in both databases; active DB migrated v5 -> v6.
- Physical two-account acceptance passed: Owner on Note 20 invited Admin on Honor.
  Owner confirmed the generated green image could be viewed on Honor; Honor changed
  the link and removed the photo. Server assertions and the real Note 20 UI confirmed
  both edits. The fixture list/image were removed through the normal RPC/Storage API;
  orphan queries verified the object was actually deleted. No personal photo was used.
- Note 20 final update-install passed. Pre/post comparison confirms the same 3 lists,
  6 entries and 5 catalog products, original sync UUIDs preserved, both queues empty.
  Honor was not USB-connected again after fixture cleanup; its final disappearance
  follows the tested deletion sync and was not separately inspected over ADB.

Release APK uses the existing private release certificate, versionCode 4; personal
Note 20/Honor use the existing debug certificate and must never be uninstalled to
change signing. Automatic update download/install is still not implemented.
