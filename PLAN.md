# Shopping List App development plan

Current product name: **Shopping List App**, renamed by the owner on 2026-10-21.
FamilyApp is the historical name. Keep the existing Android applicationId, package
and database names to preserve update compatibility and user data. The owner approved
renaming the repository and local project folder to shopping-list-app on 2026-10-21.

Latest verified checkpoint: `stable-2026-10-21`. Google sign-in was confirmed by the
owner on Note 20, and session restoration/home navigation was verified after a cold
restart. Debug app/test builds, unit tests and 8 device storage/security tests passed.
The owner requested saving this baseline to GitHub before beginning synchronization.
That tag is the pre-sync baseline. Same-account cloud synchronization now passes
build, storage, server access-control and live two-client tests. Note 20 displays
Up to date with existing local contents retained. See docs/CLOUD_SYNC.md.
Two-physical-phone same-account online/offline/concurrent acceptance passed.
Owner/Admin sharing and in-app invitations are implemented; cross-account physical
acceptance passed. See docs/LIST_SHARING.md for behavior and limits.

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

Owner-approved UI follow-up: refine the design screen by screen in the existing
Compose project after sharing is stable. Add Settings -> Language with English as
the default and Macedonian (Cyrillic) as an option, persisting the choice across
restarts. Translate interface text, not user-entered list/product names. This is
planned, not implemented.

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

## Purchased and home creation button — approved 2026-10-21 / verified 2026-10-22

Implemented To buy / Purchased sections, guarded Buy again with a new quantity,
and confirmed long-press Clear purchased for Owner/Admin. + New list is now at
upper-right home and removed from the drawer, superseding the menu-only rule.
No permanent home input form. See docs/PURCHASED_WORKFLOW.md for repository/DAO
semantics and current tests. This is newer than confirmed GitHub commit 41356ca;
no new push without approval. Note 20 updated; Honor awaits this APK update.

## Drawer and clear-action placement — owner update 2026-10-22

Drawer: account/email visually separated, then Invitations, Shopping lists, Item list.
Clear purchased is now a separated footer within open-list content below purchased
entries, with confirmation, hidden when none are purchased. Remove its long-press
menu entry. This explicitly supersedes the previous clear-action placement and is
an exception to the general contextual-action convention. Data/sync semantics stay
as documented in docs/PURCHASED_WORKFLOW.md. Note 20 updated and 5 UI tests passed.

## EN / MK — owner approved 2026-10-22

Simple direct choices at bottom of hamburger menu, English default, Macedonian
Cyrillic optional and persistent. Translate app UI/status/accessibility, preserve
stored names. See docs/LANGUAGES.md. After Note 20 verification, update Honor with
all purchased, drawer and language changes and verify language persistence there.
Last owner-confirmed GitHub checkpoint is 60a07e0; newer work is local until pushed.

## Free APK preview — owner approved 2026-10-22

Use direct signed APK distribution for now, no paid Play registration or hosting.
Fresh installs: Home, Weekend House, Vacation; editable/deletable, empty entries
and personal catalog. Preserve existing account data on sign-in. Preview 1.1-preview
(versionCode 2), dedicated private release certificate for new testers. Existing
debug-signed personal test phones are not replaced/uninstalled. See
 docs/PRIVATE_APK_DISTRIBUTION.md for build, signing, updates and limitations.
No automatic updater or catalog rename/ephemeral-product feature is approved here.

## Shared product media — resumed 2026-10-22

Owner approved list-scoped photo/link sharing, phone compression and local cache on
Supabase Free. Implemented in 1.2.0-preview/4 with private storage, existing Owner/Admin
permissions, durable offline field queues, Room v6 migration and long-press actions.
Private catalog edits and list-entry edits remain separate. See
docs/SHARED_PRODUCT_MEDIA.md for verified results and remaining device acceptance.
No new backend service, paid quota, public APK upload or automatic updater.

## MK default labels and media menu order — owner approved 2026-10-22

Localize the three standard list display names only in MK: Дома, Викендица, Одмор.
EN and stored/cloud names remain unchanged; custom names are preserved.
Order list-entry and catalog actions: available View photo, available Open link,
Take photo, Choose from gallery, Add/Edit link, optional Remove photo, then existing
item edit/removal/deletion. Both photo and link -> photo first, link second.

## Owner follow-ups — 2026-10-22

Finish the interrupted grouped-invitation physical acceptance first (owner clarified
this ordering). Then: (1) cancel every pending row in a grouped invitation together,
keeping accepted-member revocation per list; (2) home-card unseen-change badge for
other members' changes, cleared when opened, excluding own changes and changes
while the list is visibly open; (3) persistent device-local System / Light / Dark
choice in the drawer, System default. Existing theme uses system darkness and
Android 12+ dynamic colors; preserve dynamic colors unless separately requested.
No OS push notification service is requested for the home-card badge.

## Implemented follow-ups — 2026-10-23

Group cancellation, actor-aware change badges and saved System/Light/Dark are implemented in 1.4.0-preview/8. Backend deployed and regression-tested; Honor updated. Physical two-phone badge acceptance, Note 20 update and temporary Changes test cleanup are pending. Read the latest PROJECT_HANDOVER.md section.

2026-10-23 follow-up: physical two-account badge appearance/restart/clear/live-update
checks and fixture cleanup are complete. Both phones run 1.4.1-preview/9, including
a full-screen theme background fix verified with rendered-pixel UI regression and
visual Light/Dark review. Original data on both phones is preserved exactly;
queues empty. Signed APK ready. Next: wife's compatible release update, owner push,
then agree new scope; Google branding remains deferred.
