# Wigell AutoCore

Workshop system for the Wigell group's vehicle service: customers, vehicles, bookings, work orders,
invoices and payments. JavaFX UI built in code (no FXML), Spring Boot 4, Java 21 and SQLite.

The application can be run in three ways:

| Run mode | Module | Spring | Database | GUI | Command |
|---|---|---|---|---|---|
| GUI | `autocore-app` | Yes | `autocore.db` | Yes | `java -jar autocore-app/target/autocore-app-4.0.0.jar` |
| Headless | `autocore-app` | Yes | `autocore-headless.db` | No | `... --spring.profiles.active=headless` |
| Standalone | `autocore-standalone` | No | In memory | No | `StandaloneMain --role=... --lang=...` |

All three run the same domain and service code from `autocore-core`, and report errors with the same
language keys and texts.

## Table of contents

- [Architecture: modular monolith](#architecture-modular-monolith)
- [Structure](#structure)
- [Building and running](#building-and-running)
  - [The GUI](#the-gui)
  - [Users and roles](#users-and-roles)
  - [Logging](#logging)
- [Running without a UI (headless)](#running-without-a-ui-headless)
- [Standalone mode](#standalone-mode)
  - [Arguments](#arguments)
  - [What it runs](#what-it-runs)
  - [Running it](#running-it)
  - [Known limits without Spring](#known-limits-without-spring)
- [Git flow: from branch to Pull Request](#git-flow-from-branch-to-pull-request)

# Architecture: modular monolith

The project is a Maven reactor with three modules. All business logic lives in `autocore-core`, and the two
runnable modules build on it.

```
                 ┌────────────────────┐
                 │   autocore-core    │   Domain, services, repository interfaces, access rules,
                 │ (no Boot/JavaFX)   │   error handling, i18n and language files, scenarios
                 └─────────▲──────────┘
                           │ depends on
             ┌─────────────┴─────────────┐
   ┌─────────┴──────────┐      ┌─────────┴──────────┐
   │    autocore-app    │      │ autocore-standalone│
   │ Spring Boot, JPA,  │      │ Without Spring, GUI│
   │ SQLite, Flyway,    │      │ and database.      │
   │ JavaFX, headless   │      │ In-memory repos    │
   └────────────────────┘      └────────────────────┘
```

| Module | Contents | May depend on | May NOT depend on |
|---|---|---|---|
| `autocore-core` | Entities, value objects, services, repository interfaces, roles and `AccessPolicy`, exceptions, error chain, translation and language files, the scenarios and the report printer | Spring Context/Tx, Spring Data JPA (interfaces), Spring Security Core, Jakarta Persistence/Validation, SLF4J | Spring Boot, JavaFX, `com.wac.autocore.view` |
| `autocore-app` | Spring Boot startup, JavaFX views, headless mode, Spring Security wiring, correlation id aspect, `application*.properties`, `logback-spring.xml`, Flyway migrations | `autocore-core`, Spring Boot starters, JavaFX, SQLite | – |
| `autocore-standalone` | Standalone program that wires the services together with `new` against in-memory repositories, with an access check in front of the work order operations | `autocore-core` | Spring (in `com.wac.autocore.standalone`), JavaFX, `com.wac.autocore.view` |

**The rules are guarded by tests:**
- `NoJavaFxInCoreTest` and `NoJavaFxInCoreArchUnitTest` in core fail the build if any code in core refers to
  `javafx.` or `com.wac.autocore.view`.
- `StandaloneArchitectureTest` in standalone fails the build if anything depends on JavaFX or the views, or if
  any class in `com.wac.autocore.standalone` (main **and** test) depends on `org.springframework`.
  A test that needs Spring types belongs in core or app.

**Where do I put new code?**
- Business rule, new entity, new service method, new permission → `autocore-core`
- Language text → `autocore-core/src/main/resources/i18n/` (all three files)
- New view, dialog, CSS, Spring configuration, migration → `autocore-app`
- New database table or column → a new `V15__...sql` and onwards in `autocore-app`. **V1–V14 are never touched.**
- Something the standalone program needs, such as an in-memory repository → `autocore-standalone`

# Structure

All modules use the same base package, `com.wac.autocore`. Every class is therefore in the same package
as before the split, just in a different module.

```
wigell-auto-core/
├── pom.xml                              # Reactor pom: parent spring-boot-starter-parent 4.x, Java 21, lists the modules
├── mvnw  mvnw.cmd  .mvn/                # Maven Wrapper – always use ./mvnw
├── .github/workflows/build-check.yml    # CI: ./mvnw -B clean verify on Java 21 for PRs against main and dev
├── README.md  STYLEGUIDE.md
│
├── autocore-core/                       # The logic. No Spring Boot, no JavaFX
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wac/autocore/
│       │   ├── model/                   # JPA entities and domain logic
│       │   │   ├── Customer  Vehicle  Mechanic  ServiceItem  ServicePackage  AppUser
│       │   │   ├── Booking  BookingServiceItem  BookingStatus     # Booking + services with agreed price (snapshot)
│       │   │   ├── WorkOrder                # Abstract base, SINGLE_TABLE inheritance, Template Method in confirm()
│       │   │   ├── PlannedWorkOrder  DropInWorkOrder  WarrantyWorkOrder   # Order types: createFrom(Booking) / draft(...)
│       │   │   ├── WorkOrderType  WorkOrderItem  ServiceLines
│       │   │   ├── WorkOrderStatus          # State: lifecycle and allowed transitions
│       │   │   ├── WorkOrderChange  WorkOrderChangeType           # Change history on the order
│       │   │   ├── WorkOrderEvent  WorkOrderEventPublisher        # Observer: lifecycle events, no Spring
│       │   │   ├── Invoice  InvoiceLine  Payment
│       │   │   └── value/                   # Value objects
│       │   │       └── Money  MoneyFormat  TimeSlot
│       │   ├── repository/                  # Repository interfaces, one per aggregate (extend Spring Data Repository)
│       │   ├── service/                     # Business logic, @Service, @Transactional and @PreAuthorize
│       │   │   ├── BookingService  CustomerService  VehicleService  MechanicService
│       │   │   ├── ServiceItemService  ServicePackageService  WorkOrderService  AppUserDetailsService
│       │   │   ├── WorkOrderLifecycle       # Proxy subject: the six work order operations a scenario needs
│       │   │   ├── InvoiceService  PaymentService
│       │   │   ├── BookingCopyResult  BookingPriceChange  WorkOrderReopenResult  WorkOrderScheduleResult
│       │   │   └── discount/                # Strategy: discount rules for invoices
│       │   │       └── DiscountStrategy  NoDiscount  VipDiscount  Welcome10Discount  Service200Discount
│       │   ├── security/                    # Role, Permission and AccessPolicy: the single source of who may do what
│       │   ├── config/SecurityConfig        # Password encoder and authentication manager
│       │   ├── exception/                   # Domain errors with a language key (DomainException and subclasses,
│       │   │   │                            # including PermissionDeniedException)
│       │   │   └── handler/                 # Chain of Responsibility for displaying errors
│       │   │       └── ErrorHandler  ErrorHandlerChain  ErrorReport
│       │   │           SecurityErrorHandler  ValidationErrorHandler  DomainErrorHandler
│       │   │           DataAccessErrorHandler  FallbackErrorHandler
│       │   ├── i18n/                        # Translation without JavaFX, shared by GUI, headless and standalone
│       │   │   └── Translator  ResourceBundleTranslator  LocalizedError  ErrorReportFormatter
│       │   ├── headless/                    # Command: scenarios that run the services without a GUI
│       │   │   ├── ScenarioStep             # Command
│       │   │   ├── ScenarioRunner           # Invoker: runs the steps and translates errors with the error chain
│       │   │   ├── DropInScenario           # Client: the full drop-in flow
│       │   │   ├── ScenarioContext  StepResult
│       │   │   └── ScenarioReportPrinter    # The report that headless and standalone both print
│       │   ├── logging/                     # CorrelationIds  LogKeys  SafeLog
│       │   └── data/                        # JPA converters: date/time as TEXT, Money as BigDecimal
│       │       └── LocalDateConverter  LocalTimeConverter  LocalDateTimeConverter  MoneyConverter
│       ├── main/resources/i18n/             # messages.properties, messages_sv.properties, messages_en.properties
│       └── test/java/com/wac/autocore/
│           ├── unit/                        # Pure unit tests of the model and the value objects
│           ├── service/                     # Service tests with Mockito
│           ├── exception/handler/           # Tests of the error chain
│           ├── security/                    # AccessPolicy matrix
│           ├── logging/                     # CorrelationIds and SafeLog
│           ├── architecture/                # Guards the module boundary
│           └── testsupport/MoneyAssert
│
├── autocore-app/                        # Spring Boot + JavaFX + SQLite. The runnable application
│   ├── pom.xml                          # spring-boot-maven-plugin builds a runnable jar
│   └── src/
│       ├── main/java/com/wac/autocore/
│       │   ├── Main.java                    # Entry point: picks GUI or headless before Spring starts
│       │   ├── AutoCoreConfig.java          # @SpringBootApplication – scans com.wac.autocore in all modules
│       │   ├── headless/HeadlessRunner.java # @Profile("headless"): runs DropInScenario as ADMIN without a GUI
│       │   ├── security/AccessEvaluator     # Bean behind @PreAuthorize("@access.can('...')"), asks AccessPolicy
│       │   ├── logging/CorrelationAspect    # Proxy (AOP): correlation id, operation and user in MDC per service call
│       │   └── view/                        # JavaFX views, built in code (no FXML)
│       │       ├── AutoCoreApp              # Application: starts/stops Spring, signs in, builds the scene
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
│       │   ├── application-headless.properties  # Separate database file autocore-headless.db
│       │   ├── logback-spring.xml               # One logging profile per run mode, see Logging
│       │   ├── db/migration/                    # Flyway owns the schema, V1–V14 (+ .conf where the migration runs without a transaction)
│       │   └── com/wac/autocore/view/           # style.css and assets/ (logo, icon)
│       └── test/
│           ├── java/com/wac/autocore/
│           │   ├── integration/             # Against a real SQLite database built by Flyway: migrations, table rebuilds,
│           │   │                            # price history, inheritance, the headless scenario, preservation of 3.5 data
│           │   ├── security/                # @PreAuthorize per role on the services
│           │   ├── service/                 # Validation via Spring, the language files, LanguageManager
│           │   └── view/i18n/               # The same error texts in GUI and headless
│           └── resources/
│               ├── application-test.properties   # ddl-auto=validate
│               └── fixtures/autocore-3.5.db      # Copy of a WAC 3.5 database for the data test
│
└── autocore-standalone/                 # Standalone mode without Spring, GUI and database
    ├── pom.xml                          # Depends only on autocore-core. maven-shade-plugin builds autocore-standalone.jar
    └── src/
        ├── main/resources/logback.xml       # Logging for the standalone jar (WARN to the console)
        ├── main/java/com/wac/autocore/standalone/
        │   ├── StandaloneMain               # Entry point: reads --role and --lang, prints the report, sets the exit code
        │   ├── StandaloneAutoCore           # Composition root: repositories, seed data, services and proxy with new
        │   ├── StandaloneSeedData           # VIP customer, vehicle, mechanic and two service items
        │   ├── ReadOnlyScenario             # Command client for roles without write permission
        │   ├── StandaloneInvoicePrinter     # Invoice lines, discounts and totals with the GUI's language keys
        │   ├── security/WorkOrderLifecycleAccessProxy   # Protection proxy: asks AccessPolicy before each call
        │   └── repository/                  # In-memory repositories, one per repository interface
        └── test/java/
            ├── architecture/StandaloneArchitectureTest  # No Spring, JavaFX or views
            └── com/wac/autocore/
                ├── integration/WorkOrderLifeCycleIntegrationTest  # The whole lifecycle without Spring and database
                └── standalone/              # Every role runs as expected, the proxy, shared language texts,
                                             # StandaloneMainProcessTest (own JVM, exit code 0, no Spring output)
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
./mvnw -pl autocore-standalone -am verify
```

## The GUI

```bash
java -jar autocore-app/target/autocore-app-4.0.0.jar
```

> The database file `autocore.db` is created in the folder you start from. Start from the root of the repo so
> the same database is used every time.

### Users and roles

Users are stored in the `app_user` table (migration V14). Three users are seeded, one per role:

| User | Role | May |
|---|---|---|
| `admin` | `ADMIN` | Everything |
| `workshop` | `WORKSHOP` | Read, and write customers, vehicles, bookings and work orders |
| `readonly` | `READ_ONLY` | Read only |

The rules live in `AccessPolicy` in core. The services are protected with `@PreAuthorize`, so a denied operation
gives the same error whether it comes from a button or another entry point.

The login dialog is not built yet. Until then the GUI signs in automatically as `admin`. To try another role,
pass the user with `--autocore.dev-user`:

```bash
java -jar autocore-app/target/autocore-app-4.0.0.jar --autocore.dev-user=readonly
```

## Logging

Logging is configured in `autocore-app/src/main/resources/logback-spring.xml`, with one profile per run mode:

| Profile | Output | Level |
|---|---|---|
| default (dev) | Console, colored | `com.wac` DEBUG, everything else INFO |
| `test` | Console | WARN |
| `headless` | File, `logs/autocore-headless.log`, uncolored | INFO |

Every line contains time, level, logger and the MDC fields `corrId`, `op`, `user` and `wo`. The correlation id
is set by `CorrelationAspect` for each service call, and the same id is shown as a reference in the error dialog.

The standalone program has no logging backend yet, so its log output is discarded and SLF4J prints a warning
at startup. A backend is added in a separate card.

**Colors in IntelliJ:** Spring Boot only turns on colors when output goes to a terminal, and with Java 21
IntelliJ's console does not count as one. Add the following to the run configuration's VM options:

    -Dspring.output.ansi.enabled=always

The same applies to Git Bash. There, pass `--spring.output.ansi.enabled=always` after the jar file.
Do not set it in `application.properties`, since ANSI codes would then end up in test and CI logs.

# Running without a UI (headless)

Headless mode runs the app's services with Spring and SQLite but without JavaFX. It runs a complete drop-in flow
(create, add services and mechanic, confirm, start, complete, invoice, pay) followed by two disallowed
operations, and prints the result with the same error keys and texts as the UI.

```bash
./mvnw clean package -DskipTests
java -jar autocore-app/target/autocore-app-4.0.0.jar --spring.profiles.active=headless
```

| Argument | Values | Default |
|---|---|---|
| `--spring.profiles.active` | `headless` (required for this mode) | – |
| `--headless.lang` | `sv`, `en` | `sv` |

English:
```bash
java -jar autocore-app/target/autocore-app-4.0.0.jar --spring.profiles.active=headless --headless.lang=en
```

- `Main` picks the mode before Spring starts, so JavaFX is never loaded.
- The scenario (`DropInScenario`) lives in core, and `HeadlessRunner` in app runs it as a system `ADMIN`.
- Runs against a separate database file, `autocore-headless.db`. The regular `autocore.db` is not touched.
- The report is printed to the console, and the log goes to `logs/autocore-headless.log`.
- Exits with code `0` if all steps went as expected, otherwise with an error code.
- The same scenario runs as an integration test in `HeadlessDropInScenarioIntegrationTest`.

**Windows:** if `å`, `ä` and `ö` are displayed incorrectly in the console, run `chcp 65001` first.

# Standalone mode

`autocore-standalone` runs the same domain and service code as the app, but **without Spring, GUI and database**.
`StandaloneAutoCore` creates the services with `new` and wires them to in-memory repositories filled with seed data.
It shows the client that the business rules do not depend on the framework.

## Arguments

| Argument | Values | Default |
|---|---|---|
| `--role` | `ADMIN`, `WORKSHOP`, `READ_ONLY` (case-insensitive) | `ADMIN` |
| `--lang` | `sv`, `en` | `sv` |

| Exit code | Meaning |
|---|---|
| `0` | Every step went as expected, including the expected errors |
| `1` | A step did not go as expected |
| `2` | Invalid arguments. The problem and a usage line are printed to stderr |

## What it runs

The role decides the scenario, using the same `AccessPolicy` as the access check:

- **Roles with `WORK_ORDER_WRITE`** (`ADMIN`, `WORKSHOP`) run the full drop-in flow, 11 steps, including
  starting a cancelled order and creating an order for an unknown vehicle, which are expected to fail.
  The report ends with the invoice: WELCOME10 and the VIP discount, 10 % each per line.
- **`READ_ONLY`** fetches the test data and then tries to create a work order, which is denied:
  `[error.accessDenied] Error: You do not have permission to perform this action.`

The work order operations go through `WorkOrderLifecycleAccessProxy`, a protection proxy that asks `AccessPolicy`
before each call. It gives the standalone program the same rules that `@PreAuthorize` gives the app.

## Running it

**In IntelliJ (simplest):** create an *Application* run configuration with module `autocore-standalone`,
main class `com.wac.autocore.standalone.StandaloneMain` and the arguments under *Program arguments*,
for example `--role=READ_ONLY --lang=en`.

**From the command line:** build the runnable jar and start it with `java -jar`, like any separate application:

```bash
./mvnw clean package -DskipTests
java -jar autocore-standalone/target/autocore-standalone.jar --role=READ_ONLY --lang=en
echo $?
```

The jar is built with `maven-shade-plugin` and has `StandaloneMain` as its `Main-Class`. No Spring banner or Spring
log lines are printed, because no Spring context is started. Logging is configured in
`autocore-standalone/src/main/resources/logback.xml` (`logback-spring.xml` is only read by Spring Boot).
`StandaloneMainProcessTest` starts the program in its own JVM and checks that the exit code is `0`.

## Known limits without Spring

- Bean Validation and `@Transactional` have no effect. The domain rules apply fully.
- Only the work order operations are access checked. `WORKSHOP` lacks `INVOICE_CREATE` in `AccessPolicy`,
  but can still create the invoice in standalone, since `InvoiceService` has no proxy.
- Data lives only in memory and is gone when the program exits.

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
