# Wigell AutoCore

> Svensk version: [README-SV.md](README-SV.md)

Workshop system for the Wigell group's vehicle service: customers, vehicles, bookings, work orders,
invoices and payments. JavaFX UI built in code (no FXML), Spring Boot 4, Java 21 and SQLite.

# Architecture: modular monolith

The project is a Maven reactor with three modules. All business logic lives in `autocore-core`, and the two
runnable modules build on it.

```
                 ┌────────────────────┐
                 │   autocore-core    │   Domain, services, repository interfaces, error handling, i18n
                 │ (no Boot/JavaFX)   │
                 └─────────▲──────────┘
                           │ depends on
             ┌─────────────┴─────────────┐
   ┌─────────┴──────────┐      ┌─────────┴──────────┐
   │    autocore-app    │      │ autocore-standalone│
   │ Spring Boot, JPA,  │      │ Without Spring, GUI│
   │ SQLite, Flyway,    │      │ and database       │
   │ JavaFX, headless   │      │ (started in 4.0)   │
   └────────────────────┘      └────────────────────┘
```

| Module | Contents | May depend on | May NOT depend on |
|---|---|---|---|
| `autocore-core` | Entities, value objects, services, repository interfaces, exceptions, error chain, translation, the drop-in scenario | Spring Context/Tx, Spring Data JPA (interfaces), Jakarta Persistence/Validation, SLF4J | Spring Boot, JavaFX, `com.wac.autocore.view` |
| `autocore-app` | Spring Boot startup, JavaFX views, headless mode, `application*.properties`, Flyway migrations, language files | `autocore-core`, Spring Boot starters, JavaFX, SQLite | – |
| `autocore-standalone` | Standalone run mode that wires the services together with `new` against in-memory repositories | `autocore-core` | Spring context, JavaFX |

**The rule is guarded by tests:** `NoJavaFxInCoreTest` in core fails the build if any code in core
refers to `javafx.` or `com.wac.autocore.view`.

**Where do I put new code?**
- Business rule, new entity, new service method → `autocore-core`
- New view, dialog, CSS, Spring configuration, migration, language text → `autocore-app`
- New database table or column → a new `V14__...sql` and onwards in `autocore-app`. **V1–V13 are never touched.**
# Structure

All modules use the same base package, `com.wac.autocore`. Every class is therefore in the same package
as before the split, just in a different module.

```
wigell-auto-core/
├── pom.xml                              # Reactor pom: parent spring-boot-starter-parent 4.x, Java 21, lists the modules
├── mvnw  mvnw.cmd  .mvn/                # Maven Wrapper – always use ./mvnw
├── .github/workflows/build-check.yml    # CI: ./mvnw -B clean verify on Java 21 for PRs against main and dev
├── README.md  README-SV.md  STYLEGUIDE.md
│
├── autocore-core/                       # The logic. No Spring Boot, no JavaFX
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wac/autocore/
│       │   ├── model/                   # JPA entities and domain logic
│       │   │   ├── Customer  Vehicle  Mechanic  ServiceItem  ServicePackage
│       │   │   ├── Booking  BookingServiceItem  BookingStatus     # Booking + services with agreed price (snapshot)
│       │   │   ├── WorkOrder                # Abstract base, SINGLE_TABLE inheritance, Template Method in confirm()
│       │   │   ├── PlannedWorkOrder  DropInWorkOrder  WarrantyWorkOrder   # Order types: createFrom(Booking) / draft(...)
│       │   │   ├── WorkOrderType  WorkOrderItem  ServiceLines
│       │   │   ├── WorkOrderStatus          # State: lifecycle and allowed transitions
│       │   │   ├── WorkOrderChange  WorkOrderChangeType           # Change history on the order
│       │   │   ├── Invoice  InvoiceLine  Payment
│       │   │   └── value/                   # Value objects
│       │   │       └── Money  MoneyFormat  TimeSlot
│       │   ├── repository/                  # Repository interfaces, one per aggregate (extend Spring Data Repository)
│       │   ├── service/                     # Business logic, @Service and @Transactional
│       │   │   ├── BookingService  CustomerService  VehicleService  MechanicService
│       │   │   ├── ServiceItemService  ServicePackageService  WorkOrderService
│       │   │   ├── InvoiceService  PaymentService
│       │   │   ├── BookingCopyResult  BookingPriceChange  WorkOrderReopenResult  WorkOrderScheduleResult
│       │   │   └── discount/                # Strategy: discount rules for invoices
│       │   │       └── DiscountStrategy  NoDiscount  VipDiscount  Welcome10Discount  Service200Discount
│       │   ├── exception/                   # Domain errors with a language key (DomainException and subclasses)
│       │   │   └── handler/                 # Chain of Responsibility for displaying errors
│       │   │       └── ErrorHandler  ErrorHandlerChain  ErrorReport
│       │   │           DomainErrorHandler  ValidationErrorHandler  DataAccessErrorHandler  FallbackErrorHandler
│       │   ├── i18n/                        # Translation without JavaFX, shared by GUI and headless
│       │   │   └── Translator  ResourceBundleTranslator  LocalizedError  ErrorReportFormatter
│       │   ├── headless/                    # The drop-in scenario that headless runs
│       │   │   └── DropInScenario  ScenarioContext  ScenarioStep  StepResult
│       │   └── data/                        # JPA converters: date/time as TEXT, Money as BigDecimal
│       │       └── LocalDateConverter  LocalTimeConverter  LocalDateTimeConverter  MoneyConverter
│       └── test/java/com/wac/autocore/
│           ├── unit/                        # Pure unit tests of the model and the value objects
│           ├── service/                     # Service tests with Mockito
│           ├── exception/handler/           # Tests of the error chain
│           ├── architecture/NoJavaFxInCoreTest.java   # Guards the module boundary
│           ├── WorkOrderLifeCycleIntegrationTest.java # The whole lifecycle without Spring and without a database
│           └── testsupport/                 # In-memory repositories and MoneyAssert
│
├── autocore-app/                        # Spring Boot + JavaFX + SQLite. The runnable application
│   ├── pom.xml                          # spring-boot-maven-plugin builds a runnable jar
│   └── src/
│       ├── main/java/com/wac/autocore/
│       │   ├── Main.java                    # Entry point
│       │   ├── AutoCoreConfig.java          # @SpringBootApplication – scans com.wac.autocore in all modules
│       │   ├── headless/HeadlessRunner.java # @Profile("headless"): runs DropInScenario without a GUI
│       │   └── view/                        # JavaFX views, built in code (no FXML)
│       │       ├── AutoCoreApp              # Application: starts/stops Spring, builds the scene
│       │       ├── MainLayout  SideNavigation  NavigationItem   # Shell and side menu
│       │       ├── BaseView                 # Template Method: common frame for all views
│       │       ├── HomeView  CustomerView  VehicleView  BookingView  MechanicView  MechanicBookingsView
│       │       ├── ServiceItemView  ServicePackageView  WorkOrderView  InvoiceView  PaymentView
│       │       ├── component/               # Reusable controls
│       │       │   └── CustomerVehiclePicker  MultiSelectListView  ServiceSelectorBox  ServicePackageSelectorBox
│       │       ├── dialog/                  # Create*/Edit* dialogs, InvoiceDetail, ProcessPayment, QuickDraftWorkOrder etc.
│       │       ├── i18n/LanguageManager     # Language switching sv/en in the GUI, built on core's Translator
│       │       └── util/
│       │           ├── ErrorFacade          # Facade: a single entry point for showing errors via the handler chain
│       │           ├── GlobalExceptionHandler   # Catches unhandled exceptions and passes them to ErrorFacade
│       │           └── AlertHelper  DialogUtil  StringConverterUtil
│       ├── main/resources/
│       │   ├── application.properties           # SQLite (autocore.db), foreign_keys=true, ddl-auto=none, Flyway
│       │   ├── application-headless.properties  # Separate database file autocore-headless.db, quiet log
│       │   ├── db/migration/                    # Flyway owns the schema, V1–V13 (+ .conf where the migration runs without a transaction)
│       │   ├── i18n/messages*.properties        # Texts in Swedish and English
│       │   └── com/wac/autocore/view/           # style.css and assets/ (logo, icon)
│       └── test/
│           ├── java/com/wac/autocore/
│           │   ├── integration/             # Against a real SQLite database built by Flyway: migrations, price history,
│           │   │                            # inheritance, the headless scenario, preservation of 3.5 data
│           │   ├── service/                 # Validation via Spring, the language files, LanguageManager
│           │   └── view/i18n/               # The same error texts in GUI and headless
│           └── resources/
│               ├── application-test.properties   # ddl-auto=validate
│               └── fixtures/autocore-3.5.db      # Copy of a WAC 3.5 database for the data test
│
└── autocore-standalone/                 # Standalone mode without Spring, GUI and database
    └── pom.xml                          # Depends only on autocore-core. The code is built in sprint 4.0
```

# Building and running

Requires **JDK 21**. JavaFX is fetched as a Maven dependency, so no JDK with bundled FX is needed.

```bash
./mvnw clean verify          # Builds all modules and runs all tests (same as CI)
./mvnw clean package -DskipTests
```

Build and test only one module (together with the modules it depends on):
```bash
./mvnw -pl autocore-core verify
./mvnw -pl autocore-app -am verify
```

Start the UI:
```bash
java -jar autocore-app/target/autocore-app-4.0.0.jar
```

> The database file `autocore.db` is created in the folder you start from. Start from the root of the repo so
> the same database is used every time.

# Running without a UI (headless)

The application's services can run without JavaFX. Headless mode runs a complete drop-in flow
(create, add services and mechanic, confirm, start, complete, invoice, pay) followed by
two disallowed operations, and prints the result with the same error keys and texts as the UI.

```bash
./mvnw clean package
java -jar autocore-app/target/autocore-app-4.0.0.jar --spring.profiles.active=headless
```

English:
```bash
java -jar autocore-app/target/autocore-app-4.0.0.jar --spring.profiles.active=headless --headless.lang=en
```

- The scenario (`DropInScenario`) lives in core, and `HeadlessRunner` in app runs it with Spring and SQLite.
- Runs against a separate database file, `autocore-headless.db`. The regular `autocore.db` is not touched.
- Exits with code `0` if all steps went as expected, otherwise with an error code.
- The same scenario runs as an integration test in `HeadlessDropInScenarioIntegrationTest`.
**Windows:** if `å`, `ä` and `ö` are displayed incorrectly in the console, run `chcp 65001` first.

# Standalone mode

> Under construction in sprint 4.0. See the migration plan, section 7.

`autocore-standalone` runs the same domain and service code as the app, but without Spring, GUI and database.
The services are created with `new` and wired to in-memory repositories. The module is packaged as its own
runnable jar.

# Git flow: from branch to Pull Request

## 1. Update dev locally
Switch to dev and fetch the latest version from GitHub before you start anything new:
```bash
git checkout dev
git pull origin dev
```

## 2. Create and check out a new branch
Create a branch for what you are going to work on and switch to it directly:
```bash
git checkout -b branch-name
```
Use a descriptive name, e.g. `feature/customer-view` or `fix/booking-nullpointer`.

## 3. Make your changes
Edit the code as usual in your editor/IDE. Run `./mvnw clean verify` locally before you move on,
so all three modules build and all tests are green.

## 4. See what has changed
```bash
git status
git diff
```

## 5. Add the changes (stage)
```bash
git add .
```
or for specific files:
```bash
git add filename
```

## 6. Commit with a clear message
```bash
git commit -m "Add validation to booking form"
```
> Commit messages, PR titles and PR descriptions are written in English, in the imperative mood and without
> a Jira key as a prefix. Avoid nested quotation marks in the message, as they can cause trouble in Git Bash on Windows.

## 7. Push the branch to GitHub
The first time you push a new branch:
```bash
git push -u origin branch-name
```
The `-u` flag links your local branch to the same branch on GitHub, so you can just type `git push` next time.

## 8. Open a Pull Request
Go to the repo on GitHub and open a PR from your branch against `dev`. Feel free to write a short description of what the change does.

**Requirements before merge (branch protection):**
- ✅ At least 1 approved review
- ✅ The `compile` check is green (`./mvnw clean verify` on Java 21)
- ✅ The branch is up to date with `dev`
- ✅ All comments/conversations are resolved
`main` is only updated between Friday 15:00 and Monday 09:00 and must always contain a complete, working project.
