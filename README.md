# DNSly Android Application 📱🛡️

> **Native Android client for DNSly — Fast, Secure, and Private DNS with On-Device Ad Blocking.**

The DNSly Android application uses Android's `VpnService` to capture local DNS requests and filter out ads, trackers, and malicious domains directly on your device without routing your regular internet traffic through a remote VPN server.

---

## ✨ Features

- **🚀 Local DNS Interception**: Uses a loopback `VpnService` to inspect only DNS traffic on port 53 without latency overhead.
- **🛡️ On-Device Blocklist Engine**: High-performance domain matcher blocking millions of ad, telemetry, and malware hosts.
- **🔒 Encrypted DNS**: Full support for DNS-over-HTTPS (DoH) and DNS-over-TLS (DoT) upstreams (Cloudflare, Quad9, Google, AdGuard, Mullvad, etc.).
- **📊 Real-Time Query Insights**: Live dashboard displaying blocked requests, allowed requests, and top-queried domains.
- **🔄 Smart Remote Sync**: Automatically fetches updated blocklists and server configurations in the background with WorkManager.
- **🔋 Battery & Data Optimized**: Zero continuous battery drain through optimized memory caching and coroutine-based background workers.
- **🎨 Modern Material 3 UI**: Beautiful interface built 100% with Jetpack Compose, dynamic theming, and dark mode support.

---

## 🏗️ Project Architecture

The app follows Android's recommended Clean Architecture with MVVM:

```
app/src/main/java/com/dnsly/app/
├── data/
│   ├── local/              # Room database, DataStore preferences, cached blocklists
│   └── repository/         # Data repositories coordinating local cache and remote API
├── model/                  # Domain models (DnsQuery, BlocklistRule, ServerConfig)
├── service/
│   ├── api/                # Retrofit & OkHttp client for backend sync
│   ├── blocklist/          # In-memory Trie/Set domain matching engine
│   └── vpn/                # VpnService, tun interface, DNS packet parser & resolver
└── ui/
    ├── components/         # Reusable Jetpack Compose UI elements
    ├── screens/            # Home, Shield, Analytics, Settings, Logs
    └── theme/              # Material 3 colors, typography, and shapes
```

---

## 📋 Prerequisites & Requirements

- **Android Studio**: Android Studio Hedgehog (2023.1.1) or newer
- **JDK**: Java Development Kit 17 (LTS)
- **Minimum SDK**: API Level 26 (Android 8.0 Oreo)
- **Target SDK**: API Level 34 (Android 14)
- **Gradle**: Gradle 8.x with Kotlin DSL (`build.gradle.kts`)

---

## 🚀 Getting Started & Building

### 1. Clone & Navigate
```bash
cd application
```

### 2. Build Debug APK
```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows PowerShell
.\gradlew.bat assembleDebug
```

The generated APK will be available at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Install & Run on Device
Connect your Android device with USB debugging enabled or start an emulator:
```bash
.\gradlew.bat installDebug
```

---

## ⚙️ Configuration & Backend Sync

The application connects to the DNSly backend API for remote configuration updates and anonymized telemetry heartbeats.

- **Default API Endpoint**: `https://dnsly.shovon.bd` (or local `http://10.0.2.2:3000` when running in Android Emulator)
- **Client Authentication**: API calls send the required `x-api-key` header to authenticate with the server.
- **Sync Schedule**: WorkManager periodically triggers heartbeat telemetry sync every hour when network is available.

---

## 🔒 Permissions Used

| Permission | Purpose |
| :--- | :--- |
| `android.permission.INTERNET` | Resolving upstream DNS queries and syncing remote blocklists |
| `android.permission.ACCESS_NETWORK_STATE` | Detecting active network connections (Wi-Fi vs Cellular) |
| `android.permission.FOREGROUND_SERVICE` | Running the local DNS VPN loopback service reliably |
| `android.permission.POST_NOTIFICATIONS` | Showing quick status and toggle controls in the notification tray |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Optional auto-start protection when the device reboots |

---

## 📄 License

Licensed under the MIT License.
