# Shopping List App implementation review against the product vision

Date: 2026-10-19
Reference: [FAMILYAPP_VISION.md](../FAMILYAPP_VISION.md), preserved unchanged from the user's file.
Status: review and proposal only. No implementation is authorized by this document.

## Scope and evidence

Inspected current Kotlin UI, navigation, ViewModels, repository, Room entities/DAOs,
database migration, instrumentation tests, and the existing development plan.
Reviewed the current working tree, including uncommitted catalog work rather than
only the older GitHub snapshot. HEAD is adcaf84, tag stable-2026-10-19; catalog changes
and PLAN.md remain local. Three existing untracked IDE entries are unrelated.

Current debug build: SUCCESSFUL (36 tasks up-to-date). No source/build configuration
was changed for this review. No device tests were rerun or app reinstalled.
Earlier in this session, both CatalogStorageTest tests passed on the physical phone:
CRUD persistence across database reopen, and migration 1 -> 2 preserving sample lists/items.
The owner confirmed catalog additions and accepted the long-press menu UI. That does
not establish that every catalog interaction and full app restart has been manually tested.

Completed below means implemented in current code, with verification limits stated.
Partial means the user flow is incomplete even if storage methods exist.

## Feature inventory

| Vision capability | Status | Evidence and limits |
| --- | --- | --- |
| Main menu and navigation to both main screens | Completed, basic | MainActivity.kt, Screen.kt, MainMenuScreen.kt. Final visual design remains open. Current navigation uses remember rather than saved navigation state. |
| View and add catalog products | Completed | ShoppingItemsScreen, MainMenuViewModel, CatalogItemDao. Owner confirmed addition on phone. |
| Edit catalog name and category | Completed in code | Editor dialog updates CatalogItemEntity through repository; storage edit covered by earlier device test. Full manual interaction test not explicitly recorded. |
| Delete catalog products | Completed in code | Confirmation dialog and catalog-only DAO delete. Storage deletion covered by earlier test. |
| Product actions on long press | Completed | combinedClickable opens Edit/Delete. Actions button removed at owner's request. Ordinary tap also opens edit. |
| Categories | Partial relative to an undecided category system | Free-text category, blank defaults to Other; sorted by category/name. No separate category manager, category picker/filter, or finalized taxonomy. Basic categorization exists. |
| Local catalog persistence | Completed in storage; manual restart check pending | Room catalog_items, database version 2, non-destructive 1 -> 2 migration. Reopen test passed; full process restart UI behavior not manually established. |
| Select catalog products for a list | Missing | No picker, no catalogItemId on ItemEntity, no repository operation linking catalog to list entries. |
| Create and view multiple lists | Completed, basic | ShoppingListScreen and ListDao. Lists displayed by sortOrder; all newly added lists currently default to 0. No agreed tie ordering. |
| Rename a list | Partial | ListDao.rename and repository/ViewModel method exist; no UI action exposes them. |
| Delete a list | Completed, basic; lifecycle incomplete | Visible Delete button removes only the list row. No confirmation, transaction, foreign key, or cleanup of its items. |
| Open a list and display its products | Partial | ItemDao observes entries by listId, but no detail screen/navigation selection; ChecklistViewModel is not instantiated by UI. |
| Add a product directly to a list | Partial | Repository inserts ItemEntity, but no reachable UI or create-new-product/catalog integration. |
| Quantity + / - | Partial | DAO/repository updates and unused ChecklistViewModel helpers exist. No controls in a screen. Helper assumes integer step 1 and minimum 1; this is not a finalized product decision. |
| Purchased/unpurchased toggle | Partial | ItemEntity.checked and update method exist; no list-item checkbox UI. |
| Remove product from a list | Partial | Soft-delete operation sets isActive=false; no UI. Separate from hard deletion in catalog. |
| Independent quantities/status across lists | Partial | listId, qty, checked stored per item. No end-to-end UI workflow or multi-list integration test. |
| Authentication, family membership/invitations | Missing | No account or membership model/UI/service. |
| Shared lists and cross-device synchronization | Missing | Current repository is local Room only. No sync provider chosen. |
| Per-list access and administrator permissions | Missing | No user/list membership, roles, authorization rules, or backend. |
| Product images | Missing; deferred | No image fields, storage, picker/camera workflow. |
| Prices, discounts, comparison/notifications | Missing; exploratory | No retailer integration; scope and sources undecided. |
| Ordinary/location reminders | Missing; deferred | No reminder scheduling/geofencing implementation. |
| Photo recognition | Missing; exploratory | No recognition workflow or chosen service. |
| Travel templates | Missing; deferred | A manually named Travel list is not a reusable template. |
| History/statistics | Missing; deferred | No activity/history model or reports. |
| Product links | Missing; deferred | No link fields or UI. |
| Distribution | Partial | Debug build, GitHub baseline and personal phone testing exist. No production release workflow or Google Play preparation verified. |
| School/parent-event reminders | Outside scope | Preserve the explicit non-goal. |

## Technical gaps relevant to the next feature

- ItemEntity contains an independent title/category snapshot and listId, but no catalog reference.
- RoomFamilyRepository accepts kind in observeItems/getItems, but ignores it in DAO queries.
  Meaning of kind is undocumented; do not introduce new kinds or rely on filtering yet.
- ListDao.deleteById leaves associated items behind. This becomes important when list contents are exposed.
- There are no foreign keys enforcing item/list relationships.
- Catalog duplicates are allowed; repository validates only a nonblank title.
- Catalog deletion currently leaves list items untouched because they are not linked.
- List writes do not have the same saving/error UI state as catalog writes.
- The existing architecture is suitable for the next local feature; no rewrite/new backend is needed.

These are observations and future work, not changes made during this review.

## Priority order

1. Finish checking the current catalog on the phone: edit/category, long press,
   cancel/delete, and persistence after force-close/relaunch. Save the approved local
   catalog work in a checkpoint before feature development. GitHub push needs separate approval.
2. Complete one local shopping-list workflow: open a list, choose a catalog product,
   display entries, change quantity, check/uncheck, remove, and preserve state.
   Include list rename and safe list deletion. This is the highest-value missing capability.
3. Add create-new-product while composing a list; verify catalog reuse across independent
   lists and define duplicate/rename/deletion behavior. This can be part of step 2 once decisions are approved.
4. Harden the local app with focused integration tests, empty/error states, navigation
   restoration/back behavior, and phone verification. Avoid broad visual redesign.
5. Plan family sharing: agree identity, invitations, roles, permissions, backend,
   offline/conflict behavior before choosing services or implementing sync.
6. Prioritize optional enhancements after the local core; consider release preparation
   only after family/internal testing. No fixed ordering of speculative ideas is implied.

## Proposed next development task — approval required

Deliver one complete local Shopping List experience using existing layers.

1. Add selected-list navigation and a list detail screen; reuse current list observation.
2. Add a product picker from the existing catalog and repository operation to add it to
   the chosen list. Keep list quantity/status independent of every other list.
3. Add + / -, checked/un-checked controls and removal from this list only.
4. Add list rename, confirmation for deleting a list, and transactional list-item cleanup.
5. Add create-new-product from the picker after confirming duplicate handling.
6. Apply a versioned Room migration only if the approved catalog-link model requires it;
   preserve existing entries. Do not clear the database or uninstall the personal app.
7. Build and test storage/integration using isolated test databases, then update-install
   on the phone with adb install -r. Avoid the Gradle connected runner cleanup previously
   observed to remove the target app. Verify on the phone with two independent lists and restart.

Acceptance: choose Milk for Home and Travel; changing Home's quantity or purchased
state does not alter Travel. Removing Milk from Home retains it in the catalog and
Travel. Cancelled deletions change nothing. Approved state survives app restart.

## Decisions to approve before implementation

Recommended provisional choices, not requirements from the vision:

- Quantity: whole pieces, minimum 1, step 1; units/fractions deferred.
- Same catalog product added twice to one list: increase quantity by 1 rather than
  create a second active row. Different catalog products with the same name remain
  distinguishable by ID. Warn about duplicate catalog names rather than silently merge.
- Link: keep an optional catalog product ID plus a list title/category snapshot.
  Existing list rows without a catalog ID remain valid. Catalog edits affect future
  additions, not existing list snapshots, unless the owner prefers live propagation.
- Catalog deletion: existing list entries survive, with their saved labels/status/quantity.
  Removing a list entry never deletes the catalog product.
- List deletion: confirm and remove only that list's entries together; retain catalog
  and every other list. No archive feature in this task.
- Categories: keep current free-text/Other behavior while the category system is undecided.

Approval requested: the proposed local Shopping List task and these provisional
behavior rules. Implementation remains paused until the owner approves or revises them.
