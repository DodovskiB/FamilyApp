# Shopping List App — продолжување од друг разговор или компјутер

Последно ажурирање: **21 октомври 2026**, Europe/Skopje.

Овој документ е пренослива состојба на проектот, а не копија од целиот разговор.
Нов асистент треба прво да го прочита, па да го провери вистинскиот код и Git.
Не претпоставувај дека имаш пристап до претходниот разговор, компјутерот или cloud сметките.

## Како да продолжи корисникот

Прикачи го овој Markdown фајл во ChatGPT проектот **Home App** што го користиш
на другите уреди. Ако почнеш разговор надвор од тој проект, прикачи го и таму.
Фајлот во GitHub е изворната верзија; при замена користи ја најновата копија.
Локалните измени не се појавуваат автоматски во веќе прикачен ChatGPT фајл.

Почетна порака за новиот разговор:

> Прочитај го PROJECT_HANDOVER.md. Продолжуваме со Shopping List App од постојното
> repository https://github.com/DodovskiB/shopping-list-app, не правиме нов проект.
> Прво провери до кој commit имаш пристап и спореди со тековниот код. Кажи што
> можеш реално да читаш/менуваш во оваа сесија. Следната проверка е синхронизација
> на два физички телефона со истиот Google account, а потоа покани и Owner/Admin
> пристап по листа. Зачувај ги постојните податоци и не прави push без мое одобрение.

Само прикачувањето на документот не дава пристап до локалните фајлови, Android
Studio, телефоните или најавените browser tabs. Ако новата сесија нема repository
пристап, корисникот треба да го поврзе или да обезбеди source snapshot без тајни.
Не тврди дека си менувал или тестирал апликација без вистински пристап и резултати.

## Идентитет и локации

- Јавно име: **Shopping List App**. Старо име: FamilyApp / FamilyLists.
- GitHub: **https://github.com/DodovskiB/shopping-list-app**, branch **master**.
- На главниот Windows PC: `C:\Users\bojan\StudioProjects\shopping-list-app`.
- Старата папка `C:\Users\bojan\StudioProjects\FamilyApp` е преименувана; не ја користи.
- На друг компјутер користи ја неговата вистинска clone локација, не овие патеки наслепо.
- Android applicationId/namespace: **com.example.familyapp** — намерно останува.
- OAuth callback: **com.example.familyapp://auth-callback** — намерно останува.
- Базите, preferences и техничките package имиња остануваат за компатибилност.
- Launcher label, app screens, Android Studio/Gradle project и Google branding
  се Shopping List App. Не менувај applicationId само за козметичко преименување:
  Android би го сметал тоа за друга апликација.

## Последна потврдена состојба во Git

Корисникот потврди успешен push на `master` до **a037968** на 21 октомври 2026.

| Commit / tag | Содржина |
|---|---|
| `b597e42`, tag `stable-2026-10-21` | Работна верзија пред cloud sync: Google најава и локална account изолација |
| `713582e` | Same-account синхронизација, server SQL и тестови |
| `a037968` | GitHub/project branding во Shopping List App; README |

Овој документ е додаден потоа. При старт прочитај `git status -sb` и `git log -5
--oneline`; провери дали има понови commits. Не претпоставувај дека локален commit
е веќе пуштен. Непратени IDE фајлови како `.idea/deviceManager.xml`,
`.idea/easycode.ignore`, `.idea/easycode/` не се дел од функционалниот код;
не ги додавај во Git наслепо и не ги бриши корисничките фајлови.

## Технологии и архитектура

Android / Kotlin / Jetpack Compose / Room v5 / Flow / ViewModel.
Задржи: **Compose UI → ViewModel → Repository → DAO → Room**.
Некои source фајлови имаат необична физичка локација, а repository package е `repo`.
Прочитај ги declarations/imports; не прави масовен package refactor без потреба.

Важни фајлови/директориуми, релативно на repository root:

- `AGENTS.md` — инструкции за работа и тестирање.
- `PLAN.md` — договорени одлуки; дел од подолните секции се историски.
- `FAMILYAPP_VISION.md` — оригинална визија; историски документ, не доказ за имплементација.
- `docs/ACCOUNTS_AND_SHARING.md` — договорени улоги, покани и purchased однесување.
- `docs/CLOUD_SYNC.md` — актуелна синхронизација, тестови и ограничувања.
- `docs/SHOPPING_LISTS.md`, `PRODUCT_PHOTOS.md`, `LINKS_AND_MENU.md`, `HOME_AND_SEARCH.md`.
- `app/src/main/java/com/example/familyapp/MainActivity.kt`.
- `app/src/main/java/com/example/familyapp/auth/` — најава, secure storage, account UI.
- `app/src/main/java/com/example/familyapp/sync/` — SyncStore, SyncController, SyncTransport.
- `app/src/main/java/repo/`, `data/`, `ui/` и package declarations во реалниот source.
- `supabase/migrations/20261021_owner_sync.sql`, `supabase/tests/owner_sync.sql`.
- `app/src/androidTest/java/com/example/familyapp/` — storage/account/sync тестови.

## Што работи сега

- Почетниот екран директно ги прикажува shopping lists.
- Default листи Home, Weekend House, Vacation се создаваат еднаш на празна инсталација.
- Создавање, отворање, преименување и бришење листи.
- Посебен каталог на производи, име/категорија, уредување/бришење.
- Додавање од каталог во листа, prefix search, создавање производ при внесување.
- Количини со +/−, минимум 1; купено/некупено, отстранување производ од листа.
- Производот во каталогот и entry во листа се различни записи; количини/статуси
  се независни по листа. Бришење од листа не го брише каталогот.
- Камера/галерија за локална фотографија; optional http/https линк на каталог производ.
- Google најава преку Supabase, обновување сесија и одделни локални бази по account.
- Првиот најавен account ги презема постојните локални податоци; друга сметка не ги гледа.
- Cloud sync на листи, каталог текст/линкови, entries, количини и check статус
  за **истиот account на повеќе уреди**.

## Важни UI договори

- Нема постојани Actions/Rename/Delete копчиња под редовите.
- **Tap** отвора листа; **long press** нуди контекстни акции.
- Идните контекстни акции исто така одат преку long press на релевантното поле.
- Директните +/−, checkboxes, додавање и навигација остануваат директни контроли.
- Home cards го покажуваат само името, без „Tap to open — hold for actions“.
- Hamburger горе лево: Shopping lists, Item list (каталог), New list, Account.
- Нема посебна Shopping Items плочка или New list name форма на home.
- Празно product search поле покажува prompt, не цел каталог; внесен префикс дава suggestions.

## Cloud конфигурација и заштита

- Supabase Free, project display name Shopping List App, EU Ireland.
- Project ref: `kcfkwzrvkeggijokkmsx` (идентификатор, не тајна).
- Google Cloud project: `shopping-list-app-512222`.
- Google OAuth: External / Testing; сопственикот го потврди својот test-user login.
- Нови pilot accounts може да бараат додавање во Google test audience од сопственикот.
- Без платен plan, billing или trial; не активирај трошоци без одобрение.
- `local.properties` е игнориран и содржи SDK path, Supabase URL/publishable key.
- Google client secret е само во Supabase provider settings. Нема service-role key во APK/Git.
- Никогаш не ставај passwords, tokens, client secrets, приватни DB backups или лични
  податоци во handover документ, source archive или GitHub.
- На нов компјутер треба локална конфигурација; clone од GitHub не ја носи со себе.

Sync користи auxiliary SQLite mapping/outbox/meta tables и triggers во истата Room
база, без промена на Room entity schema. Промените се редат атомски со DAO writes.
Server RPC functions ја утврдуваат сметката преку auth.uid(); RLS дозволува само
сопствени записи. Direct writes се забранети. Операциите се idempotent.
Количините се спојуваат со deltas; за исто друго поле победува последната примена
промена на серверот. Tombstones спречуваат враќање на избришан запис од offline edits.

Ограничувања: polling на 5 секунди **додека апликацијата е отворена**, без background
sync service; фотографии се само локални; нема покани/меѓу-account споделување.
Server лимити: текст 512 знаци, линкови 8192, batch 100 операции/1 MiB. Одбиен batch
останува queued со генеричка retry порака. Retention/compaction на operations/tombstones
и подобра error презентација остануваат за подоцна.

## Проверки што навистина поминаа

- Gradle debug app/test APK builds и unit тестови.
- На Galaxy Note 20: **12 изолирани instrumentation тестови** — account 3,
  catalog/migrations 2, shopping lists/migrations 3, sync storage/retry 4.
- SQL tests на вистинскиот Supabase: RLS/account isolation, забранети direct writes,
  replay, quantity deltas, deletion cascade и no resurrection; fixtures се rollback.
- Opt-in LiveSyncTest на вистински сервер со **две изолирани Room бази на еден телефон**:
  creation/replay, concurrent qty, checked, rename, delete convergence.
- Работната апликација на Note 20 ги задржа податоците; Account покажа **Up to date**.
- По branding и преместување: build/unit тестови поминаа од новата папка, Android
  Studio го отвори проектот како Shopping List App, APK update-install и launch беа успешни.

Ова **не е** потврда за тест со два физички телефона. Тоа е следниот acceptance чекор.

## Безбедно тестирање и Windows околина

На главниот PC, JDK е `E:\Android Studio\jbr`; SDK е
`C:\Users\bojan\AppData\Local\Android\Sdk`. На лаптоп провери ги неговите патеки.

```powershell
git status -sb
git log -5 --oneline
$env:JAVA_HOME='E:\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Пред значајни измени направи Git checkpoint и приватна резервна копија по потреба.
**Не uninstall** на личниот телефон. Користи **adb install -r** за update.
**Не користи Gradle connectedAndroidTest на личниот телефон**: претходно runner cleanup
ја отстрани апликацијата. Користи isolated DB tests и директно am instrument.
Прво `adb devices`, избери го точниот физички уред; не претпоставувај серијски број.
Не допирај ја live базата со тестови и не печати лични записи или auth credentials.

Во ограничената агентска shell околина git push откажуваше со Windows/MSYS
NtCreateDirectoryObject / credential-manager грешка. Корисникот успешно пушташе од
својот Android Studio Terminal. Не менувај credentials или Windows security за да го
заобиколиш тоа. Push само по експлицитно одобрение за конкретните промени.

## Следни чекори, по ред

1. Провери latest Git/source состојба и дека двата уреда ја имаат актуелната верзија.
2. Тестирај истиот Google account на два физички телефона: download на постојните
   листи, додавање/уредување, concurrent qty/check, offline edits и повторно поврзување.
3. Поправи евентуални sync проблеми пред следната функционална фаза.
4. Имплементирај list ownership/membership и server-enforced permissions за различни
   accounts, со denial tests пред UI покани. Постојниот owner-only schema не е shared-list модел.
5. Покани на email/in-app acceptance, избрани листи и отповикување пристап;
   тестирај со различни accounts и физички телефони.
6. Доврши го договорениот Purchased workflow; cloud photos и идните функции подоцна.

## Договорени улоги и покани — сè уште НЕ имплементирани

- Секоја листа има еден **Owner**, поканетите се **Admins**, нема Viewer.
- Двете улоги гледаат/додаваат/уредуваат/отстрануваат entries, менуваат qty/check,
  преименуваат и прават Clear purchased.
- Само Owner поканува/отстранува членови и ја брише целата листа.
- Пристап само до конкретно споделените листи; другите приватни листи се невидливи.
- Без вештачки cap од 20, но Free quotas постојат; не ветувај неограничено бесплатно користење.
- Почетни покани во app, адресирани на најавениот email; optional Android share sheet.
  Автоматско праќање email не е вклучено. Не праќај пораки на други без овластување.
- Личниот каталог не станува автоматски видлив за членовите; деталите во споделената
  листа се достапни според membership. Photos/links sharing треба дополнително да се изведе.

Purchased договор: купените остануваат во долна Purchased секција со strikethrough;
uncheck ги враќа. Long-press Clear purchased со потврда ги брише само купените list
entries, не каталогот. Re-add на веќе купен производ прашува дали повторно е потребен
и користи новоизбрана количина. Не тврди дека ова е завршено — сè уште е планирано.

## Одржување на овој документ

Пред крај на секоја работна сесија ажурирај: последна потврдена GitHub верзија,
промените од сесијата, реални тест-резултати, ограничувања и точен следен чекор.
Разликувај извршено од планирано и локално зачувано од push-нато.
Корисникот потоа може да го замени фајлот во Home App со новата верзија.
