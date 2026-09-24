# Struktur:

```
src/main/
├── java/com/wac/autocore/
│   ├── Main.java                       # Startpunkt, anropar AutoCoreApp
│   ├── AutoCoreConfig.java             # @SpringBootApplication – Spring hittar allt i underpaketen
│   ├── model/                          # JPA-entiteter (@Entity)
│   │   ├── Customer.java  Vehicle.java  Booking.java  ServiceItem.java
│   │   └── Mechanic.java  WorkOrder.java  Invoice.java  Payment.java
│   ├── repository/                     # Spring Data-repositories (kommer)
│   ├── service/
│   │   ├── GarageSystem.java           # @Service – affärslogik
│   │   └── LanguageManager.java        # Språkbyte sv/en
│   ├── data/
│   │   ├── LocalDateConverter.java     # LocalDate <-> TEXT
│   │   ├── LocalTimeConverter.java     # LocalTime <-> TEXT
│   │   ├── Database.java               # Tillfällig, tas bort
│   │   └── ConnectionManager.java      # Tillfällig, tas bort
│   └── view/                           # Alla JavaFX-vyer
│       ├── AutoCoreApp.java            # Application: startar/stänger Spring, bygger scenen
│       ├── MainLayout.java             # BorderPane-skal: sidomeny + innehållsyta
│       ├── SideNavigation.java         # Sidomenyn
│       ├── NavigationItem.java         # enum: sektionerna + ikon/etikett
│       ├── HomeView.java
│       ├── CustomerView.java  VehicleView.java  BookingView.java  ServiceItemView.java
│       ├── MechanicView.java  MechanicBookingsView.java
│       ├── WorkOrderView.java  InvoiceView.java  PaymentView.java
│       ├── dialog/
│       │   ├── CreateCustomerDialog.java  CreateVehicleDialog.java  CreateBookingDialog.java
│       │   ├── CreateWorkOrderDialog.java  CreateInvoiceDialog.java
│       │   └── ProcessPaymentDialog.java
│       └── util/
│           ├── AlertHelper.java        # Gemensam fel-/bekräftelsedialog
│           └── DialogUtil.java
└── resources/
    ├── application.properties          # Spring: SQLite, Hibernate-dialekt, schema.sql vid start
    ├── schema.sql                      # Tabellerna
    ├── i18n/messages*.properties       # Texter på svenska och engelska
    └── com/wac/autocore/view/          # style.css och bilder
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
