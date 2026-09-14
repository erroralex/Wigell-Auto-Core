# Styleguide för Wigell Auto Core

## Syfte
Denna styleguide beskriver ett gemensamt visuellt språk för JavaFX-applikationen och ska användas konsekvent i hela gränssnittet.

### Grundprinciper
- Mörkt, rent och tydligt gränssnitt.
- Få färger med tydliga kontraster.
- Samma spacing, typografi och komponentstilar överallt.
- Namnge CSS-klasser konsekvent och beskrivande.

### Temat utgår från:
- mörk basyta
- enkel sidonavigation
- tydlig aktiv markering för vald vy

### Klasser i kodbasen:
- `main-layout`
- `side_navigation`
- `nav-button`
- `nav-button-active`


## Typografi

### Font family
- Standard: `System`, `Segoe UI`, eller annan läsbar sans-serif som finns tillgänglig i JavaFX.
- Undvik dekorativa typsnitt (rent och enkelt).

### Storlekar
- Base text: `13px`
- Small text: `12px`
- Large text: `16px`
- Section heading: `18px`
- Page heading: `22px`
- Display heading: `28px`

### Vikt
- Normal text: `400`
- Emphasized text: `600`
- Headings: `700`

### Rubriknivåer
- `h1`: övergripande sidrubrik
- `h2`: sektionstitel
- `h3`: kortare underrubrik
- `text-muted`: sekundär eller stödjande text

## Färgsystem

### Basfärger
- **Background:** `#212121`
- **Surface:** `#2B2B2B`
- **Surface elevated:** `#333333`
- **Border/divider:** `#3D3D3D`

### Textfärger
- **Primary text:** `#F2F2F2`
- **Secondary text:** `#B5B5B5`
- **Disabled text:** `#7A7A7A`

### Accentfärger
- **Primary accent:** `#4A90E2`
- **Secondary accent:** `#5C6BC0`
- **Hover accent:** `#66A3FF`

### Statusfärger
- **Success:** `#4CAF50`
- **Warning:** `#F2C94C`
- **Error:** `#E57373`

### Användningsprincip
- Bakgrunder ska vara mörka och neutrala.
- Accentfärger ska användas sparsamt för fokus, val och primära handlingar.
- Statusfärger ska endast användas för tydlig semantik.

## Spacing

Sidonavigation använder `padding: 20px` och `spacing: 8px`
Detta utgör grunden för vår återanvändbara layoutspacing;

- `4px`
- `8px`
- `12px`
- `16px`
- `20px`
- `24px`
- `32px`

### Rekommenderad användning
- Tight intern spacing: `4-8px`
- Standard mellan element: `12-16px`
- Mellan sektioner: `20-24px`
- Yttre sidpadding: `20-32px`


## Kontrollers

### Knappar
- Standardhöjd: `32-36px`
- Rundade hörn: `6-8px`
- Primär knapp ska använda accentfärg
- Sekundär knapp ska använda neutral surface-färg

### Input
- Bakgrund: mörk surface
- Tydlig border
- Fokus ska markeras tydligt
- Text ska ha hög kontrast

### Labels
- Vanlig label: primary text
- Hjälptext: secondary text
- Felmeddelande: error-färg

### Tables
- Mörk bakgrund
- Tydlig radseparation eller subtil hover
- Selected row ska markeras med accent eller höjd surface

### Dialogs
- Dialogyta ska använda elevated surface
- Tydlig rubrik överst
- Primär handling tydligt markerad

### Cards
- Card-yta ska använda `surface` eller `surface elevated`
- Padding: `16-24px`
- Border eller subtil shadow för separation

## Interaktion

### Hover
- Förstärk yta eller accent svagt
- Undvik kraftiga färgskiften

### Pressed
- Mörkare eller mer komprimerad känsla

### Focused
- Tydlig fokusram i accentfärg
- Fokus ska alltid vara synligt på tangentbordsstyrda kontroller

### Disabled
- Reducerad opacitet eller dämpad textfärg
- Ingen stark accent

### Selected
- Använd accentfärg eller tydligt markerad surface
- Särskilt viktig för navigation och tabellrader

## Återanvändbara utility-/theme-klasser

### Layoutklasser
- `.main-layout` - huvudskal för appen
- `.side-navigation` - sidopanel för navigation
- `.content-area` - huvudytan för visning av vyer

### Typografiklasser
- `.text-muted`
- `.text-secondary`
- `.text-title`
- `.text-section`
- `.text-caption`

### Ytklasser
- `.surface`
- `.surface-elevated`
- `.surface-muted`
- `.card`

### Knappklasser
- `.btn`
- `.btn-primary`
- `.btn-secondary`
- `.btn-danger`
- `.btn-ghost`

### Formklasser
- `.input`
- `.input-error`
- `.label`
- `.helper-text`

### Statusklasser
- `.state-success`
- `.state-warning`
- `.state-error`
- `.state-info`

## Konsekvent namngivning

### Rekommenderat format
- Använd `kebab-case` i CSS-klasser.
- Håll samma språk och stil i hela filen.

### Rekommenderat mönster
- Layout: `main-layout`, `side-navigation`, `content-area`
- Komponent: `nav-button`, `primary-button`, `form-input`
- State: `is-active`, `is-disabled`, `is-selected`
- Utility: `text-muted`, `surface-elevated`

För konsekvent namngivning, undvik blandade format som `side_navigation` och `nav-button`.
Nya klasser bör följa samma konsekventa stil med kebab-case.

## Gemensam stil för appen
För en gemensam styleguide i hela JavaFX-applikationen delar projektet upp styling i:
1. **Tema/färger** i en central CSS-fil
2. **Layoutklasser** för struktur
3. **Komponentklasser** för knappar, inputs och kort
4. **State-klasser** för aktiv, disabled, selected och hover
