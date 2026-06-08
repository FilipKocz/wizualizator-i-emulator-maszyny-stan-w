Emulator i Wizualizator Maszyny Stanów
Interaktywne narzędzie mobilne na platformę Android służące do projektowania, testowania i symulowania skończonych automatów stanów (FSM).
🎯 Cel projektu
Projekt powstał jako narzędzie edukacyjne i inżynierskie, pozwalające na graficzne modelowanie maszyny stanów oraz natychmiastową weryfikację jej poprawności.

🚀 Funkcjonalności
Edytor Graficzny: Intuicyjne dodawanie stanów (węzłów) oraz definiowanie ich charakteru (stan początkowy, stany końcowe).
Kreator Przejść: Definiowanie relacji między stanami pod wpływem konkretnych sygnałów wejściowych.
Silnik Symulacji: Interaktywny tryb uruchomienia, w którym użytkownik może "przejść" przez maszynę, obserwując zmiany stanów w czasie rzeczywistym.
Tester (Walidator): Zaawansowany algorytm sprawdzający spójność logiczną:
  Wykrywanie duplikatów sygnałów (brak dwóch różnych wyjść dla tego samego sygnału).
  Analiza osiągalności stanów.
  Sprawdzanie poprawności definicji początku i końca grafu.
  
System Projektów: Pełna obsługa bazy danych (zapis, edycja, usuwanie wielu niezależnych maszyn stanów).
Historia Przejść: Logowanie wykonanych kroków podczas symulacji.


🛠 Technologie
Język: Kotlin
UI: Jetpack Compose (nowoczesny, deklaratywny interfejs użytkownika)
Architektura: MVVM (Model-View-ViewModel) – separacja logiki biznesowej od interfejsu.
Baza danych: Room Persistence Library (SQLite) – zapewniająca trwałość danych projektów.
Zarządzanie stanem: Kotlin Coroutines & Flow.

👤 Autor
Filip Koczorowski
