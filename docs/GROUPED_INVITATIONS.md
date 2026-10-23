# Grouped list invitations

The owner opens Share / Manage members for a list, enters an email, and checks
the owned lists to share. The recipient sees one card containing the selected
list names, with one Accept / Decline action. Acceptance grants Admin access to
each selected list. Member removal remains per list. Personal catalogs stay private.

Migration: `supabase/migrations/20261022131000_grouped_invitations.sql`, deployed
on 2026-10-22. The rollback SQL suite in `supabase/tests/grouped_invitations.sql`
passed at deployment. Legacy clients can still accept individual list invitations.

## Rapid edits after adding a shared product — 1.3.1-preview / 7

Physical acceptance testing exposed a queue stall: a new shared item inserted and
then edited before the next upload could be included in one request. The existing
shared RPC validates every operation against the committed database before applying
the batch, so it rejected the edit whose insert had not committed yet.

SyncController now ends a shared batch before a subsequent operation targeting an
ID inserted within that batch. Insert commits first, followed by queued edits in
order. Independent operations still batch; personal uploads retain their behavior.
Queue entries are acknowledged only after success and retain their operation IDs
for retry safety. No backend change or Room migration is required for this fix.

Regression: SharingStorageTest models committed-row server validation and covers
adding a catalog product to a shared list, incrementing quantity and checking it
before synchronization. Existing ownership, revocation and media tests also pass.

## Verification on 2026-10-22

- Android Studio Gradle: assembleDebug, assembleRelease, assembleDebugAndroidTest,
  testDebugUnitTest passed; 3 unit tests passed.
- Direct Honor instrumentation: SharingStorageTest + SharingUiTest, 8 passed.
- Note 20 selected only temporary Batch proof A and B; Honor saw one card with
  both names and one Accept. Accepted once; both lists became accessible.
- Honor added a test product to both lists and edited quantities/check state.
  Note 20 confirmed A = 2/purchased, B = 3/not purchased.
- Honor hit the rapid-edit stall with three queued operations. After update to
  version 7 its queue drained to zero, showing Up to date. Private pre/post
  comparison confirmed all list/item/catalog rows unchanged and exactly two
  shared grants retained.
- Per-list revocation and temporary-fixture cleanup: see latest handover section
  for the final physical outcome.

## Follow-up implemented in 1.4.0-preview / 8

The reported cancellation bug is fixed by migration
`20261023090000_cancel_groups_and_activity.sql`: cancelling one pending invitation
now cancels all pending rows of its owner's group atomically. Accepted-member
removal remains per list and accepted grants survive cancellation. The confirmation
shows the affected names. Three-list cancellation and partial legacy acceptance
were verified in the deployed rollback SQL suite.

Per-account/per-device unseen-change indicators and persistent System / Light / Dark
selection are implemented. See docs/CHANGES_AND_THEME.md and the latest handover
for final verification and pending cross-device acceptance.
