# Product photos and list labels

Owner requests: attach product photos from camera or gallery; use English list names;
show only list names on home cards, while retaining tap and long-press behavior.
Checkpoint: checkpoint/before-product-photos-20261020 at afcbc7a.

## Behavior

Long press a catalog product or a catalog-linked list entry to open Take photo,
Choose from gallery, View photo, and Remove photo. Photo options are contextual,
not persistent buttons below the item. List-entry removal now also uses long press.

Photos belong to reusable catalog products and are visible in the catalog, product
suggestions and all list entries still linked to that catalog product. Replacing a
photo updates all those views. Title/category list snapshots remain independent.
Deleting a catalog product removes its photo copy; list entries retain their text,
quantity and purchased status but no longer show that catalog photo. Unlinked legacy
entries have removal actions only; they are not silently converted into catalog products.

The gallery uses Android's single-image photo picker (or its supported system fallback).
The camera delegates to the installed camera app using TakePicture and a narrowly
scoped FileProvider URI for one cache capture. No broad gallery or direct camera
permission is requested. No cloud upload or image-generation service is involved.

A successful selection is copied into app-private files/product_photos, downsampled
to at most 1600 pixels on the longest side and encoded as JPEG. EXIF orientation is
applied and source metadata is not copied into the JPEG. The database stores only
an app-relative filename. Gallery originals are never modified or deleted. The import
limit is 40 MiB. Unsupported images show a save error and keep the previous attachment.
Cancelling camera/picker preserves the old photo. Replacement/removal/catalog deletion
cleans the old private photo after the database update succeeds. Cache capture/import
files are removed on completion/cancellation; Android may clear leftover cache files.

Room version 4 adds catalog_items.photoFileName. All prior migration steps remain.
The same one-time migration translates existing Doma/Дома -> Home,
Vikendica/Викендица -> Weekend House, and Odmor/Одмор -> Vacation. Other list names,
IDs and contents are retained. New user-created list names are not auto-translated.
The Tap to open / Hold for actions subtitle was removed from home cards.

## Verification

Debug and Android test APK builds passed. All 10 relevant tests passed on VNE-N41 (Android 12):
- All previous catalog/list storage and UI checks after the migration.
- New photo storage test: persisted private copy, resizing/orientation, safe failed
  replacement, stale text edit retaining photo, file replacement/removal/deletion.
- Version 3 -> 4 test: existing product/list entry preservation and only known names translated.
- Photo UI test supplies controlled ActivityResult camera/picker success/cancel results;
  it does not photograph the owner or browse their personal gallery.

The real OEM camera/gallery still need manual confirmation on the owner's phone.
Update-install only; never uninstall. A phone database backup was saved before migration.
No GitHub push is authorized for these changes yet.
