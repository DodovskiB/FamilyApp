# Shopping List App

An Android shopping-list app built with Kotlin, Jetpack Compose and Room.

- Multiple shopping lists with quantities and purchased status.
- Reusable product catalog with optional local photos and product links.
- Google sign-in and synchronization between devices using the same account.
- Context actions available by long-pressing lists and products.

Invitations and Owner/Admin sharing are planned; photos currently stay on the phone.

## Development

Open this repository in Android Studio. Use its bundled JDK and the existing Gradle
wrapper. Configure the Android SDK and the existing Supabase URL/publishable key in
ignored `local.properties`; see [account setup](docs/ACCOUNTS_AND_SHARING.md).
Never add client secrets, service-role keys or account credentials to the repository.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

To continue from another chat or computer, start with [the project handover](PROJECT_HANDOVER.md).
See [the plan](PLAN.md), [sync behavior and verification](docs/CLOUD_SYNC.md), and
[the original product vision](FAMILYAPP_VISION.md).

## Naming and update compatibility

The public name is **Shopping List App**. This project was originally called
FamilyApp. The Android application ID `com.example.familyapp`, OAuth callback and
private database/preference names are intentionally retained so existing installs
continue to update and retain their data. These are internal identifiers, not the
app's launcher or screen title. Do not change them as a cosmetic rename.

Repository: https://github.com/DodovskiB/shopping-list-app
