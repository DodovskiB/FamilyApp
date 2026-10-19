# Local shopping lists: approved behavior and verification

Implementation date: 2026-10-19
Product reference: [FAMILYAPP_VISION.md](../FAMILYAPP_VISION.md)
Pre-implementation review: [IMPLEMENTATION_REVIEW.md](IMPLEMENTATION_REVIEW.md)

The owner approved the proposed local Shopping List task and behavior rules.
Checkpoint: local commit e6fea25, tag checkpoint/before-shopping-lists-20261019.
No new changes have been pushed to GitHub.

## Implemented behavior

- Create, open, rename and delete shopping lists.
- Select products from the existing catalog and add them to the current list.
- Add a new catalog product while composing a list, in a single database transaction.
- Warn when a newly entered name already exists; the owner can return to the picker
  or deliberately create a distinct catalog product.
- Repeated selection of the same catalog ID increments quantity instead of creating
  a second active entry. Concurrent selections are serialized in a Room transaction.
- Integer quantity, minimum 1, step 1, maximum Int.MAX_VALUE.
- Purchased/unpurchased checkbox; list-specific status and quantity.
- Confirm removal from a list and deletion of a list. Cancelling performs no writes.
- List removal is a soft delete; confirmed list deletion cleans its active/inactive
  entries together in a transaction, retaining catalog and other lists.
- Catalog title/category are copied into list entries. Later catalog edits leave these
  snapshots unchanged. Catalog deletion clears the optional reference and retains
  list snapshots, quantities and purchased states.
- New Room version 3 adds a nullable catalogItemId; legacy rows remain valid.
- Register both 1 -> 2 and 2 -> 3 migrations, without destructive fallback.
- Main screen and selected list use saveable UI state. Existing architecture retained.

## Verification record

Debug application and Android test APK builds passed before device testing.
Five storage tests passed on VNE-N41 (Android 12): catalog CRUD persistence,
v1 migration chain, list isolation/snapshots/reopen, concurrent additions and quantity
bounds, and v2 -> v3 migration preserving catalog and unlinked legacy entries.

The first UI test attempt could not find a visible Compose hierarchy because the
phone was asleep and locked. UI testing is pending an unlocked, awake phone.

Tests use isolated databases. The phone app databases were backed up locally before
update-installation. APKs were installed with adb install -r, and instrumentation was
invoked directly. The target app was not uninstalled. Do not use the Gradle connected
runner on this personal installation because its earlier cleanup removed the app.

## Remaining gate

Manually confirm the real app can open
Home and Travel, reuse a product, change only one list, and preserve state on restart.
GitHub push and a new stable release require owner approval.
