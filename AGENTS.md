# Shopping List App development guidance

Read FAMILYAPP_VISION.md for the product reference and PLAN.md for approved decisions.
Read PROJECT_HANDOVER.md for the latest cross-chat context. Keep its verified state,
test results, pending work and last confirmed GitHub checkpoint current when finishing
a development session. Never include credentials or private user data in it.

## Interaction convention

The owner wants contextual actions exposed by long-pressing the relevant item/field.
Do not add persistent Actions/Rename/Delete buttons underneath list or product entries.
A normal tap on a shopping list opens it; long press exposes Rename/Delete.
Use this convention for future contextual actions. Preserve direct primary controls
such as navigation, adding, quantity +/-, and purchased checkboxes.

Owner update 2026-10-22: Clear purchased is an explicit exception to the long-press
convention. Put it inside the open list, as a separated footer below purchased
entries, with confirmation. Remove it from list long-press actions. Hide the footer
when there are no purchased entries. Drawer order: account/email, visually separated,
then Invitations, Shopping lists, Item list.

## Personal phone testing

Use isolated test databases and update-install APKs with adb install -r. Do not
uninstall the owner's app or use the Gradle connected test runner on this phone:
its cleanup previously removed the target app. Invoke instrumentation directly.

Do not push to GitHub without explicit owner approval for the changes being pushed.

## Home and catalog navigation

The app opens on saved shopping lists. Catalog maintenance is accessed through the
upper-left menu, Item list. Add product shows a search field with live title-prefix
suggestions (case-insensitive; match the alphabet used in stored product names).
Empty search shows a prompt; unmatched text can be used to create a new product.
Do not add a separate visible Shopping Items tile to the home screen.

## List-card labels

Show only the list name on a home list card. Keep tap-to-open and long-press actions,
but do not display Tap to open / Hold for actions helper text below each name.

## Creating lists

Keep the home screen free of a permanent New list name input/Add form. The owner
changed this on 2026-10-21: show a + New list button at the upper right of the
shopping-list home screen, opening the existing creation dialog. Remove New list
from the hamburger menu. This supersedes the earlier menu-only creation rule.

## Initial lists

The owner approved Home, Weekend House and Vacation as default lists for empty
installations. Initialize once; preserve existing lists and do not recreate defaults
if the owner subsequently deletes them. This supersedes the earlier undecided default-list policy.

## Languages — owner decision 2026-10-22

Keep direct EN / MK choices at the drawer bottom, English as explicit default.
Localize app UI in Android resources; keep user-entered and stored names intact.
Use AppCompat per-app locales; see docs/LANGUAGES.md. New UI/status copy needs both
English and Macedonian Cyrillic. Always supply `git push origin master` when a
version is saved; the owner performs the push unless explicitly delegated.


## Standard list labels and media action order — owner update 2026-10-22

Display exactly Home / Weekend House / Vacation as Дома / Викендица / Одмор in MK.
Keep stored and cloud names unchanged, and retain custom names. Apply the display
label to cards, list headers, contextual titles, deletion text and invitations.
Prefill rename with the localized label; saving unchanged text must not rename data.

Product context menus in lists and catalog: View photo first when locally available,
then Open link when present, then Take photo, Choose from gallery, Add/Edit link,
Remove photo when present, and existing item edit/removal/deletion actions last.
Keep removal possible while a remote photo is awaiting download.
