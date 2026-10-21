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
> и нејзината последна потврдена состојба на два телефона; online/offline тестот
> помина и со различни accounts. Owner/Admin sharing е имплементиран; прочитај
> docs/LIST_SHARING.md за последната acceptance состојба и ограничувањата.
> Зачувај ги постојните податоци и не прави push без мое одобрение.

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

Корисникот потврди успешен push на `master` до **41356ca** (Owner/Admin sharing,
in-app покани и physical acceptance со зачувани оригинални податоци).
Новата Purchased/+ New list работа е понова локална фаза, без одобрен push.
Види docs/PURCHASED_WORKFLOW.md за тековната имплементација и верификација.

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

Дополнително договорено: по стабилизација на sharing, дизајнот го доработуваме
екран по екран во Compose. Планирано Settings -> Language: English (default) и
Македонски (кирилица), со зачуван избор. Се преведува интерфејсот, не внесените
имиња на листи/производи. Ова сè уште не е имплементирано.

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

Горните автоматски live тестови беа на еден уред. Потоа е направена и следнава
физичка проверка со Note 20 и Honor.

Почнат е тој тест: Honor VNE-N41 е ажуриран со install -r, со приватни резервни
копии пред update и по најавата. Корисникот се најави со истиот Google account.
Старите три локални листи (4 entries) се увезоа покрај трите cloud листи (2 entries),
создавајќи шест листи со дупли имиња. Корисникот експлицитно одобри спојување на
трите пара со задржување на сите записи. Атомската серверска поправка и проверката
на локална backup база потврдија 3 листи, 6 entries со непроменети qty/check/други
полиња и 5 непроменети catalog записи. Honor е ажуриран со поправката и покажува
Up to date. First import сега бара combine/separate избор за еднозначни истоимени
листи; нема слепо спојување на cloud листи по име. Види docs/CLOUD_SYNC.md.
Build + 3 unit тестови, 15 изолирани device тестови, 2 live two-client тестови и
две SQL suites поминаа. Note 20 потоа е backed up и ажуриран; прикажува исти три
листи. Агентот преку UI создаде привремени „Sync proof 2250“ и „Sync proof product
2250“ на Note 20 со qty=2, checked=false. Корисникот потврди дека се појавија на
Honor, таму постави qty=3/checked=true, а агентот ги потврди тие вредности на Note 20.
Онлајн физичкиот двонасочен тест помина. Honor offline постави qty=4/checked=false;
Note 20 сè уште имаше 3/true, па таму независно беше направен уште +1. По reconnect
двата уреда покажаа **5/false**, потврдено од корисникот на Honor и преку UI на Note 20.
Тест-листата и тест-каталог производот се отстранети преку Note 20 UI; home повторно
прикажува Home/Weekend House/Vacation. Оригиналните 6 entries и 5 catalog записи не
беа менувани. Основниот physical online/offline/concurrent тест е завршен; следна
развојна фаза се Owner/Admin server rules, па покани, со различни accounts.

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

Детална работна чек-листа: `NEXT_SESSION_CHECKLIST.md` (подготвена за 22 октомври 2026).

1. Провери latest Git/source состојба и дека двата уреда ја имаат актуелната верзија.
2. Прочитај го потврдениот physical sync тест погоре. Повтори релевантни проверки
   ако има нови sync промени; не го прогласувај тестирањето за неизвршено.
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

## Owner/Admin sharing — тековна работа по 8e14b05

Види docs/LIST_SHARING.md за имплементацијата и ограничувањата. Миграцијата
20261021235000_list_sharing.sql е deployed. Owner управува со покани/членови и
ја брише листата; Admin ги менува нејзините entries, qty/check и име. Поканите
се in-app, за потврден Google email, со Accept/Decline и истек по 7 дена.
Нема email испраќање, нов платен сервис или нова библиотека.

Shared snapshots не го поместуваат personal cursor. Queue за shared edits се
испраќа кај вистинскиот Owner; при revoke се трга cached листата, а offline
промените се чуваат во приватен shopping_revoked_changes без повторен upload.
Cached податоци на offline телефон се тргаат по reconnect. Личниот каталог
останува приватен; photos/links sharing сè уште е подоцнежна задача.

Build/app/test APK/unit tests поминаа, 17 изолирани phone tests и 2 постојни live
sync tests поминаа. Проширените SQL owner/merge/sharing тестови поминаа со rollback.
Дополнителната поправка за локалниот catalog link кај Admin entries е изградена
и 2 sharing теста повторно поминаа на Honor. Приватни pre-update backup копии
се зачувани за двата телефона. Note 20 има sharing build; Honor го има поновиот
build со local catalog association поправката. Ажурирај Note 20 со последниот APK
кога повторно ќе се поврзе.

Преку Note 20 UI се создадени привремени Sharing test и Sharing test product,
и pending покана до договорениот втор тест account. Google OAuth test-user
пристапот е додаден. Не запишувај приватни email адреси или auth UUID во Git.
Физичката cross-account проверка е во тек; Honor е подготвен за рачна најава
со вториот account. Не тврди дека прифаќање/edit/revoke е физички потврдено сè уште.

Следно: заврши physical acceptance, offline/revoke/account isolation, исчисти
само тест записи и направи локален commit. Нема одобрение за push на новите измени.
Последниот потврден GitHub checkpoint останува 8e14b05.

## Потврден physical sharing acceptance — по 7480dd7

Honor е најавен со различен Google account; рачниот sign-out/sign-in помина.
Поканата се појави пред прифаќање. По Accept се појави само дозволената shared
листа, покрај новите лични defaults. Admin измени qty/check 1/false -> 2/true;
Owner го потврди на Note 20 и измени 3/false; тоа е директно видено на Honor.
Admin додаде сопствен тест-производ, повторно го додаде како qty 2 без дупла
entry, и го отстрани од листата. Неговите list actions имаа само Rename/Cancel.
19 изолирани instrumentation tests (2 UI, 2 sharing storage, 15 претходни) поминаа
на Honor со последниот main APK; новиот test APK исто е изграден и инсталиран.

Honor потоа беше offline; тест-количината стана 4. Приватен backup потврди 1
pending operation. Owner преку Note 20 UI го одзеде пристапот. По reconnect
листата исчезна од Honor. Втор backup потврди 0 shared access, 0 pending, 0 entries,
3 лични листи и 1 quarantined delta +1. List/item/catalog табелите на претходниот
account останаа точно исти меѓу backup-ите. Привремениот Admin catalog производ
е избришан; Honor Account покажува Up to date. Нема споделено вистинска корисничка
листа автоматски. Тест-поканата е отповикана.

Завршено: Note 20 е повторно backed up и update-installed со истиот последен
APK како Honor. Owner UI директно потврди qty 3/false по revoked offline +1.
Sharing test и Sharing test product се отстранети преку UI. Account покажува
Up to date. Финален приватен backup и SQLite споредба потврдија 3 листи, 6 entries,
5 catalog производи, 0 pending; сите оригинални колони/редови се непроменети.
Привремениот Admin catalog производ исто е отстранет од Honor, а тест пристапот
останува отповикан. Нема автоматски споделена вистинска листа.

Ова ја заменува претходната pending cleanup состојба. 19 phone tests, 3 unit tests,
2 live sync tests, SQL security/lifecycle и physical two-account online/revoke
проверките се поминати. Нов код не беше потребен при завршната проверка.
Нема одобрение за push; последен потврден GitHub checkpoint е 8e14b05, локалниот
sharing code commit е 7480dd7 и потоа има acceptance документациски commits.
Google branding verification е во чек-листата пред поширока дистрибуција;
не активирај платен Supabase custom domain. Photos/links sharing и Clear purchased
остануваат идни задачи. Прво договори следен чекор со корисникот; не започнувај
нова функција автоматски по оваа завршна проверка.

## Purchased и + New list — завршена локална фаза, 22 октомври 2026

Owner одобри: To buy / Purchased секции со купените долу/прецртани; uncheck ги
враќа горе; long-press Clear purchased со потврда за Owner и Admin; повторно
додавање купен catalog производ бара Buy again потврда и нова количина.
Потврден нов циклус ги крие старите purchased entry и создава нов unchecked entry
со избраната количина, без менување каталог или старата qty преку неподдржан
absolute sync patch. Clear е scoped Room transaction со active=false patches.
Нема Room миграција/серверска миграција/нова библиотека/нов сервис.

Owner го промени претходното правило за New list: отстранет од hamburger, ставен
како + New list горе десно на home, што го отвора постојниот Create дијалог.
Нема постојан name input на home. AGENTS.md е усогласен со новото барање.

Build app/test APK и 3 unit tests поминаа. 19 изолирани избрани phone checks
поминаа (18 првично и поправениот dialog-scoped UI selector потоа). Сите 3 live
sync tests поминаа, вклучувајќи нов test за repeat/clear/catalog retention меѓу
два изолирани клиенти. Нема тврдење за нов physical 2-phone purchased тест.
Note 20 е backed up/update-installed; + New list/Create/Cancel и отсуство од
менито се видени на вистинскиот UI. Account покажува Up to date. Финален backup
потврди сите оригинални полиња непроменети: 3 lists, 6 entries, 5 catalog, 0 pending.
Honor го има претходниот sharing APK и чека update кога ќе се поврзе.

Последен потврден GitHub checkpoint е 41356ca. Новите измени се локални;
не прави push без ново одобрение. Следно: корисничка проба на Purchased на Note 20,
update на Honor кога е достапен, па договорените design/language чекори според
кориснички приоритет. Photos/links sharing останува планирано.

## Drawer и Clear purchased footer — последна корекција, 22 октомври 2026

Owner побара и е направено: account/email прв и издвоен со простор/линија, под него
Invitations, Shopping lists, Item list последно. Clear purchased НЕ е повеќе со
long press на home листата. Сега е внатре во отворената листа, како издвоено
outlined копче со простор и divider под последниот purchased entry. Се прикажува
само кога има купени entries; потврдата останува. Ова е изричен исклучок на старото
правило за сите contextual actions преку long press; AGENTS.md е усогласен.

Нема промени на Repository/DAO/sync/permissions. Build app/test APK помина и
5 релевантни isolated UI tests поминаа. Note 20 е privately backed up и ажуриран;
реалниот drawer редослед/издвојување е визуелно потврден. Финален SQLite backup
потврди сите оригинални list/item/catalog полиња непроменети, со 0 pending.
Последен потврден GitHub commit останува 41356ca. Purchased фазата e9d360b и оваа
поновa корекција се локални, без одобрение за push. Honor сè уште чека update.

## EN / MK — последна состојба, 22 октомври 2026

Owner побара директни EN / MK копчиња најдолу во hamburger menu, без дополнителен
Settings екран. Имплементирани се 132 EN/MK ресурси за UI, потврди, accessibility,
account/sharing/sync пораки. English е експлицитен default, MK е кирилица. Изборот
е по уред и се зачувува со официјален AppCompat per-app locale механизам.
MainActivity е AppCompatActivity со соодветна host тема; Compose архитектурата,
package, DB и OAuth callback остануваат. Види docs/LANGUAGES.md за деталите.
Не се преведуваат внесените имиња, постојните default списоци/категории или брендот.

Checkpoint пред работа: checkpoint-before-languages-2026-10-22 на 60a07e0.
Последен owner-confirmed GitHub push е 60a07e0; ова го заменува постарото 41356ca
тврдење погоре. Новата EN/MK верзија е локална додека owner не направи push.
Секогаш дај git push origin master по зачувување; owner засега ја извршува командата.

Проверки: app/test APK build успешен, 3 unit tests успешни, lint успешен со
постојни photo/dependency/config предупредувања (не е тврдење дека е warning-free).
7 постојни isolated UI tests и новиот MK home/catalog/language-choice тест поминаа.
132 EN/MK ресурси имаат усогласени format аргументи. Синтетичкиот локализиран test
context го проверува home/catalog; вистинскиот MK Add product dialog е проверен
одделно на Note 20. Првичните проблеми беа во test fixture/selector и се поправени.

Note 20 е privately backed up и update-installed. Реално EN -> MK, целосно force-stop
и повторно отворање го задржуваат MK; MK dialog преводот е потврден. Вратен е EN.
Backup comparison: сите оригинални list/item/catalog полиња непроменети, 3 lists,
6 entries, 5 catalog, 0 pending. Нема нова physical two-phone sync проверка во оваа фаза.
Завршното test APK градење е преку Android Studio: shell Gradle повторно доби
Windows project-directory write ограничување иако grants се дадени. IDE build помина.
Не менувај security settings; користи IDE Gradle task launcher ако ова се повтори.

Следно: owner да поврзе Honor со USB, отклучен, со интернет. Направи приватен backup,
update-install со сите purchased/drawer/EN-MK измени, зачувај го неговиот account и
податоци. Провери EN/MK persistence и на постариот Android и видливоста/синхронизацијата
на постојните споделени листи. Не користи connected test runner или uninstall.
