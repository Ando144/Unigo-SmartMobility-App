# UNIGO: Smart Urban Mobility & Transit Integration

Native Android application designed to centralize and optimize public transit data and urban mobility metrics across the metropolitan area of Bilbao and Biscay[cite: 4]. 
The platform integrates localized transit data, geospatial visualization, and background services into a cohesive user experience[cite: 4].

## 🚀 Key Features & Modules

*   **Multi-Network Transit Telemetry:** Parses and manages structured JSON data for all major regional transit networks, including Metro, Bilbobus, Bizkaibus, Euskotren, Renfe, Tranvía, and public bicycles[cite: 4].
*   **Geospatial Navigation & Heatmaps:** Interactive map interfaces (`MapFragment`) paired with spatial data density visualization (`HeatmapActivity`)[cite: 4].
*   **Real-Time Weather Integration:** Live meteorological context (`WeatherFragment`) supported by an automated background notification system (`NotificacionClimaReceiver`)[cite: 4].
*   **Secure User Identity & Persistence:** Robust local authentication engine (`LoginActivity`, `RegisterActivity`) with local SQLite database management (`DataBaseHelper`) for user state and profile configurations[cite: 4].
*   **Dynamic Localization:** Runtime multi-language support and state management via `LanguageDialogFragment` and `SettingsFragment`[cite: 4].

## 🛠 Tech Stack & Architecture

*   **Core Language:** Java[cite: 4]
*   **Target Platform:** Android SDK 
*   **Build System:** Gradle[cite: 4]
*   **Local Persistence:** SQLite (`DataBaseHelper`)[cite: 4]
*   **Asynchronous Operations:** `NetworkWorker` for non-blocking API consumption[cite: 4]
*   **Background Services:** Android BroadcastReceivers (`NotificacionClimaReceiver`)[cite: 4]

## 📂 Project Structure

The codebase strictly adheres to a modular architecture to separate business logic from UI controllers:

*   `activities/`: Core application entry points and specialized visualization screens (Heatmaps)[cite: 4].
*   `fragments/`: Modular UI controllers managing specific domains (Map, Weather, Settings, Profile, School)[cite: 4].
*   `adapters/`: Custom RecyclerView adapters (`CentroAdapter`) for efficient data binding[cite: 4].
*   `db/`: Database schemas, CRUD operations, and SQLite instantiation[cite: 4].
*   `network/`: Network operation offloading[cite: 4].
*   `receivers/`: Event-driven background task execution[cite: 4].
*   `item/`: Plain Old Java Objects (POJOs) defining data models (`Centro`, `Parada`)[cite: 4].

---

## 🔧 Setup & Installation

1. Clone this repository:
   ```bash
   git clone [https://github.com/Ando144/Unigo-SmartMobility-App.git](https://github.com/Ando144/Unigo-SmartMobility-App.git)
   Open the project directory in Android Studio.

2. Allow Gradle to synchronize dependencies.

3. Set up your Google Maps API Key in secrets.properties or local.properties (if required).

4. Build and deploy to an Android Virtual Device (AVD) or a physical device.

5. 
