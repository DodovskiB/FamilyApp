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
