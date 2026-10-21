# Shopping List App

An Android shopping-list app built with Kotlin, Jetpack Compose and Room.

- Multiple shopping lists with quantities and purchased status.
- Reusable product catalog with optional local photos and product links.
- Google sign-in and synchronization between devices using the same account.
- List-specific Owner/Admin access with in-app email-addressed invitations.
- Context actions available by long-pressing lists and products.

Sharing is a private pilot; see [permissions and verification](docs/LIST_SHARING.md).
Photos currently stay on the phone. Automatic invitation emails are not enabled.

## Development

Open this repository in Android Studio. Use its bundled JDK and the existing Gradle
wrapper. Configure the Android SDK and the existing Supabase URL/publishable key in
ignored `local.properties`; see [account setup](docs/ACCOUNTS_AND_SHARING.md).
Never add client secrets, service-role keys or account credentials to the repository.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

To continue from another chat or computer, start with [the project handover](PROJECT_HANDOVER.md).
Use [the next-session checklist](NEXT_SESSION_CHECKLIST.md) for the testing and sharing rollout order.
See [the plan](PLAN.md), [sync behavior and verification](docs/CLOUD_SYNC.md), and
[the original product vision](FAMILYAPP_VISION.md).

## Naming and update compatibility

The public name is **Shopping List App**. This project was originally called
FamilyApp. The Android application ID `com.example.familyapp`, OAuth callback and
private database/preference names are intentionally retained so existing installs
continue to update and retain their data. These are internal identifiers, not the
app's launcher or screen title. Do not change them as a cosmetic rename.

Repository: https://github.com/DodovskiB/shopping-list-app
