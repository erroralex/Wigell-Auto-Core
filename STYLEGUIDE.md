# Styleguide för Wigell Auto Core

## Syfte
Gemensamt visuellt språk för JavaFX-applikationen: mörk sidomeny, ljus innehållsyta,
raka hörn och intryckta kontroller. Färger från Wigell Autos logga.

## Filer och ansvar

| Fil | Ansvar |
|---|---|
| `theme.css` | **Enda** filen med färgvärden. Alla färger som looked-up colors i `.root`, plus kopplingen till Modenas variabler. |
| `style.css` | Typografi, layout och alla komponenter. Refererar bara till färgnamn. |
| `login.css` | Det som är unikt för inloggningen. |
| `Stylesheets.java` | Enda stället som laddar stilmallarna (scen, inloggning, dialoger). Ny fil läggs till här. |

## Regler
- Inga hex- eller rgba-värden utanför `theme.css`.
- Inga `setStyle` i vyer. Enda undantaget är Ctrl+scroll-zoomen i `AutoCoreApp` och `DialogUtil`.
- Inga avstånd i Java (`setSpacing`, `setPadding`, `new HBox(15, …)`). Använd en klass.
- Alla storlekar i `em`, så att de följer zoomen. Basen är 14px (`.root` och `AutoCoreApp.BASE_FONT_SIZE`).
- Klassnamn i kebab-case.
- Lägen (hover, fokus, fel, inaktiverad) byter bara färgnamn, aldrig hur kontrollen ritas.

## Färger

| Namn | Värde | Används till |
|---|---|---|
| `-color-bg` | `#E6E3DB` | Fönster- och innehållsbakgrund |
| `-color-surface` | `#F3F1EB` | Tabeller, listor, dialoger, kort |
| `-color-surface-alt` | `#E2DED4` | Tabellhuvud, dialogens knappfält |
| `-color-field` | `#E9E6DE` | Textfält, comboboxar, datumväljare |
| `-color-sunken` | `#DCD8CD` | Neutrala knappar |
| `-color-border` / `-color-divider` | `#C9C3B5` / `#D3CEC2` | Ramar på kontroller / skiljelinjer |
| `-color-text` / `-color-text-secondary` / `-color-muted` | `#23251F` / `#4A4D44` / `#5E6157` | Text i tre nivåer |
| `-color-primary` | `#4F5D4B` | Primärknapp, markering, fokus |
| `-color-error` / `-color-warning` / `-color-success` | `#9A3B2E` / `#8A5A12` / `#3F6B47` | Status |
| `-color-sidebar` | `#3F4B3C` | Sidomeny, dialogrubrik, inloggningens vänstra panel |
| `-color-brand-light` | `#82916E` | Logans gröna. Endast dekoration. |

Se `theme.css` för alla färger, inklusive hover-, skugg- och glödfärger.

## Typografi
Basstorlek 19px. Inloggningen 19px.

| Klass | Storlek | Vikt | Används till |
|---|---|---|---|
| `.text-title` | 2.15em | 700 | Sidrubrik i varje vy |
| `.text-header` | 1.38em | 600 | Sektionsrubrik i en vy, dialogrubrik |
| `.text-body` | 1em | 400 | Brödtext |
| `.text-small` | 0.92em | – | Summeringar, hjälptext |
| `.text-secondary` / `.text-muted` | – | – | Textfärg, kombineras med en storlek |
| `.text-error` / `.text-warning` | – | 700 / 600 | Fel och varningar |

## Avstånd
xs 0.31em · sm 0.62em · md 0.92em · lg 1.23em · xl 1.54em · xxl 1.85em

| Klass | Används till |
|---|---|
| `.content-area` | Varje vy (`BaseView`): padding och avstånd mellan delar |
| `.toolbar` | Knappraden under en vys rubrik |
| `.view-header` | Rubrik med knapp till höger |
| `.section` / `.section-stack` | Sektion med rubrik och tabell / kolumn av sektioner |
| `.inline-row` | Kontroll med en knapp bredvid |
| `.form-content` / `.form-grid` | Innehåll och rutnät i formulärdialoger (sätts av `FormDialog`) |

## Komponenter

### Knappar
| Klass | Används till |
|---|---|
| (ingen) / `.btn-secondary` | Neutral handling |
| `.btn-primary` | Vyns huvudhandling, högst en eller två per vy |
| `.btn-danger` | Ta bort, avboka, avbryt order |

Standardknappen i en dialog (OK, Spara) blir primär automatiskt.

### Fält, listor och tabeller
Fält har en intryckt yta, grön ram och glöd vid fokus, och röd ram vid fel (`.input-error`).
Tabeller har platta rader med skiljelinje, svag hover och grön markerad rad.
Ingen justering i CSS på tabellceller: beloppskolumner högerjusteras i Java.

### Dialoger
Mörkgrön rubrik (när rubriktext finns), ljust innehåll och beige knappfält.
Felruta överst i formulär: `.form-error`.

### Sidomeny
Mörk bakgrund. Aktiv rad är intryckt (`-color-sidebar-sunken`). Fokusring endast vid tangentbordsnavigering (`:focus-visible`).

## Kontrast
All text klarar minst 4.5:1 och fokusmarkeringar minst 3:1 mot sin bakgrund.
Inaktiverade kontroller är undantagna. Kontrollera nya färgkombinationer innan de används.