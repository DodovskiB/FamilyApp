# Home screen and product search

Owner request: 2026-10-20.
Checkpoint before this change: b8d20d1, tag checkpoint/before-home-and-search-20261020.

- MainActivity now opens FamilyAppRoot, showing the existing saved shopping lists directly.
- No default/demo lists or products were inserted into the owner's database.
- Upper-left menu contains Shopping lists and Item list (catalog maintenance).
- Catalog visibility and list screen state are retained through navigation/configuration changes.
- Add product opens a search field; an empty query shows a prompt rather than all products.
- Case-insensitive title-prefix suggestions update as the user types. Example: l matches
  Leb and Lubenica when those Latin-script names exist in the catalog. Cyrillic names
  match Cyrillic input; automatic transliteration is not included.
- Select a saved product to add it, or choose New product. The query pre-fills its name.
- Existing list-specific quantities/status, long-press actions and Room data are retained.
- No database migration, new dependencies or backend were needed.
- Username/authentication/settings behavior remains future work; no inactive menu stubs.

Verification: debug/test APK builds passed. FamilyHomeUiTest passed on VNE-N41,
covering saved lists at launch, prefix filtering, new product creation from an unmatched
query, catalog navigation via the menu, and returning to lists. The prior list UI test
needed its selector updated to distinguish the query input from the matching suggestion;
its rerun passed (OK, 1 test). Both relevant UI tests now pass.

No push to GitHub was performed.
