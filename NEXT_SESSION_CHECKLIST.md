# Shopping List App — чек-листа за продолжување

Подготвено: 21 октомври 2026. За следната сесија: 22 октомври 2026.

Цел: прво да ја потврдиме синхронизацијата на два вистински телефона, па да
почнеме безбедно споделување избрани листи меѓу различни Google accounts.
Ова е редослед на работа, не ветување дека сите фази ќе завршат за еден ден.

Тековен напредок: Honor е ажуриран и најавен со истиот account. Дуплите увезени
листи се споени со изрично одобрение, без губење на 6-те entries/5-те catalog записи.
Поправката за initial-import избор е тестирана и инсталирана на Honor; Up to date.
Note 20 е ажуриран и онлајн двонасочниот тест со привремен производ помина
(2/unchecked од Note 20 до Honor; 3/checked од Honor назад до Note 20).
Offline/concurrent проверката исто помина: 3 -> независно +1 на двата уреда ->
5/unchecked на двата по reconnect. Привремените записи се отстранети преку Note 20.
Owner/Admin и in-app поканите сега се имплементирани; physical online/revoke
проверките поминаа. Види ја последната checkpoint секција и docs/LIST_SHARING.md.

## 1. Подготовка од корисникот

- [ ] Главниот компјутер со проектот и Android Studio е достапен.
- [ ] Ако работиме од друг уред/разговор, прикачен е актуелниот PROJECT_HANDOVER.md.
- [ ] Двата Android телефона се достапни, отклучени и со интернет.
- [ ] За update-install: USB debugging и потврден пристап на точниот уред.
- [ ] За првиот sync тест: истиот Google account на двата телефона.
- [ ] За подоцнежниот sharing тест: втор, различен Google account под контрола
  на корисникот/тестерот. Сопственикот сам ги завршува најавите и согласностите.

Нема потреба однапред да се отвора нов сервер, да се плаќа сервис или да се
инсталира Android Studio на телефоните.

## 2. Почетна проверка — пред нов код

- [ ] Прочитај AGENTS.md, PROJECT_HANDOVER.md, PLAN.md и docs/CLOUD_SYNC.md.
- [ ] Провери Git status, последни commits и remote адреса.
- [ ] Разликувај локални commits од потврден push. Последниот потврден GitHub
  code checkpoint е 8e14b05; новите sharing измени сè уште не се pushed.
- [ ] Проект: C:\Users\bojan\StudioProjects\shopping-list-app, branch master.
- [ ] Заштити непратени промени; направи checkpoint пред новата развојна фаза.
- [ ] Пред промени на live податоци направи приватна резервна копија по потреба.
- [ ] Провери build. Ако е расипан, поправи го пред додавање функции.

## 3. Прв приоритет: same-account sync на два физички телефона

- [x] Ажурирај ги двата телефона со иста актуелна APK верзија преку install -r.
  Не uninstall и не користи Gradle connectedAndroidTest на личен телефон.
- [ ] Најави го истиот Google account; провери дека се појавуваат истите листи
  и производи, без дуплирање на Home / Weekend House / Vacation.
- [ ] Остави ги двете апликации отворени и провери Account → Up to date.
- [x] Во посебна тест-листа додај производ на едниот телефон; провери го на другиот.
- [ ] Провери rename, qty +/− и check/uncheck во двете насоки.
- [x] Провери истовремено менување количина: двата прираста треба да се зачуваат.
- [x] На едниот уред исклучи интернет, направи промени и повторно поврзи го.
  Провери дека промените се испраќаат без дупликати кога app е отворена.
- [ ] Провери отстранување entry и бришење само на тест-листа, со потврда.
- [ ] Рестартирај app и провери сесија, зачувани податоци и sync статус.
- [ ] Ако има проблем: поправка и релевантни тестови пред следната фаза.

Критериум: двата уреда се усогласуваат без губење постојни податоци.
Сликите се локални; не е грешка ако сè уште не се пренесуваат.
Sync работи додека app е отворена, не како background service.

## 4. Втор приоритет: безбедна основа за Owner/Admin sharing

- [x] Прегледај го постојниот owner-only cloud schema и подготви миграција што
  ги задржува постојните листи, UUID mappings, queues и податоци.
- [x] Дефинирај list ownership, membership и invitation lifecycle, со јасни
  правила за прифаќање, одбивање, истекување/откажување и повторена покана.
- [x] Имплементирај server-enforced пристап само до конкретната споделена листа.
- [x] Единствен Owner; поканетите се Admins; нема Viewer.
- [ ] Двете улоги можат да менуваат entries, qty/check, име на листата и
  да користат идно Clear purchased. Само Owner управува со членовите и ја брише листата.
- [x] Не откривај личен каталог, други приватни листи или туѓи accounts.
- [x] Приспособи sync да презема дозволени shared lists со ист идентитет за
  сите членови; спречи погрешно презапишување ownership при upload/import.
- [x] Дефинирај што се случува со локалната копија и offline queued edits по
  одземање пристап. Server мора да одбие нови недозволени промени.
- [x] Тестови со различни accounts: Owner дозволено, Admin дозволено,
  непоканет корисник одбиен, друга приватна листа невидлива, revoked член одбиен.

Критериум: дозволите се на серверот и се проверени пред да ја додадеме invite UI.

## 5. Трет приоритет: покани и тест со различни accounts

- [x] Owner преку long press на листата добива Share / Manage members.
- [x] Покана адресирана на email; листа со pending invitations за соодветниот account.
- [x] Accept / Decline и прикажување на прифатената листа на home.
- [x] Owner може да види членови и да отповика пристап.
- [x] На другите accounts по потреба додади Google OAuth test-user пристап.
- [x] Провери два физички телефона со различни accounts и една споделена листа.
- [ ] Провери дека Admin ги гледа сите entries и може да додава, менува,
  отстранува, check/uncheck, а приватните листи на Owner не ги гледа.
- [x] Провери revoke, sign-out/account switch и offline/reconnect.

Почнуваме со in-app покани. Автоматски email пораки не се вклучени;
не додавај email сервис или праќање пораки без посебен договор.

## 6. Следно по основното sharing

- [ ] Дизајн екран по екран во постојниот Compose проект, според примерите и
  повратните информации на корисникот.
- [ ] Settings -> Language: English (default) / Македонски (кирилица), зачуван избор;
  превод на UI текстовите без менување кориснички имиња на производи/листи.

- [x] Purchased секција со strikethrough и uncheck за враќање.
- [x] Clear purchased footer под purchased entries, со потврда: се тргаат само купените entries,
  каталогот останува.
- [x] Re-add на купен производ: потврда дека повторно е потребен и нова количина.
- [ ] Посебно планирање за приватно споделување фотографии, права и storage quotas.
- [ ] Подготовка на APK за мал круг тестери со соодветно потпишување и
  зачуван клуч за идни updates. Не објавувај инсталер пред договор за дистрибуција.
- [ ] Избери начин за испорака на APK/download линк; Google Play е подоцнежна фаза.
- [ ] Пред поширока дистрибуција: Google Branding verification за името/логото
  Shopping List App на Google consent screen. Browser OAuth засега го покажува
  Supabase project доменот. Не активирај платен custom domain без одобрение.

## 7. Завршување на секоја сесија

- [ ] Build и тестови соодветни на промените; преглед на Git diff.
- [ ] Краток извештај: што е направено, што е проверено и што останува.
- [ ] Локален Git commit на проверената работа.
- [ ] Push само со одобрение за конкретните промени; потврди успешен резултат.
- [ ] Ажурирај PROJECT_HANDOVER.md и оваа чек-листа со реалната состојба.
- [ ] Дај актуелна handover копија за замена во ChatGPT Home App.

## Порака за почеток утре

> Продолжуваме со Shopping List App. Прочитај PROJECT_HANDOVER.md и
> NEXT_SESSION_CHECKLIST.md. Прво провери Git и пристапот до вистинскиот проект.
> Почни со проверка на same-account sync на два физички телефона. Ако помине,
> продолжи со Owner/Admin list sharing според договорените правила. Зачувај ги
> постојните податоци, без платени сервиси и без push без мое одобрение.

## Owner/Admin checkpoint after 7480dd7

In-app invitations and the server-enforced Owner/Admin model are implemented.
19 isolated Honor tests (including 2 UI tests), 3 unit tests, 2 live sync tests
and SQL security/lifecycle regressions passed. Two physical accounts passed
online edits in both directions and offline revocation with queue quarantine.
See docs/LIST_SHARING.md for the observed proof and remaining pilot limitations.
Final Note 20 update and disposable-data cleanup passed. Original 3 lists / 6
entries / 5 catalog products are unchanged, with 0 pending. Clear purchased and
cloud photo/link sharing are still later work; unchecked combined requirements
containing those features are deliberately left unchecked.

## Последна локална фаза — Purchased / + New list

Purchased/re-add/clear и + New list горе десно се имплементирани и верифицирани.
Види docs/PURCHASED_WORKFLOW.md. Note 20 ажуриран; Honor чека update.
Build/unit tests, 19 избрани изолирани checks и 3 live sync tests поминаа.
Оригиналните 3/6/5 записи се непроменети и sync queue е празна.
Потврден GitHub commit: 41356ca; поновите измени сè уште не се pushed.
Следно: user acceptance на Purchased, Honor update, design и EN/МК според договор.

Последна UI корекција: drawer = account (издвоен) -> Invitations -> Shopping lists
-> Item list. Clear purchased е footer внатре во листата, не long-press акција.
App/test build и 5 UI tests поминаа; Note 20 ажуриран, оригинални податоци непроменети.

## Најново — 22 октомври, EN / MK завршено на Note 20

- [x] Последен потврден GitHub push: 60a07e0.
- [x] Директни EN / MK најдолу во hamburger menu, English default, зачуван избор.
- [x] MK кирилични UI/dialog/account/sharing/sync ресурси; имињата се задржани.
- [x] Build, 3 unit tests, lint и 8 selected isolated UI tests поминаа.
- [x] Note 20 update и MK cold-restart проверка; вратен EN; оригинални податоци зачувани.
- [ ] Owner push на новата локална верзија: git push origin master.
- [ ] Поврзи Honor, приватен backup, update-install без uninstall.
- [ ] Провери јазик по restart на Honor и двата account-а/shared листи.
- [ ] Потоа договор за следен дизајн/продуктен чекор; photos/links sharing останува идно.

## Honor update — потврдено 22 октомври

- [x] Приватен backup и update-install со најновата EN/MK APK.
- [x] MK останува по cold restart на Android 12; вратен EN.
- [x] Вториот account задржан, Up to date, оригинални записи непроменети во двете бази.
- [ ] Следен двонасочен purchased/shared edit тест по потреба.
- [ ] Договор со owner дали да подготвиме Play Internal testing (US$25 developer регистрација)
      или засега директен APK. Нема одобрен Play upload/уплата.

## Бесплатен APK preview — следно

- [x] 1.1-preview/2 release APK изграден, потпишан и signature/manifest/content проверен.
- [x] Initial empty catalog/entries, rename/delete/default persistence и account isolation: 5 checks pass.
- [ ] Owner: offline backup на приватниот .android/shopping-list-app-signing folder.
- [ ] Прв new tester: Google email, OAuth audience проверка, fresh APK install и login/sharing проба.
- [ ] Не заменувај debug Note 20/Honor со release APK без договорена миграција.
- [ ] Owner push на packaging code/docs: git push origin master. Нема јавен APK upload.

## Утрешна проба кај сопругата

- [x] Нов release APK 1.1.1-preview/3 со Product catalog / Каталог на производи.
- [x] Release/test build и signature verification поминаа; истиот release key.
- [ ] Користи го новиот 1.1.1-preview APK за новиот телефон.
- [ ] Провери Google email/OAuth audience, fresh defaults, login и sharing.
- [ ] Owner push за rename commit: git push origin master.
