# Shared product media — WORK IN PROGRESS, 2026-10-22

Do not install or distribute this branch yet. The owner requested stopping safely
because account usage is nearly exhausted, unless the feature was almost complete.
It is not almost complete: client compile passes, but backend/tests are still pending.

## Stable state

Stable master remains 9366d38 (verified Note 20 update); last human-confirmed GitHub
push is d19ca1a. Both personal phones still have Room v5 stable builds. Release
1.1.1-preview/3 in the chat workspace remains the previous stable tester APK.
No shared-media SQL has been executed, no bucket created and no new APK installed.
Checkpoint tag: checkpoint-before-shared-media-2026-10-22, at 9366d38.

## Approved direction

Share links and compressed photos on a list entry with its existing Owner/Admin
members. Keep the personal catalog private. Perform compression on the phone, max
1024 pixels and 250 KiB (256000 bytes), metadata-free JPEG, private cloud bucket,
local download cache. No paid services or billing activation. Official Gmail is
only an idea for tomorrow; no Google account/provider change is authorized now.

Supabase Free currently lists 1 GB file storage and separate egress limits. Phone
storage is a cache, not a reliable always-online server. Google-account 15 GB
Gmail/Drive/Photos is separate from Cloud Storage/Supabase quotas.

## Implemented locally, NOT VERIFIED end-to-end

- Room v6: nullable entry linkUrl/photoFileName/photoRemotePath, migration backfills
  from linked catalog rows. Existing IDs/quantities/status remain unchanged.
- Repository/DAO/ViewModel methods for list-entry attachments, including entries
  with no local catalog row. Catalog attachment edits propagate to linked entries.
- Reuse existing long-press photo/link UI through a display adapter; no new row buttons.
- Phone JPEG compression cap, content-addressed download cache, hash validation.
- ItemMediaStore: separate field-mask durable queue, legacy seed, pending-field
  protection, revocation archive and cache adoption guarded against stale responses.
- ItemMediaSynchronizer/transport: per-list immutable private image paths; uploads,
  snapshots, retry acknowledgments, bounded cleanup. Wired into main SyncController.
- Proposed additive SQL migration 20261022020000_shared_media.sql: attachments,
  idempotency, authorization helpers, merge redirect handling, RPCs and private bucket
  policies. Existing sync RPCs are left compatible with old clients.
- Existing migration test builders now include MIGRATION_5_6.

Last actual verification: :app:compileDebugKotlin passed (15 seconds). Compiler
warns about inferred mixed arrayOf types in ItemMediaStore; make arrayOf<Any?>
explicit. No new instrumentation/build APK/live/SQL tests have run for this feature.
No version bump for this feature yet (still 1.1.1-preview/3 in Gradle).

## Resume in this order

1. Review the diff and this design before editing. WIP branch is the source of this
   work; do not reset it or deploy stable old APK pretending it includes attachments.
2. Fix/review client edge cases below, compile and implement meaningful isolated tests.
3. Create SQL tests with disposable fixtures and ROLLBACK: owner/admin/stranger,
   scope, addressed membership, revoked access, replays, invalid links/photo paths,
   pending uploads and private catalog. First test proposed migration + tests inside
   one rolled-back transaction before applying the actual migration.
4. Supabase existing browser tab is authenticated SQL Editor; original query is
   untouched. No direct DB credential is needed. Use documented browser controls,
   verify results and save UI proof for any deployed change. Do not enable billing.
5. Apply tested additive migration, verify bucket is PRIVATE, max 256000 bytes and
   image/jpeg only, no UPDATE object policy, same Owner/Admin list scope on download.
6. Build/test app and test APK; isolated Room migrations (v1-v5 to v6), media queue
   restart/replay, link/photo independent edits, no-catalog recipient actions, photos
   capped including noisy large inputs, original gallery untouched, replacements,
   deletion of catalog retaining entry photos, cleared fields staying cleared.
7. Run isolated live media checks with synthetic images/disposable rows, then the
   actual two-phone sharing/offline/revocation acceptance. Only private pre-update
   backups + adb install -r; never uninstall or connectedAndroidTest on these phones.
8. Update account sync-info resource text EN/MK: currently incorrectly says photos
   are phone-only. Update docs that currently describe photos/links as private-only.
9. Bump versionCode/versionName after checks, update-install appropriate debug APK
   on personal phones and package a release APK with the EXISTING private release
   key for new testers. Do not replace debug personal installs with release signing.
10. Update handover/checklist, commit tested result, give owner push command. No push
    or cloud/APK publication has been performed by the agent.

## Specific review items / known unfinished edges

- Add explicit `field_mask is null` validation in shopping_media_apply SQL.
- uploadPhoto duplicate-error handling should also recognize "already exists".
- uploadBytes currently re-encodes an already compressed local JPEG. Reuse its bytes
  when within bounds and app-owned/metadata-free to keep hash deduplication and avoid
  repeated JPEG quality loss. Legacy oversized copies still need compression.
- Buy again currently copies catalog media; review copying current entry media/path
  for repeat cycles, especially edits made by another member and entries with no
  private catalog reference. Do not reupload one image for each new purchase cycle.
- ItemMediaSynchronizer drops/acknowledges a queued op when snapshot lacks its target.
  Preserve it privately on confirmed removal, rather than silently losing edits.
- A remote image not downloaded yet has null photoFileName; current photo menu hides
  View/Remove for that state. Allow deliberate removal without requiring a download,
  and review missing-file/cache eviction handling.
- Deleting/replacing a local photo keeps files referenced by pending/revoked queues,
  but unreferenced old imported UUID files need safe cleanup after acknowledgments.
  Avoid racing an import before its DB attachment commits; remote cached hashes can
  be evicted immediately only when unreferenced by rows/queues.
- Check current pending-field logic and photo acknowledgment for edits during upload,
  revocation during upload/download, process death and changed canonical list IDs.
- SQL Storage API cleanup currently retains photos referenced by non-deleted archived
  entries. Do not silently destroy history to reclaim storage; document retention.
- Main SyncController currently invokes new RPCs whenever transport supports media.
  Until the backend exists the WIP app will show sync failure. This is why the branch
  must NOT be installed/distributed yet.
- The display adapter uses an ephemeral CatalogItemEntity with an entry ID; confirm
  every entry action calls the new entry methods, never private catalog mutations.
- Existing updateItem(entity) may overwrite media with a stale snapshot; preserve
  independent attachment fields if this legacy API is used for ordinary item edits.

## Tools/environment

Repo C:\Users\bojan\StudioProjects\shopping-list-app. JDK E:\Android Studio\jbr.
ADB C:\Users\bojan\AppData\Local\Android\Sdk\platform-tools\adb.exe.
Note 20 RF8NB24HBLB; Honor AAYSCP2B18205060. Current phone connection may change.
Shell Gradle works intermittently; latest compile succeeded through shell. Otherwise
use Android Studio's Gradle task launcher via computer-use skill, not terminal UI.
Private workspace work/media-build.log contains the latest compiler result; scripts
implement_media.py/media_ui.py/media_wire.py describe edits already applied. Do NOT
rerun these scripts blindly (not idempotent). Private credentials/backups stay out of Git.
