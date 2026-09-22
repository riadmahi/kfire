<p align="center">
  <img src="https://firebase.google.com/static/images/brand-guidelines/logo-logomark.png" alt="Firebase" width="80" height="80">
</p>

<h1 align="center">Firebase KMP</h1>

<p align="center">
  <strong>Firebase SDK for Kotlin Multiplatform</strong>
</p>

<p align="center">
  <a href="https://central.sonatype.com/search?q=com.riadmahi.firebase"><img src="https://img.shields.io/maven-central/v/com.riadmahi.firebase/firebase-core?color=blue&label=Maven%20Central" alt="Maven Central"></a>
  <img src="https://img.shields.io/badge/Kotlin-2.3.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Platform-Android%20|%20iOS-34A853" alt="Platform">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
</p>

<p align="center">
  <a href="#get-started">Get Started</a> •
  <a href="#usage">Usage</a> •
  <a href="#modules">Modules</a> •
  <a href="#manual-installation">Manual Installation</a>
</p>

---

## Get Started

The fastest way to add Firebase to your KMP project.

### Prerequisites

Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com) (just the project — no need to add apps, the CLI does it automatically)

### 1. Install the CLI

```bash
brew tap riadmahi/kfire
brew install kfire
```

### 2. Run the setup wizard

```bash
kfire init
```

### 3. That's it!

The CLI automatically:

| Step | What it does |
|------|--------------|
| **Firebase config** | Downloads `google-services.json` and `GoogleService-Info.plist` |
| **Gradle dependencies** | Adds Firebase modules to your `build.gradle.kts` |
| **Gradle plugins** | Adds `google-services` + `crashlytics` plugin (if selected) |
| **iOS dependencies** | Configures **SPM** or **CocoaPods** (your choice) |
| **Sample code** | Generates `FirebaseInit.kt` with initialization code |

```
kfire init

  ✓ firebase-tools installed
  ✓ Authenticated with Firebase
  ✓ Project scanned

  Select your Firebase project
  ● my-app (my-app-12345)

  Which Firebase services do you need?
  ✓ Authentication
  ✓ Firestore
  ○ Storage
  ○ Messaging
  ✓ Analytics
  ○ Remote Config
  ✓ Crashlytics

  How do you want to manage iOS dependencies?
  ● Swift Package Manager (SPM) — Recommended
  ○ CocoaPods

  ✓ Android configured → google-services.json
  ✓ iOS configured → GoogleService-Info.plist
  ✓ SPM configured → Firebase packages added to Xcode
  ✓ Gradle updated → firebase-core, firebase-auth, firebase-firestore, firebase-analytics, firebase-crashlytics

  Setup Complete!
```

---

## Usage

```kotlin
// Initialize once at app startup
FirebaseApp.initialize()
```

### Authentication

```kotlin
val auth = FirebaseAuth.getInstance()

// Sign in
auth.signInWithEmailAndPassword("user@example.com", "password")

// Create account
auth.createUserWithEmailAndPassword("user@example.com", "password")

// Current user
val user = auth.currentUser
```

### Firestore

```kotlin
val db = FirebaseFirestore.getInstance()

// Add a city
val city = hashMapOf(
    "name" to "Los Angeles",
    "state" to "CA",
    "country" to "USA",
    "capital" to false,
    "population" to 3900000
)
db.collection("cities").document("LA").set(city)

// Get a city
val document = db.collection("cities").document("LA").get()

// Query cities
val cities = db.collection("cities")
    .whereEqualTo("country", "USA")
    .whereGreaterThan("population", 1000000)
    .get()
```

### Storage

```kotlin
val storage = FirebaseStorage.getInstance()
val ref = storage.reference.child("images/mountains.jpg")

// Upload
ref.putBytes(imageData)

// Download URL
val url = ref.downloadUrl
```

---

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| **Core** | `firebase-core` | Firebase initialization (required) |
| **Auth** | `firebase-auth` | Authentication (Email, Google, Apple, Phone) |
| **Firestore** | `firebase-firestore` | NoSQL cloud database |
| **Storage** | `firebase-storage` | File storage and uploads |
| **Messaging** | `firebase-messaging` | Push notifications (FCM/APNs) |
| **Analytics** | `firebase-analytics` | Event tracking and user analytics |
| **Remote Config** | `firebase-remoteconfig` | Feature flags and A/B testing |
| **Crashlytics** | `firebase-crashlytics` | Crash reporting |

---

## Manual Installation

<details>
<summary>If you prefer to configure everything manually</summary>

### Gradle Dependencies

```kotlin
// build.gradle.kts (commonMain)
dependencies {
    implementation("com.riadmahi.firebase:firebase-core:1.0.0")
    implementation("com.riadmahi.firebase:firebase-auth:1.0.0")
    implementation("com.riadmahi.firebase:firebase-firestore:1.0.0")
    // ... add modules you need
}
```

### Firebase Console

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Create or select a project
3. Add Android app → Download `google-services.json` → Place in your Android app module
4. Add iOS app → Download `GoogleService-Info.plist` → Add to Xcode project

### Android Setup

```kotlin
// Root build.gradle.kts
plugins {
    id("com.google.gms.google-services") version "4.5.0" apply false
}

// App build.gradle.kts
plugins {
    id("com.google.gms.google-services")
}
```

### iOS Dependencies

Choose **SPM** or **CocoaPods**:

**Swift Package Manager**
1. In Xcode: File → Add Package Dependencies
2. Enter: `https://github.com/firebase/firebase-ios-sdk.git`
3. Select the Firebase products you need

**CocoaPods**
```ruby
# Podfile
platform :ios, '15.0'
use_frameworks!

target 'YourApp' do
  pod 'FirebaseCore'
  pod 'FirebaseAuth'
  pod 'FirebaseFirestore'
end
```

Then run `pod install` and open `.xcworkspace`.

</details>

---

## Requirements

| Platform | Minimum Version |
|----------|-----------------|
| Android | API 24 (Android 7.0) |
| iOS | 15.0 |
| Kotlin | 2.4.20 |

---

## License

```
Copyright 2025 Riad Mahi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

---

<p align="center">
  <a href="https://github.com/riadmahi/kfire">GitHub</a> •
  <a href="https://firebase.google.com/docs">Firebase Docs</a> •
  <a href="https://kotlinlang.org/docs/multiplatform.html">Kotlin Multiplatform</a>
</p>
