# English / Macedonian

Owner decision, 2026-10-22: two direct EN / MK choices at the bottom of the
hamburger drawer. No settings screen or selection dialog. English is the explicit
initial default regardless of system language. Choice is local to each device.

AppCompatDelegate.setApplicationLocales handles recreation and persistence;
MainActivity now extends AppCompatActivity with a compatible NoActionBar host
style. Android 13+ uses system per-app locale storage; earlier versions use
AppCompat autoStoreLocales metadata. Both locales are declared in locales_config.
AppCompat 1.7.1 is the only added runtime dependency, for this platform feature.
The Compose theme, package/application ID, OAuth callback and database remain intact.

English copy lives in values/strings.xml and Macedonian Cyrillic in
values-mk/strings.xml. Format arguments keep names and quantities intact. Stable
ViewModel/account/sync messages are localized only at the presentation boundary
in LocalizedMessage.kt. Unknown messages are left unchanged. Future app-owned
status messages must add a mapping and EN/MK resources; never translate user data.

User-entered list/product/category names, existing default list names, emails,
links, stored category Other and the Shopping List App brand are retained.
Google/OS camera, gallery and authentication screens control their own language.
There is no translation service, server change, schema migration or cost.

Personal phone verification uses update-install and direct instrumentation, with
isolated in-memory fixtures. Do not use the Gradle connected runner or uninstall.
Honor needs its pending purchased/drawer/language APK update and pre-Android-13
locale persistence verification after the owner connects it.
