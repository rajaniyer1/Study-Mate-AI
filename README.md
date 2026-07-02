# Digital Binder

## Summary
An AI-powered Android study app that scans handwritten or printed notes using OCR and automatically generates flashcards and study insights using spaced repetition and NLP techniques.

## Key Features
- OCR-based note scanning using Google ML Kit
- Automatic flashcard generation system
- Custom spaced repetition algorithm (Leitner system)
- Firebase cloud sync for persistent storage
- Analytics dashboard for study tracking

## Tech Stack
- Java
- Android SDK (XML, RecyclerViews, Adapters)
- Google ML Kit (OCR)
- Firebase Firestore

## My Role / What I Built
- Implemented OCR pipeline using ML Kit
- Built flashcard generation and spaced repetition logic
- Designed Firebase data architecture and sync system
- Created UI components and study analytics dashboard

---


# Full Details
**Digital Binder** is an AI-powered study companion built natively for Android. Designed for high school and college students, this application digitizes physical notes and textbook pages, automatically extracting text to generate smart flashcards, study guides, and mastery analytics. 

By leveraging **Google ML Kit** for optical character recognition (OCR) and a custom implementation of the **Leitner Spaced Repetition Algorithm**, Digital Binder transforms passive reading into active, data-driven learning.



## Key Features

* **AI Note Scanning:** Uses Android's high-resolution `FileProvider` camera bridge to capture notes. Integrates Google ML Kit Vision API to extract raw text offline.
* **Smart Summarization (NLP):** A custom Natural Language Processing algorithm cleans extracted text (using Regex), filters out common English stop-words, and uses a Priority Queue to calculate and display the top 5 key concepts for quick review.
* **Spaced Repetition Flashcards:** Automatically generates a deck of interactive flashcards featuring custom 3D flip animations.
* **Leitner System Algorithm:** Adapts to the user's learning pace. "Knew It" responses exponentially push the review date into the future (2^(mastery-1) days), while "Forgot" responses immediately push the card to the back of the current study session queue.
* **Cloud Syncing:** Real-time data storage and retrieval using Firebase Firestore, ensuring notes are safely backed up and synced across devices.
* **Analytics Dashboard:** Calculates and displays real-time study metrics, including total AI words scanned and average deck mastery levels.

---

## Technical Stack

* **Language:** Java
* **UI/UX:** Android SDK, XML, Material Design Guidelines (Recycler Views, View Pagers, Card Views)
* **Machine Learning:** Google ML Kit Vision API (Text Recognition v2)
* **Database:** Firebase Firestore (NoSQL Cloud Database)
* **Architecture:** Object-Oriented Programming (OOP) with custom Adapters and asynchronous background processing.

---

## How to Run the Project Locally

Follow these steps to build and run the application on your local machine.

### Prerequisites
* [Android Studio](https://developer.android.com/studio) (Latest version recommended)
* An Android Emulator or physical device running Android 8.0 (API 26) or higher.
* A Google account to configure Firebase.

### Step-by-Step Installation

1. **Clone the Repository**
   `git clone https://github.com/YourUsername/Digital-Binder.git`

2. **Open in Android Studio**
   * Launch Android Studio.
   * Select **Open** and navigate to the directory where you cloned the repository.
   * Wait for Gradle to finish syncing the project dependencies.

3. **Configure Firebase (Crucial Step)**
   * Because this app relies on Firebase Firestore, you must connect it to your own Firebase project.
   * Go to the [Firebase Console](https://console.firebase.google.com/) and create a new Android project.
   * Register the app using the package name found in the `AndroidManifest.xml` (e.g., `com.example.digitalbinder`).
   * Download the `google-services.json` file provided by Firebase.
   * Place the `google-services.json` file inside the `app/` directory of this project.
   * Ensure your Firestore database rules are temporarily set to test mode (allow read/write) for local testing.

4. **Build and Run**
   * Click the green **Run (Play)** button in the top toolbar of Android Studio.
   * *Note: To test the camera functionality, testing on a physical Android device is highly recommended over the emulator.*

---

## Core Algorithms Highlight

As a portfolio project, this application demonstrates proficiency in applying algorithmic logic to real-world user problems:

**1. The Leitner Engine (Time-Complexity & Data Manipulation)**
When reviewing flashcards, the system determines the next review date dynamically:

    // Mastery calculation for exponential spaced repetition
    long intervalDays = (long) Math.pow(2, a.getMasteryLevel() - 1);
    long nextReview = System.currentTimeMillis() + (intervalDays * 24 * 60 * 60 * 1000);

If a user forgets a card, the algorithm manipulates the underlying `ArrayList` to push the object to the back of the queue, enforcing immediate review without breaking the `RecyclerView` adapter state.

**2. AI Text Cleaning & Summarization**
Raw OCR text often contains garbage characters. The app uses Regex to sanitize the string, splits it into an array, and filters it against a `HashSet` of stop-words (O(1) lookup time). A `PriorityQueue` (Max-Heap) is then used to sort word frequencies and extract the top 5 keywords in O(N log K) time.

---


## Academic Context
This application was developed as a comprehensive portfolio project to demonstrate mastery of Mobile Application Development. It successfully fulfills requirements for:
* Complex multi-screen UI navigation.
* Integration of third-party cloud APIs (Firebase) and local Machine Learning APIs (ML Kit).
* Processing and persisting user input and hardware hardware utilization (Camera/FileProvider).
* Algorithm implementation (Sorting, Filtering, and Math operations).

---
*Developed by Rajan Iyer*
