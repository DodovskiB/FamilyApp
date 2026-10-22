# Free private APK distribution

Approved 2026-10-22: distribute a signed APK directly, with no Play Console purchase
or paid hosting. Current preview: versionName 1.1.1-preview, versionCode 3.

## First use

A genuinely new installation starts in English with Home, Weekend House and
Vacation, no shopping entries and an empty personal catalog. The owner can rename
and delete these defaults. They are initialized once, not recreated after deletion.
An existing account receives its existing server lists after sign-in; a shared list
invitation can also bring its own entries. Never clear existing users' databases to
force the fresh-install state. No personal database or account session is in the APK.

## Producing a package

1. Build `:app:assembleRelease` in Android Studio (or Gradle when shell access works).
2. Set JAVA_HOME to the local JDK and run scripts/Package-PrivateApk.ps1.
3. The script signs the unsigned release APK and verifies its signature. Default
   output is build/distribution/, ignored by Git. Pass -OutputApk for a different path.
4. Share only the signed APK and user instructions. Never share the signing folder,
   credentials, local.properties or database backups.

The dedicated release signing key is outside this repository, in the current
Windows user's .android/shopping-list-app-signing folder. Back up that whole folder
securely offline, including the password file. Do not regenerate the key for updates.
The source repository and GitHub do not contain the key/password or APK binaries.
The default filename reads versionName from Gradle's release output metadata.

## Updates

Changes to application code/design require another APK. Increase versionCode, keep
application ID and release signing key, then users open the new APK and choose
Update. Do not uninstall; normal compatible updates preserve data. Shared-list
content changes sync independently and do not require reinstalling the APK.
Automatic APK download/install is not implemented. No public upload is authorized.

The owner's Note 20/Honor currently use the original DEBUG certificate. This release
certificate is different: the release APK will not replace those debug installs.
Keep both test phones intact. Any later move to the release certificate needs an
explicit data-preserving migration plan. New testers start with the release APK.

## Google sign-in and sharing

Local lists can be used without sign-in. Google sign-in plus list invitations are
needed for shared lists. Play/APK install permission does not grant list access.
If Google OAuth is still in Testing, add each tester's Google email to its audience
before testing sign-in. Do not promise unrestricted login until checked with a new
allowed tester. List-entry photos and links are shared with that list's current members in
1.2.0-preview/4. The personal catalog remains private. See SHARED_PRODUCT_MEDIA.md.

## Catalog name (approved 2026-10-22)

The owner chose `Product catalog` for both drawer and screen title (MK: Каталог на производи). Keep the
catalog initially empty. Persistence enables prefix suggestions, reusable products,
photos and links. Potential later features: barcode lookup, favorites, frequently
bought products and price history. None is added by this packaging work.
