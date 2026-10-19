# FamilyApp development guidance

Read FAMILYAPP_VISION.md for the product reference and PLAN.md for approved decisions.

## Interaction convention

The owner wants contextual actions exposed by long-pressing the relevant item/field.
Do not add persistent Actions/Rename/Delete buttons underneath list or product entries.
A normal tap on a shopping list opens it; long press exposes Rename/Delete.
Use this convention for future contextual actions. Preserve direct primary controls
such as navigation, adding, quantity +/-, and purchased checkboxes.

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

Keep the home screen free of a New list name input/Add form. New list belongs in
the upper-left navigation menu alongside Shopping lists and Item list.

## Initial lists

The owner approved Home, Weekend House and Vacation as default lists for empty
installations. Initialize once; preserve existing lists and do not recreate defaults
if the owner subsequently deletes them. This supersedes the earlier undecided default-list policy.
