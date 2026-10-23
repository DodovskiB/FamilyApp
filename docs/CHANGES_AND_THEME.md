# Group cancellation, list-change badges and appearance

## Group cancellation

The existing owner-only `shopping_sharing_remove` RPC cancels every pending row
in the selected invitation's group, in one transaction under the existing owner
lock. Ungrouped invitations still cancel singly. Member removal remains per list;
already accepted grants survive cancellation of the remaining pending rows.
The confirmation lists all affected list names in EN/MK. Older clients also use
this RPC and receive the corrected group cancellation behavior.

## Unseen list changes

Migration `20261023090000_cancel_groups_and_activity.sql` adds private per-list,
per-actor revision watermarks and a permission-filtered activity RPC. Triggers track
list/item content changes, deletion markers, and attached photo/link changes from
all client versions. Repeated writes without a content change do not raise activity.
The RPC excludes the signed-in user's own contributions and returns only currently
accessible shared list IDs and revisions, never another user's activity history.
Existing authentication and Owner/Admin access rules remain in force.

Each account's local Room database has an auxiliary `shopping_list_seen` table.
Opening a list marks the last downloaded activity as seen, independently on each
phone. A small accessible badge appears next to a list's name after somebody else
changes it. While that list is visible in a started activity, successfully downloaded
changes are marked seen. Returning to home shows no stale badge. Another later
change shows it again. Read state survives app restart, is isolated by account DB,
and is removed when the activity snapshot no longer grants access.

Activity is fetched before content, but published/marked seen only after content
and media synchronization complete with no pending writes. Network failures retain
the cached badge state. This avoids acknowledging activity newer than the content
the phone has downloaded. The existing five-second foreground sync loop is reused;
there is no background OS notification or paid push service.

No historical activity is backfilled at deployment. Existing lists acquire new
activity watermarks when their contents change after deployment.

## Theme

Drawer -> Theme / Тема offers System / Light / Dark (Системска / Светла / Темна).
System is the default. Selection is stored privately on the device, applies to all
accounts and the account screen, survives restart, and preserves the EN/MK choice.
System follows the phone's dark setting. Android 12+ dynamic accent colors remain
enabled, so palettes can still vary between phones/wallpapers.

Version 1.4.1 adds a full-screen themed Surface: MaterialTheme alone did not paint
the home background or provide a default content color. Rendered pixel assertions
now verify actual light/dark backgrounds, not just the selected color palette.

## Verification

The migration and exact rollback SQL regression suite were exercised through the
project SQL editor, then deployed and tested again. Coverage includes three-list
group cancellation, retries, non-owner denial, legacy partial acceptance, accepted
grant preservation, per-list revocation, own/other-actor separation, media activity,
and private/revoked activity isolation. Test users and rows are rolled back.

Android Studio builds debug/release/test APKs and unit tests. Isolated device tests
cover durable read state, account separation, open/closed behavior, offline failure,
revocation, group confirmation, badge UI, actual light/dark palette changes and
preference persistence. English assertions use a configuration override, preserving
the personal phone's MK locale and real activity-result registry for photo actions.

See the latest PROJECT_HANDOVER.md section for final device acceptance, APK signing,
Git checkpoint and remaining work. Never infer physical cross-device acceptance
solely from these automated checks.

Physical cross-account acceptance on 2026-10-23 also passed: Honor changes produced
a Note 20 home badge, which survived restart and cleared on opening. A further
change while the list was visible updated immediately and left no stale badge.
Owner removed the temporary list and catalog product. Both phones now run 1.4.1/9;
exact original entity comparisons and empty sync queues were verified after cleanup.
The targeted three UI tests passed again on Note 20 after the Surface fix.

### Additional Honor live-account proof — 2026-10-23

Owner deferred Note 20 and requested finishing Honor-only work. The explicitly
opted-in PhysicalGroupCancelTest used the signed-in Honor account to create three
generated disposable list IDs, invite the designated existing test recipient,
cancel one invitation, retry it, and verify that all three pending rows disappeared.
Existing invitations (including Changes test) and members were preserved. The
generated test lists were soft-deleted and the private cleanup ledger removed.
This live RPC test passed. It never launches the main UI or writes real Room rows.
Normal test runs skip it unless physicalGroupCancel=true and the expected owner
and recipient arguments are provided. A stale ledger blocks rerun after incomplete
cleanup, rather than creating more fixtures.

After normal app synchronization, exact comparison of every local list/item/catalog
row against the earlier private backup passed; queue empty and Up to date. The
pre-existing Changes test fixture remains for the deferred two-phone badge check.
App code and signed 1.4.0 APK are unchanged by this test-only follow-up.
