# Purchased workflow and home creation button

Owner approved this phase on 2026-10-21; verification continued after midnight.
It supersedes the old menu-only New list placement.

## User behavior

- The home screen has a **+ New list** button at the upper right. It opens the
  existing name/Create dialog. New list is removed from the hamburger menu.
- List details show outstanding entries under **To buy**, then checked entries
  under **Purchased**, with strikethrough. Unchecking moves an entry back to To buy.
  Existing ID order is retained within each section; quantities and checkboxes work
  in both sections. Empty sections have no heading.
- Inside an open list, **Clear purchased** is a separate footer below the last
  Purchased entry, with extra spacing, a divider and an outlined button. Owner and
  Admin can use it; confirmation is retained. Hide the footer when none are purchased.
  It is no longer a list long-press action (owner revision, 2026-10-22). Only that list's currently purchased, active shopping entries are removed
  from the active view. Unpurchased entries, catalog products, other lists, product
  photos/links and permissions are unchanged. Cancel does not mutate anything.
- Selecting a catalog product already purchased on this list opens **Buy again?**,
  with a new quantity (default 1, valid range 1..Int.MAX_VALUE). Confirm starts a new
  unpurchased purchase cycle; Cancel retains the original quantity and purchased state.
  Adding an outstanding product still increments it by one. Matching uses catalog
  identity, not a name-only heuristic that could combine distinct products.

## Persistence and sync

The architecture and Room schema version 5 are unchanged. Clearing is one Room
transaction updating isActive=false for the checked entries in the selected list.
It reuses the existing durable item outbox and Owner/Admin field permissions.
There is no new SQL migration, service, library or billing change.

Buying again is guarded in the repository, including against a stale picker view.
Without explicit quantity confirmation it throws a typed result handled by ViewModel.
On confirmation, the previous purchased entry becomes inactive and a fresh entry
UUID carries the chosen initial quantity and unchecked state, in one transaction.
Late deltas/check changes targeting the old cycle cannot change the new entry.
This also avoids an unsupported absolute quantity update in the existing sync API.
If another actor has already unchecked/removed the product before confirmation,
the transaction rejects the stale confirmation; the user can Cancel and select again.

Rows are soft-removed as in the existing list-removal workflow; this is not a catalog
deletion or new history UI. Cross-device batches follow the existing sync/retry rules.

## Validation

Debug app APK, test APK and unit-test Gradle tasks passed. Two new isolated storage
tests passed: scoped durable clear/catalog retention/outbox fields, and guarded
re-add/new quantity/new identity/invalid quantity behavior. The new isolated UI test
passed after restricting its selector to the picker dialog: section order, cancel,
confirmed re-add, clear confirmation/cancel and no duplicate New list drawer entry.
A total of 19 selected isolated checks passed: 18 passed in the first run, then
the corrected UI test passed separately. No owner records are used by tests.

Existing shared-role UI/storage, sync queue, list migration/storage, home search and
list UI checks were included. All 3 opt-in live sync tests passed, including the
new purchased-cycle test using two isolated Room databases against the real server:
checked qty 5 -> confirmed new unchecked qty 2 -> clear -> both clients empty,
with the catalog retained and no pending operations. Disposable cloud fixtures
were deleted in finally; no real shopping data was edited.

Note 20 was privately backed up and update-installed with adb install -r. Actual
home UI showed + New list at the upper right; its Create/Cancel dialog opened,
and the drawer no longer contained New list. Account showed Up to date. Final
SQLite backup comparison verified every original list/item/catalog column unchanged:
3 lists, 6 entries, 5 catalog products, 0 pending. The Home screenshot was visually
reviewed. The phone was left on the list home screen.

Honor still has the preceding sharing APK until it is connected for this update.
No new two-physical-phone purchased acceptance was performed; live two-client sync
and existing shared permission/storage checks are the verification in this phase.
No push is authorized for these newer changes.

## Drawer/footer follow-up — 2026-10-22

Drawer destinations now appear as account/email (medium emphasis and a separated
block), Invitations, Shopping lists, Item list. The existing application heading stays.
Clear purchased moved from the home list action dialog to the bottom of the open
list content; its data handling and Owner/Admin permissions are unchanged.

App/test builds and 5 focused isolated UI tests passed: purchased workflow, sharing
UI (2), home search and shopping list UI. The updated purchased test verifies footer
position below the purchased row, confirmation/cancel, hiding after clear, absence
from long-press actions and drawer order. The real Note 20 drawer order/spacing was
observed and visually reviewed after a backup and update-install. Final backup
comparison found all original list/item/catalog columns unchanged and 0 pending.
No new live-service/storage tests were needed for this UI-only placement change.
Honor still awaits the newer APK; no push for these changes has been authorized.
