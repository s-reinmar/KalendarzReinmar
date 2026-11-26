# KalendarzReinmar

**KalendarzReinmar** to natywna aplikacja na system Android napisana w języku **Kotlin** z interfejsem **Jetpack Compose (Material 3)**. Łączy rozbudowany kalendarz wpisów z mapą OpenStreetMap, edytorem notatek z formatowaniem tekstu (Rich Text), systemem przypomnień, importem/eksportem plików (`.ics`, `.csv`) oraz interaktywnym kołem barw HSV do personalizacji motywu aplikacji na żywo.

---

## Funkcjonalności

### 1. Kalendarz i 5 typów wpisów
- **Widok miesięczny** z nawigacją po miesiącach, podglądem dni ze zdarzeniami oraz filtrowaniem według wybranego dnia lub wszystkich wpisów.
- **5 wyspecjalizowanych typów wpisów** (tworzonych z menu szybkiego dodawania):
  - **Wydarzenie (`EVENT`)** — zakres dat i godzin (lub cały dzień), lista gości/uczestników, link do spotkania online, opis.
  - **Zadanie (`TASK`)** — priorytet (Niski / Średni / Wysoki), status ukończenia (przełączany bezpośrednio na liście), lista kontrolna podzadań.
  - **Urodziny (`BIRTHDAY`)** — solenizant, opcjonalny rok urodzenia (z automatycznym wyliczaniem wieku), pomysł na prezent, domyślne powtarzanie roczne.
  - **Wizyta (`APPOINTMENT`)** — specjalista/placówka, telefon kontaktowy, notatki przygotowawcze / wymagane dokumenty.
  - **Podróż (`TRIP`)** — zakres dat, środek transportu (Samochód, Pociąg, Samolot, Autobus, Inny), numer rezerwacji/biletu, lista do spakowania.
- **Powtarzanie (cykliczność):** Brak, Codziennie, Co tydzień, Co miesiąc, Co rok oraz **niestandardowe** (co *N* godzin lub *N* dni, zakres 1–999).
- **Kolory etykiet:** 5 predefiniowanych barw (Sosna, Kobalt, Bursztyn, Terakota, Śliwka) oraz **własny selektor koloru RGB / HEX**.
- **Przypomnienia systemowe:** planowanie alarmów (`AlarmManager`), obsługa powiadomień (`POST_NOTIFICATIONS`), otwieranie szczegółów wydarzenia po kliknięciu powiadomienia oraz automatyczne przywracanie alarmów po restarcie urządzenia (`BootReminderReceiver`).

### 2. Interaktywna Mapa (OpenStreetMap + Leaflet)
- **Mapa wydarzeń** osadzona na bazie Leaflet (`leaflet_map.html`) i kafelków OpenStreetMap.
- **Wyszukiwanie adresów i miejsc** (geokodowanie oraz geokodowanie odwrotne przez Nominatim API).
- **Wybór lokalizacji** poprzez dotknięcie mapy, wyszukanie adresu lub wyśrodkowanie na bieżącej pozycji GPS urządzenia.
- **Pinezki wydarzeń** z podglądem szczegółów, dopasowaniem widoku do wszystkich pinezek oraz możliwością dodania nowego wydarzenia bezpośrednio z wybranego punktu na mapie.

### 3. Notatki Rich Text i powiązania z kalendarzem
- **Formatowanie tekstu:** pogrubienie, kursywa, podkreślenie, przekreślenie, indeks górny (`SUP`), indeks dolny (`SUB`), czyszczenie stylu.
- **Rozmiary tekstu:** Nagłówki `H1`, `H2`, `H3`, tekst zwykły (`BODY`) oraz mały (`SMALL`).
- **Listy:** punktowa, liczbowa, lista zadań (checklist), literowa oraz z myślnikami.
- **Załączniki z kalendarza:** możliwość przypinania jednego lub wielu wpisów kalendarza do notatki, wyszukiwanie ich w oknie wyboru oraz szybkie otwieranie podglądu podpiętego wydarzenia.

### 4. Import i eksport danych
- Dostępny z górnego menu aplikacji z wykorzystaniem systemowego Storage Access Framework (SAF):
  - **iCalendar (`.ics`)** — import i eksport wydarzeń kalendarza.
  - **CSV (`.csv`)** — import i eksport wydarzeń oraz notatek.

### 5. Personalizacja wyglądu (Color Wheel HSV)
- **Interaktywne koło barw HSV** z suwakiem jasności (Value) dla każdej próbki.
- **9 reguł harmonii kolorów:** Własna (`CUSTOM`), Analogiczna, Dopełniająca, Rozdzielona dopełniająca, Triada, Kwadrat, Złożona, Odcienie (`SHADES`), Monochromatyczna.
- **Konfigurowalna liczba kolorów harmonii (2–5)** oraz **3 tryby powierzchni i tła:** Barwny (z koła), Jasny, Ciemny barwny.
- **Podgląd i aktualizacja motywu Material 3 na żywo** (wraz z dopasowaniem stylu pasków systemowych edge-to-edge) oraz **zapisywanie, przywracanie i usuwanie własnych palet** w Jetpack DataStore.

### 6. Interfejs adaptacyjny
- Obsługa układu **edge-to-edge** (`WindowInsets`).
- Automatyczne przełączanie nawigacji pomiędzy **NavigationBar** (ekrany kompaktowe) i **NavigationRail** (szersze ekrany / tablety).
- Czcionki **Plus Jakarta Sans** oraz **Inter** dołączone lokalnie w zasobach aplikacji (`res/font/`).

---

## Architektura i stos technologiczny

Projekt jest zorganizowany zgodnie z zasadami **SOLID**, **MVC (Model–View–Controller)** oraz **Clean Architecture / Ports & Adapters**, z ręcznym wstrzykiwaniem zależności (`AppContainer`).

### Struktura pakietów (`app/src/main/java/com/example/`)

```text
com.example
├── MainActivity.kt               # Punkt wejścia, edge-to-edge, obsługa deep-linków z powiadomień
├── di/
│   └── AppContainer.kt           # Kontener DI i fabryki ViewModeli
├── domain/
│   ├── model/                    # Niemutowalne modele domenowe (data class, sealed interface, value class)
│   ├── port/                     # Interfejsy repozytoriów i portów (EventRepository, NoteRepository, ReminderPort, ...)
│   └── service/                  # Czysta logika domenowa (CalendarGridBuilder, ColorHarmonyEngine, NoteRichTextEditorEngine, ...)
├── data/
│   ├── AppDatabase.kt            # Baza Room (v5) z migracjami 1->2->3->4->5
│   ├── local/                    # Repozytoria Room i mappery encji
│   ├── preferences/              # Repozytorium DataStore (zapisane palety i motyw)
│   ├── geocoding/                # Integracja z OpenStreetMap Nominatim
│   ├── location/                 # Dostawca lokalizacji GPS/Network Androida
│   ├── transfer/                 # Kodeki ICS / CSV oraz obsługa ContentResolver (SAF)
│   └── time/                     # Implementacja zegara systemowego
├── reminder/                     # AlarmManager, EventReminderReceiver, BootReminderReceiver
├── feature/
│   ├── shell/                    # Główna powłoka aplikacji, nawigacja adaptacyjna, menu import/eksport, stopka
│   ├── calendar/                 # Ekran kalendarza, formularze 5 typów wpisów, szczegóły, selektor RGB
│   ├── map/                      # Ekran mapy, picker lokalizacji, mostek WebView <-> Leaflet
│   ├── notes/                    # Ekran notatek, edytor Rich Text, załączniki wydarzeń
│   ├── settings/                 # Ekran ustawień, koło barw HSV, zarządzanie paletami
│   └── transfer/                 # Koordynacja importu/eksportu plików
└── ui/
    ├── components/               # Współdzielone komponenty Material 3
    └── theme/                    # Dynamiczny generator ColorScheme, typografia, ikony
```

### Kluczowe technologie i biblioteki
- **Język:** Kotlin (`2.4.21`), Coroutines & Flow (`1.10.2`), Kotlinx Serialization JSON (`1.8.1`)
- **UI:** Jetpack Compose (BOM `2024.09.00`), Material 3, Activity Compose (`1.10.1`), Lifecycle Compose (`2.8.7`)
- **Baza danych i preferencje:** Room (`2.7.0`, KSP `2.3.5`, aktualna wersja schematu: `5`), Jetpack DataStore Preferences (`1.1.7`)
- **Mapa i geokodowanie:** Leaflet (WebView + `app/src/main/assets/leaflet_map.html`), OpenStreetMap Nominatim
- **Testy:** JUnit 4 (`4.13.2`), Kotlinx Coroutines Test (`1.10.2`), Robolectric (`4.16.1`), Compose UI Test JUnit4

---

## Wymagania i budowanie

- **Android Gradle Plugin:** `9.1.1`
- **JDK:** Java `11`
- **minSdk:** `24` (Android 7.0)
- **compileSdk / targetSdk:** `36`
- **Application ID:** `com.aistudio.kalendarzreinmar.qzvkmr`

### Budowanie aplikacji

```bash
./gradlew assembleDebug
```

### Uruchamianie testów jednostkowych i Robolectric

Projekt zawiera zestaw testów w `app/src/test/java/com/example/` (m.in. `DomainModelsTest`, `ColorHarmonyEngineTest`, `NoteRichTextEditorEngineTest`, `AppColorSchemeBuilderTest`, `TransferFormatsTest`, `CalendarControllerTest`, `MapControllerTest`, `NotesControllerTest`, `SettingsControllerTest`, `TransferControllerTest`, `ExampleRobolectricTest`).

```bash
./gradlew :app:testDebugUnitTest
```

---

## Uprawnienia Android

Deklarowane w `app/src/main/AndroidManifest.xml`:
- `INTERNET` — ładowanie kafelków mapy OpenStreetMap oraz wyszukiwanie adresów (Nominatim).
- `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` — opcjonalne wyśrodkowanie mapy na bieżącej lokalizacji użytkownika (żądane w czasie działania aplikacji).
- `POST_NOTIFICATIONS` — wyświetlanie powiadomień przypomnień na Android 13+ (żądane w czasie działania aplikacji).
- `RECEIVE_BOOT_COMPLETED` — automatyczne odtwarzanie zaplanowanych przypomnień po ponownym uruchomieniu telefonu.

---

## Autor i licencja

Copyright (c) 2026 [s-reinmar](https://github.com/s-reinmar).

Projekt jest udostępniany na licencji **MIT** — szczegóły w pliku [LICENSE](LICENSE).
