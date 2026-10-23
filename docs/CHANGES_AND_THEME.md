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
