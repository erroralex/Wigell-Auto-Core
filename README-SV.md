# Wigell AutoCore
 
Verkstadssystem för Wigellkoncernens fordonsservice: kunder, fordon, bokningar, arbetsorder,
fakturor och betalningar. JavaFX-gränssnitt byggt i kod (ingen FXML), Spring Boot 4, Java 21 och SQLite.
 
# Arkitektur: modulär monolit
 
Projektet är en Maven-reactor med tre moduler. All affärslogik ligger i `autocore-core`, och de två
körbara modulerna bygger på den.
 
```
                 ┌────────────────────┐
                 │   autocore-core    │   Domän, tjänster, repository-interface, felhantering, i18n
                 │ (ingen Boot/JavaFX)│
                 └─────────▲──────────┘
                           │ beror på
             ┌─────────────┴─────────────┐
   ┌─────────┴──────────┐      ┌─────────┴──────────┐
   │    autocore-app    │      │ autocore-standalone│
   │ Spring Boot, JPA,  │      │ Utan Spring, GUI   │
   │ SQLite, Flyway,    │      │ och databas        │
   │ JavaFX, headless   │      │ (påbörjas i 4.0)   │
   └────────────────────┘      └────────────────────┘
```
 
| Modul | Innehåll | Får bero på | Får INTE bero på |
|---|---|---|---|
| `autocore-core` | Entiteter, värdeobjekt, tjänster, repository-interface, undantag, felkedja, översättning, drop-in-scenariot | Spring Context/Tx, Spring Data JPA (interface), Jakarta Persistence/Validation, SLF4J | Spring Boot, JavaFX, `com.wac.autocore.view` |
| `autocore-app` | Spring Boot-start, JavaFX-vyer, headless-läge, `application*.properties`, Flyway-migreringar, språkfiler | `autocore-core`, Spring Boot-startrar, JavaFX, SQLite | – |
| `autocore-standalone` | Fristående körläge som kopplar ihop tjänsterna med `new` mot in-memory-repositoryn | `autocore-core` | Spring-kontext, JavaFX |
 
**Regeln vaktas av tester:** `NoJavaFxInCoreTest` i core underkänner bygget om någon kod i core
refererar till `javafx.` eller `com.wac.autocore.view`.
 
**Var lägger jag ny kod?**
- Affärsregel, ny entitet, ny tjänstemetod → `autocore-core`
- Ny vy, dialog, CSS, Spring-konfiguration, migrering, språktext → `autocore-app`
- Ny databastabell eller kolumn → ny `V14__...sql` och framåt i `autocore-app`. **V1–V13 rörs aldrig.**
# Struktur
 
Alla moduler använder samma baspaket, `com.wac.autocore`. Varje klass ligger alltså i samma paket
som före uppdelningen, bara i en annan modul.
 
```
wigell-auto-core/
├── pom.xml                              # Reactor-pom: parent spring-boot-starter-parent 4.x, Java 21, listar modulerna
├── mvnw  mvnw.cmd  .mvn/                # Maven Wrapper – använd alltid ./mvnw
├── .github/workflows/build-check.yml    # CI: ./mvnw -B clean verify på Java 21 för PR mot main och dev
├── README.md  STYLEGUIDE.md
│
├── autocore-core/                       # Logiken. Ingen Spring Boot, ingen JavaFX
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wac/autocore/
│       │   ├── model/                   # JPA-entiteter och domänlogik
│       │   │   ├── Customer  Vehicle  Mechanic  ServiceItem  ServicePackage
│       │   │   ├── Booking  BookingServiceItem  BookingStatus     # Bokning + tjänster med avtalat pris (snapshot)
│       │   │   ├── WorkOrder                # Abstrakt bas, SINGLE_TABLE-arv, Template Method i confirm()
│       │   │   ├── PlannedWorkOrder  DropInWorkOrder  WarrantyWorkOrder   # Ordertyper: createFrom(Booking) / draft(...)
│       │   │   ├── WorkOrderType  WorkOrderItem  ServiceLines
│       │   │   ├── WorkOrderStatus          # State: livscykel och tillåtna övergångar
│       │   │   ├── WorkOrderChange  WorkOrderChangeType           # Ändringshistorik på ordern
│       │   │   ├── Invoice  InvoiceLine  Payment
│       │   │   └── value/                   # Värdeobjekt
│       │   │       └── Money  MoneyFormat  TimeSlot
│       │   ├── repository/                  # Repository-interface, en per aggregat (ärver Spring Data Repository)
│       │   ├── service/                     # Affärslogik, @Service och @Transactional
│       │   │   ├── BookingService  CustomerService  VehicleService  MechanicService
│       │   │   ├── ServiceItemService  ServicePackageService  WorkOrderService
│       │   │   ├── InvoiceService  PaymentService
│       │   │   ├── BookingCopyResult  BookingPriceChange  WorkOrderReopenResult  WorkOrderScheduleResult
│       │   │   └── discount/                # Strategy: rabattregler för fakturor
│       │   │       └── DiscountStrategy  NoDiscount  VipDiscount  Welcome10Discount  Service200Discount
│       │   ├── exception/                   # Domänfel med språknyckel (DomainException och subklasser)
│       │   │   └── handler/                 # Chain of Responsibility för felvisning
│       │   │       └── ErrorHandler  ErrorHandlerChain  ErrorReport
│       │   │           DomainErrorHandler  ValidationErrorHandler  DataAccessErrorHandler  FallbackErrorHandler
│       │   ├── i18n/                        # Översättning utan JavaFX, delas av GUI och headless
│       │   │   └── Translator  ResourceBundleTranslator  LocalizedError  ErrorReportFormatter
│       │   ├── headless/                    # Drop-in-scenariot som headless kör
│       │   │   └── DropInScenario  ScenarioContext  ScenarioStep  StepResult
│       │   └── data/                        # JPA-konverterare: datum/tid som TEXT, Money som BigDecimal
│       │       └── LocalDateConverter  LocalTimeConverter  LocalDateTimeConverter  MoneyConverter
│       └── test/java/com/wac/autocore/
│           ├── unit/                        # Rena enhetstester av modellen och värdeobjekten
│           ├── service/                     # Tjänstetester med Mockito
│           ├── exception/handler/           # Test av felkedjan
│           ├── architecture/NoJavaFxInCoreTest.java   # Vaktar modulgränsen
│           ├── WorkOrderLifeCycleIntegrationTest.java # Hela livscykeln utan Spring och utan databas
│           └── testsupport/                 # In-memory-repositoryn och MoneyAssert
│
├── autocore-app/                        # Spring Boot + JavaFX + SQLite. Den körbara applikationen
│   ├── pom.xml                          # spring-boot-maven-plugin bygger en körbar jar
│   └── src/
│       ├── main/java/com/wac/autocore/
│       │   ├── Main.java                    # Startpunkt
│       │   ├── AutoCoreConfig.java          # @SpringBootApplication – skannar com.wac.autocore i alla moduler
│       │   ├── headless/HeadlessRunner.java # @Profile("headless"): kör DropInScenario utan GUI
│       │   └── view/                        # JavaFX-vyer, byggda i kod (ingen FXML)
│       │       ├── AutoCoreApp              # Application: startar/stänger Spring, bygger scenen
│       │       ├── MainLayout  SideNavigation  NavigationItem   # Skal och sidomeny
│       │       ├── BaseView                 # Template Method: gemensam ram för alla vyer
│       │       ├── HomeView  CustomerView  VehicleView  BookingView  MechanicView  MechanicBookingsView
│       │       ├── ServiceItemView  ServicePackageView  WorkOrderView  InvoiceView  PaymentView
│       │       ├── component/               # Återanvändbara kontroller
│       │       │   └── CustomerVehiclePicker  MultiSelectListView  ServiceSelectorBox  ServicePackageSelectorBox
│       │       ├── dialog/                  # Create*/Edit*-dialoger, InvoiceDetail, ProcessPayment, QuickDraftWorkOrder m.fl.
│       │       ├── i18n/LanguageManager     # Språkbyte sv/en i GUI:t, bygger på core:s Translator
│       │       └── util/
│       │           ├── ErrorFacade          # Facade: en ingång för att visa fel via handler-kedjan
│       │           ├── GlobalExceptionHandler   # Fångar ohanterade undantag och skickar till ErrorFacade
│       │           └── AlertHelper  DialogUtil  StringConverterUtil
│       ├── main/resources/
│       │   ├── application.properties           # SQLite (autocore.db), foreign_keys=true, ddl-auto=none, Flyway
│       │   ├── application-headless.properties  # Egen databasfil autocore-headless.db, tyst logg
│       │   ├── db/migration/                    # Flyway äger schemat, V1–V13 (+ .conf där migreringen körs utan transaktion)
│       │   ├── i18n/messages*.properties        # Texter på svenska och engelska
│       │   └── com/wac/autocore/view/           # style.css och assets/ (logga, ikon)
│       └── test/
│           ├── java/com/wac/autocore/
│           │   ├── integration/             # Mot riktig SQLite som Flyway bygger: migreringar, prishistorik,
│           │   │                            # arv, headless-scenariot, bevarande av 3.5-data
│           │   ├── service/                 # Validering via Spring, språkfilerna, LanguageManager
│           │   └── view/i18n/               # Samma feltexter i GUI och headless
│           └── resources/
│               ├── application-test.properties   # ddl-auto=validate
│               └── fixtures/autocore-3.5.db      # Kopia av en WAC-3.5-databas för datatestet
│
└── autocore-standalone/                 # Fristående läge utan Spring, GUI och databas
    └── pom.xml                          # Beror bara på autocore-core. Koden byggs i sprint 4.0
```
 
# Bygga och köra
 
Kräver **JDK 21**. JavaFX hämtas som Maven-beroende, så ingen JDK med inbyggt FX behövs.
 
```bash
./mvnw clean verify          # Bygger alla moduler och kör alla tester (samma som CI)
./mvnw clean package -DskipTests
```
 
Bygga och testa bara en modul (med de moduler den beror på):
```bash
./mvnw -pl autocore-core verify
./mvnw -pl autocore-app -am verify
```
 
Starta gränssnittet:
```bash
java -jar autocore-app/target/autocore-app-4.0.0.jar
```
 
> Databasfilen `autocore.db` skapas i den mapp du startar från. Starta från repots rot så
> används samma databas varje gång.
 
# Köra utan gränssnitt (headless)
 
Applikationens tjänster kan köras utan JavaFX. Headless-läget kör ett komplett drop-in-flöde
(skapa, lägg till tjänster och mekaniker, bekräfta, starta, slutför, fakturera, betala) och därefter
två otillåtna operationer, och skriver ut resultatet med samma felnycklar och texter som gränssnittet.
 
```bash
./mvnw clean package
java -jar autocore-app/target/autocore-app-4.0.0.jar --spring.profiles.active=headless
```
 
Engelska:
```bash
java -jar autocore-app/target/autocore-app-4.0.0.jar --spring.profiles.active=headless --headless.lang=en
```
 
- Scenariot (`DropInScenario`) ligger i core, och `HeadlessRunner` i app kör det med Spring och SQLite.
- Kör mot en egen databasfil, `autocore-headless.db`. Den vanliga `autocore.db` rörs inte.
- Avslutar med kod `0` om alla steg blev som förväntat, annars med en felkod.
- Samma scenario körs som integrationstest i `HeadlessDropInScenarioIntegrationTest`.
**Windows:** visas `å`, `ä` och `ö` fel i konsolen, kör `chcp 65001` först.
 
# Fristående läge (standalone)
 
`autocore-standalone` kör samma domän- och servicekod som appen, men utan Spring, GUI och databas.
Tjänsterna skapas med `new` och kopplas mot in-memory-repositoryn. Modulen paketeras som en egen
körbar jar.

## Köra programmet i fristående läge:

Bygg den körbara jaren och starta den med `java -jar`, som vilket fristående program som helst:

```bash
./mvnw clean package -DskipTests
java -jar autocore-standalone/target/autocore-standalone.jar --role=READ_ONLY --lang=sv
echo $?
```

| Argument | Värden | Standard |
|---|---|---|
| `--role` | `ADMIN`, `WORKSHOP`, `READ_ONLY` (skiftlägesokänsligt) | `ADMIN` |
| `--lang` | `sv`, `en` | `sv` |

| Felkod | Betydelse |
|---|---|
| `0` | Alla steg gick som förväntat, även de förväntade felen |
| `1` | Ett steg gick inte som förväntat |
| `2` | Ogiltiga argument. Problemet och en användningsrad skrivs till stderr |

Jaren byggs med `maven-shade-plugin` och har `StandaloneMain` som `Main-Class`. Ingen Spring-banner eller
Spring-logg skrivs ut, eftersom ingen Spring-kontext startas. Loggningen konfigureras i
`autocore-standalone/src/main/resources/logback.xml` (`logback-spring.xml` läses bara av Spring Boot).
`StandaloneMainProcessTest` startar programmet i en egen JVM och kontrollerar att felkoden är `0`.
 
# Git-flöde: från branch till Pull Request
 
## 1. Uppdatera dev lokalt
Byt till dev och hämta senaste versionen från GitHub innan du börjar något nytt:
```bash
git checkout dev
git pull origin dev
```
 
## 2. Skapa och checka ut en ny branch
Skapa en branch för det du ska jobba på och hoppa direkt till den:
```bash
git checkout -b namn-pa-branch
```
Använd ett beskrivande namn, t.ex. `feature/kundvy` eller `fix/nullpointer-bokning`.
 
## 3. Gör dina ändringar
Redigera koden som vanligt i din editor/IDE. Kör `./mvnw clean verify` lokalt innan du går vidare,
så att alla tre modulerna bygger och alla tester är gröna.
 
## 4. Se vad som ändrats
```bash
git status
git diff
```
 
## 5. Lägg till ändringarna (stage)
```bash
git add .
```
eller för specifika filer:
```bash
git add filnamn
```
 
## 6. Committa med tydligt meddelande
```bash
git commit -m "Add validation to booking form"
```
> Commit-meddelanden, PR-titlar och PR-beskrivningar skrivs på engelska, i imperativ form och utan
> Jira-nyckel som prefix. Undvik nästlade citattecken i meddelandet, det kan krångla i Git Bash på Windows.
 
## 7. Pusha branchen till GitHub
Första gången du pushar en ny branch:
```bash
git push -u origin namn-pa-branch
```
Flaggan `-u` kopplar ihop din lokala branch med samma branch på GitHub, så du bara kan skriva `git push` nästa gång.
 
## 8. Öppna en Pull Request
Gå till repot på GitHub, öppna en PR från din branch mot `dev`. Skriv gärna en kort beskrivning av vad ändringen gör.
 
**Krav innan merge (branch protection):**
- ✅ Minst 1 godkänd review
- ✅ `compile`-checken är grön (`./mvnw clean verify` på Java 21)
- ✅ Branchen är uppdaterad mot `dev`
- ✅ Alla kommentarer/konversationer är lösta
`main` uppdateras bara mellan fredag 15:00 och måndag 09:00 och ska alltid innehålla ett komplett, fungerande projekt.
