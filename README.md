# Struktur

```
src/main/
├── java/com/wac/autocore/
│   ├── Main.java                       # Startpunkt, anropar AutoCoreApp
│   ├── AutoCoreConfig.java             # @SpringBootApplication – Spring hittar allt i underpaketen
│   ├── model/                          # JPA-entiteter och domänlogik
│   │   ├── Customer.java  Vehicle.java  Mechanic.java  ServiceItem.java
│   │   ├── Booking.java  BookingServiceItem.java        # Bokning + tjänster med avtalat pris (snapshot)
│   │   ├── WorkOrder.java              # Abstrakt bas, SINGLE_TABLE-arv, Template Method i confirm()
│   │   ├── PlannedWorkOrder.java       # Ordertyp PLANNED, fabriksmetod createFrom(Booking)
│   │   ├── WorkOrderItem.java          # Jobb på ordern (snapshot från bokningen)
│   │   ├── WorkOrderStatus.java        # State: livscykel och tillåtna övergångar
│   │   └── Invoice.java  InvoiceLine.java  Payment.java
│   ├── repository/                     # Spring Data JPA-repositories, en per aggregat
│   ├── service/                        # @Service – affärslogik, @Transactional
│   │   ├── BookingService.java  CustomerService.java  VehicleService.java  MechanicService.java
│   │   ├── ServiceItemService.java  WorkOrderService.java  InvoiceService.java  PaymentService.java
│   │   ├── LanguageManager.java        # Språkbyte sv/en via ResourceBundle
│   │   └── discount/                   # Strategy: rabattregler för fakturor
│   │       ├── DiscountStrategy.java
│   │       └── NoDiscount.java  VipDiscount.java  Welcome10Discount.java  Service200Discount.java
│   ├── exception/                      # Domänfel med språknyckel (DomainException och subklasser)
│   │   └── handler/                    # Chain of Responsibility för felvisning
│   │       ├── ErrorHandler.java  ErrorHandlerChain.java  ErrorReport.java
│   │       └── DomainErrorHandler.java  DataAccessErrorHandler.java  FallbackErrorHandler.java
│   ├── data/                           # JPA-konverterare för datum/tid som TEXT i SQLite
│   │   └── LocalDateConverter.java  LocalTimeConverter.java  LocalDateTimeConverter.java
│   └── view/                           # JavaFX-vyer, byggda i kod (ingen FXML)
│       ├── AutoCoreApp.java            # Application: startar/stänger Spring, bygger scenen
│       ├── MainLayout.java             # BorderPane-skal: sidomeny + innehållsyta
│       ├── SideNavigation.java  NavigationItem.java     # Sidomenyn och dess sektioner
│       ├── BaseView.java               # Template Method: gemensam ram för alla vyer
│       ├── HomeView.java  CustomerView.java  VehicleView.java  BookingView.java
│       ├── ServiceItemView.java  MechanicView.java  MechanicBookingsView.java
│       ├── WorkOrderView.java  InvoiceView.java  PaymentView.java
│       ├── component/                  # Återanvändbara kontroller
│       │   └── MultiSelectListView.java  ServiceSelectorBox.java
│       ├── dialog/
│       │   ├── CreateCustomerDialog.java  CreateVehicleDialog.java  CreateBookingDialog.java
│       │   ├── CreateWorkOrderDialog.java  CreateInvoiceDialog.java  ProcessPaymentDialog.java
│       │   └── EditBookingServicesDialog.java  InvoiceDetailDialog.java  AdminServiceItemDialog.java
│       └── util/
│           ├── ErrorFacade.java        # Facade: en ingång för att visa fel via handler-kedjan
│           ├── GlobalExceptionHandler.java  # Fångar ohanterade undantag och skickar till ErrorFacade
│           └── AlertHelper.java  DialogUtil.java
└── resources/
    ├── application.properties          # Spring: SQLite, foreign_keys=true, ddl-auto=none, Flyway
    ├── db/migration/                   # Flyway äger schemat
    │   ├── V1__initial_schema.sql  V2__seed_data.sql
    │   ├── V3–V6                       # Pris-snapshots och fakturarader
    │   └── V7__work_order_types.sql (+ .conf)   # Ordertyper; körs utanför Flyways transaktion
    ├── i18n/messages*.properties       # Texter på svenska och engelska
    └── com/wac/autocore/view/          # style.css och assets/ (logga, ikon)

src/test/
├── java/com/wac/autocore/
│   ├── service/                        # Enhetstester med Mockito + LanguageBundleTest (språkfilerna)
│   └── integration/                    # Mot riktig SQLite-fil som Flyway bygger
│       └── HistoricalPricingIntegrationTest.java  WorkOrderInheritanceIntegrationTest.java
└── resources/application-test.properties   # ddl-auto=validate
```


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
Redigera koden som vanligt i din editor/IDE. Testa lokalt att allt kompilerar och fungerar innan du går vidare.

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
git commit -m "Kort beskrivning av ändringen"
```
> Undvik nästlade citattecken i meddelandet — det kan krångla i Git Bash på Windows. Skriv gärna på imperativ form, t.ex. "Lägg till valideringslogik för bokningsformulär".

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
- ✅ `compile`-checken är grön
- ✅ Branchen är uppdaterad mot `dev`
- ✅ Alla kommentarer/konversationer är lösta
