# Budget App

## Overview

**Budget App** is a Kotlin-based mobile application developed using Android Studio. Designed as a final year capstone project, it aims to simplify personal finance management by enabling users to track, analyze, and plan their financial activities through a modern, modular, and user-friendly mobile interface. The app is tailored to support local banking needs, with advanced support for Turkish banks like **Ziraat Bank** and **VakıfBank**, allowing users to parse PDF and Excel bank statements into structured financial data.

## Features

* **Smart Statement Parsing:**
  * Extracts critical information such as billing periods, due dates, transaction amounts, and descriptions from bank statements using regex.
  * Supports **PDF (VakıfBank)** and **Excel (Ziraat Bank - Bankkart)** formats.

* **Transaction Management:**
  * Import and preview bank transactions before persisting them to the Room database.
  * Add manual transactions with category tagging and notes.

* **Analytics & Reporting:**
  * Graphical representation of spending habits using MPAndroidChart (pie, bar, and line charts).
  * Insights include category-wise distributions, monthly trends, and bank-wise balances.

* **Budget Goal Tracking:**
  * Set monthly spending targets by category.
  * Visual progress indicators and warnings for budget overruns.

* **Backup and Export:**
  * Automatic weekly backups in JSON format.
  * Export financial records to Excel or PDF.

* **Custom UI with Jetpack Compose:**
  * Built using Material Design 3 principles with adaptive layouts for different screen sizes.
  * Dark/light mode support and high accessibility (TalkBack compatible).

* **MVVM Architecture:**
  * Clean separation of concerns using ViewModel, LiveData, and Repository patterns.
  * Dependency Injection via Hilt.

## ScreenShots
<img width="335" height="706" alt="image" src="https://github.com/user-attachments/assets/7746eedb-ccbd-42b1-ad4b-eb1ebfc150b6" />

<img width="249" height="526" alt="image" src="https://github.com/user-attachments/assets/4a395926-9d50-4493-98db-1da43ba45944" />

<img width="272" height="573" alt="image" src="https://github.com/user-attachments/assets/a37fec83-f171-4ec3-a186-168276a59e9f" />

<img width="236" height="499" alt="image" src="https://github.com/user-attachments/assets/b94705c4-946b-49fc-b57a-8eff662f4fcf" />

<img width="321" height="678" alt="image" src="https://github.com/user-attachments/assets/1303361f-3b71-4504-8a46-772a4e16a153" />

<img width="345" height="727" alt="image" src="https://github.com/user-attachments/assets/c8ba47d7-8d27-422e-9a13-67b552eba274" />

<img width="289" height="609" alt="image" src="https://github.com/user-attachments/assets/a364574e-a515-4bd0-b114-4a11b76adb2c" />


## Project Structure

```
app/
 └── src/
      └── main/
          ├── java/com/example/budget/
          │    ├── adapter/           # RecyclerView adapters
          │    ├── analytics/         # Spending analysis and MPAndroidChart usage
          │    ├── data/              # Room DB entities, DAOs, migrations
          │    ├── parsers/           # PDF and Excel parsing for banks
          │    ├── ui/                # Activities and Fragments
          │    ├── utils/             # Regex patterns, Export/Backup logic
          │    ├── viewmodel/         # ViewModels and ViewModelFactory
          │    └── views/             # Custom Views like ChartView
          ├── res/                    # Layouts, drawables, strings, themes
          └── AndroidManifest.xml
```

## Getting Started

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/ferdi-kanat/budget-app.git
   cd budget-app
   ```

2. **Open in Android Studio:**

   * Ensure Kotlin plugin and Android Gradle Plugin are up to date.

3. **Build and Run:**

   * Use a real device or emulator with minimum SDK specified in the project.

## Requirements

* Android Studio (Hedgehog or newer recommended)
* Kotlin 1.9+
* Minimum SDK: API 24 (Android 7.0)
* Internet not required (fully offline-capable)

## Architecture Highlights

* **Jetpack Compose:** Declarative UI system for building modern interfaces.
* **Room Database:** Local storage for transactions, budgets, and settings.
* **Coroutines + Flow:** Asynchronous data handling with reactive patterns.
* **Material Design 3:** Seamless experience aligned with latest Android UX standards.

## Research Contribution

This application is a deliverable of a final year thesis project submitted to Yalova University (2025), titled:

> "Kişisel Bütçe Uygulaması – Personal Budget Management Application"

The goal is to bridge the gap in the Turkish market for mobile apps that natively support local banking formats. It not only empowers users to manage finances more consciously but also serves as an open-source framework adaptable to other regional formats.

## Future Improvements

* Add AI-driven spending prediction based on past transactions.
* Extend statement parsing to additional banks.
* Integrate cloud synchronization for cross-device support.
* Implement multi-currency and language support.

## Contributing

Contributions are encouraged! You can fork the repo, submit PRs, or create issues for bugs or feature requests.

## License

This project is licensed under the **MIT License**.

---

**Note:** This app currently supports bank statements from Ziraat Bank (Excel) and VakıfBank (PDF). For more details, refer to in-code documentation and thesis report.
