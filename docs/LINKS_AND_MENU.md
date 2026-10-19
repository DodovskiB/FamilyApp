# Item links and New list menu

Owner request: 2026-10-20. Local checkpoint: 2b2040a,
checkpoint/before-item-links-20261020. No push authorized for this change.

## Product links

Long press a catalog product or a linked shopping-list item:
- Add link / Edit link opens a URL field.
- Open link launches the saved website in a browser only when the user chooses it.
- Remove link in the editor removes the attachment without deleting the product.
- Cancel discards edits.

One optional URL belongs to each catalog product and is shared with its linked list
entries. Amazon, eBay and other http/https product pages are supported. A missing
scheme is normalized to https. Malformed URLs, unsupported schemes, and embedded
credentials are rejected. No link preview is fetched and there is no purchase flow.
Existing title/category snapshots remain unchanged. A link edit only updates linkUrl,
leaving photo metadata intact. Text edits similarly retain the existing link/photo.

Room version 5 adds nullable catalog_items.linkUrl, with the full migration chain
retained. Current products, photos, lists, quantities and purchased states are preserved.

## List creation

The home screen no longer displays the New list name field or its Add button.
Use the upper-left menu -> New list -> enter List name -> Create. Cancel creates
nothing. Saving errors remain in the dialog, with the entered name retained for retry.
Shopping lists and Item list remain in the same navigation drawer.

## Verification

Debug and Android test APK builds passed. All 13 relevant checks have passed on VNE-N41 (Android 12), including the corrected browser-action interception test:
- Link validation, persistence after database reopen, stale text edits and removal.
- Version 4 -> 5 migration retaining photo metadata and shopping state.
- UI add/edit/cancel/open/remove from catalog and shopping-list context menus.
- List creation through the drawer, plus existing photos/search/shopping regressions.

Browser opening is intercepted by a test UriHandler; no Amazon/eBay page is opened
by automated tests. Phone tests use isolated databases and update-installation.
A backup of the personal app databases and private files was saved before migration.
