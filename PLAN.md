# FamilyApp development plan

Current product name: **Shopping List App**, renamed by the owner on 2026-10-21.
FamilyApp is the historical name. Keep the existing Android applicationId, package,
database names and Git repository to preserve update compatibility and user data.

Updated: 2026-10-19. Based on the user's Project Handover & Development Plan.

## Confirmed baseline

- Android / Kotlin / Jetpack Compose, package `com.example.familyapp`.
- Existing architecture: Compose UI -> ViewModel -> Repository -> Room DAO -> Database.
- Stable commit `adcaf84`, GitHub tag `stable-2026-10-19`.
- User confirmed `:app:assembleDebug` succeeded and the app worked on a physical phone.
- Main menu navigation and basic shopping-list creation/deletion exist.
- Shopping Items was a placeholder before Phase 2. Full shopping-list item workflows are not verified or complete.
- Prior handover statements that compilation was unconfirmed are superseded by this baseline.
- The emulator remains unreliable on this PC; use the physical Android phone for verification.

## Product direction

FamilyApp provides a reusable product catalog and multiple independent shopping lists
(Home, Weekend House, Travel). Each list has its own quantities and purchased status.
Later, family members will share selected lists with synchronization and access controls.
Start with a dependable local app, test privately, then consider Google Play.

## Working rules

- Keep the existing project and architecture; inspect real code before changing it.
- Create a Git checkpoint before a new development phase.
- Stabilize broken builds before adding features.
- Review diffs, run Gradle, and verify on a physical phone.
- Do not claim unexecuted tests passed.
- Push only with explicit user approval. Approval for the stable baseline did not cover future changes.
- No new frameworks, cloud services, or libraries without technical need and agreement.

## Phase 1: Stabilization — complete

Repository/DAO/ViewModel compilation mismatches were fixed. Build and phone testing
were confirmed by the user. The stable commit and tag were pushed to GitHub.

## Phase 2: Shopping Items — implementation awaiting verification

- [ ] Display the persistent catalog.
- [ ] Add a product.
- [ ] Edit its name.
- [ ] Delete with confirmation.
- [ ] Organize by category.
- [ ] Verify Room persistence, including after app restart.

Implemented locally: a separate `catalog_items` table, version 1 -> 2 migration,
DAO/repository operations, ViewModel state, and the Shopping Items screen. Keep existing
list/item tables and data. Categories are free-text with `Other` as the blank default;
this is a provisional UI choice, not a fixed category taxonomy. Products are sorted
by category then name. Long press exposes edit/delete; the user requested no Actions button.

Verified: debug APK and Android test APK built successfully. Both CatalogStorageTest tests passed on VNE-N41 (Android 12): CRUD persistence across database reopen and v1 -> v2 migration retaining existing lists/items.

Remaining verification gate: physical phone
add/edit/delete/category checks, retained shopping lists, and persistence after restart.
No new stable tag until this verification is complete.

## Phase 3: Shopping Lists — build and automated tests passed; manual acceptance pending

1. Verify existing list display/create/delete.
2. Rename and open a specific list.
3. Choose reusable products from the catalog, or add a new product.
4. Change quantity with + / -.
5. Mark purchased / unpurchased.
6. Edit and remove a list item.
7. Verify independent quantities/status across lists and local persistence.

Completion: a fully functional local shopping app. Catalog products and list entries
are separate concepts. Decide the catalog-to-list link and deletion semantics before
implementing that integration; do not assume changes to one list affect another.

## Phase 4: Family Sharing

Decide authentication, backend/cloud service, roles, and membership model first.
Then implement family joining, list sharing, selected-list access, admin controls,
synchronization, and testing across multiple phones. The owner selected Google sign-in
and Supabase Free on 2026-10-21. See docs/ACCOUNTS_AND_SHARING.md for approved
Owner/Admin permissions, invitations, purchased-item behavior and staged acceptance gates.

## Phase 5: Future features (order undecided)

Product photos; normal and location reminders; reusable travel templates; product
links; history/statistics; photo-based product recognition; price/discount tracking.
Sources/stores, AI service, statistics, and reminder rules are not decided.
School/parent-event reminders are outside the current scope.

## Phase 6: Distribution

Internal testing -> family testing -> fixes -> Google Play preparation -> possible
public release. The current app is a development build, not production-ready.

## Open decisions

Final visual design; fixed vs user-defined category management; catalog/list linkage;
authentication; backend; user/admin roles; access model; future feature order.

## Device test note

The Gradle connected-test runner removed the target app installation after testing
on this device. The debug APK was reinstalled afterward. Earlier local test data may
have been removed by that runner teardown; this is separate from the Room migration.
For subsequent tests on a phone containing user data, avoid this runner cleanup:
install app/test APKs with adb install -r and invoke instrumentation directly, or use
a dedicated test device. Do not rerun connected tests on the user's app data.


## Approved Shopping List implementation

The owner approved the local Shopping List workflow and provisional rules on 2026-10-19. See docs/SHOPPING_LISTS.md for implemented behavior, migrations, tests and remaining verification. FAMILYAPP_VISION.md remains the original product reference.

## Interaction rule approved by the owner — 2026-10-20

Use a long press on the relevant item/field to expose contextual actions. Do not add
persistent Rename/Delete/Actions buttons below each entry. Shopping list cards open
on a normal tap and expose Rename/Delete on long press. Preserve this convention for
future contextual actions. Primary entry/navigation controls and direct quantity or
purchased-state controls remain usable directly.

## Home and product lookup — owner request, 2026-10-20

- Open directly on existing shopping lists (e.g. Doma, Vikendica, Odmor), without the old two-button main menu.
- Keep the catalog behind the upper-left menu as Item list.
- Add product uses prefix search with case-insensitive live suggestions from saved products.
- Do not display the full catalog in an empty search; prompt for a name first.
- Allow selecting a suggestion or creating a product using the search text as the initial name.
- Preserve existing lists/products and long-press contextual actions.
- Username and settings are future menu destinations. No authentication/profile/settings system has been approved or implemented in this change.

## Product photos and English list labels — 2026-10-20

Approved by owner: attach a product photo from camera or gallery. Implemented through
long-press product actions, with local private copies and previews in catalog/search/lists.
See docs/PRODUCT_PHOTOS.md for lifecycle, migrations and verification limits.

Translate existing Doma, Vikendica and Odmor (including common Cyrillic equivalents)
to Home, Weekend House and Vacation once, preserving contents. Home list cards show
only their names; tap opens and hold exposes actions. Do not restore hint subtitles.

## Product links and menu-only list creation — 2026-10-20

Owner requested links on items (Amazon/eBay examples), and moving New list name off
the home screen. Long-press product actions now include Add/Edit link and Open link;
the editor can also remove it. Store one optional http/https link per catalog product.
Do not auto-fetch previews or launch websites except on an explicit Open link action.
The home screen has no new-list form; use hamburger -> New list -> name/Create.
See docs/LINKS_AND_MENU.md for implementation and verification.

## Default shopping lists — approved 2026-10-20

On first setup with an empty database, create Home, Weekend House and Vacation.
Apply this to new installs and previously empty installations such as the owner's
Galaxy Note 20. Retain any existing non-empty list set rather than adding duplicates.
Record one-time completion in private preferences. If the owner later deletes all
lists, do not recreate them on restart. Products are not copied or synchronized
between devices by this change. No Room schema change is required.
