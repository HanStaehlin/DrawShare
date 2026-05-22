# DrawShare

DrawShare is a minimalist, real-time collaborative drawing application for Android. It allows users to connect via a shared invite code and draw together on a synchronized canvas.

## Features

- **Real-time Sync**: Drawing strokes are synchronized instantly across devices using WebSockets (PieSocket relay).
- **End-to-End Encryption (E2EE)**: All drawing data and user profiles are encrypted using AES-128 (CBC mode) before leaving the device. Only users with the same room code can decrypt the content.
- **Home Screen Widget**: A Jetpack Glance-powered widget that displays the latest drawing received from your partner directly on your home screen.
- **Creative Tools**:
    - 25+ vibrant colors to choose from.
    - Adjustable brush size and opacity.
    - Undo/Redo and Canvas Clear functionality.
- **User Profiles**: Set a custom display name to identify yourself in the shared history.
- **History Log**: Browse through a thumbnail feed of all shared drawings in the current session.
- **Secure by Design**: Sensitive API keys and secrets are managed via the Secrets Gradle Plugin and environment variables.

## Tech Stack

- **UI**: Jetpack Compose
- **Widget**: Jetpack Glance
- **Persistence**: Room Database (SQLite)
- **Networking**: OkHttp (WebSockets), Retrofit/Moshi
- **Security**: AES-128 Encryption, SHA-256 Hashing, Secrets Gradle Plugin
- **Architecture**: MVVM with StateFlow and Coroutines

## Getting Started

### Prerequisites
- [Android Studio Ladybug](https://developer.android.com/studio) or newer.
- A PieSocket API Key (provided in `.env` by default for testing).

### Installation
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/DrawShare.git
    ```
2.  **Configuration**:
    - Ensure you have a `.env` file in the root directory (based on `.env.example`).
    - The `PIESOCKET_API_KEY` should be set.
3.  **Build & Run**:
    - Open the project in Android Studio.
    - Click **Sync Project with Gradle Files**.
    - Run the `app` module on an emulator or physical device.

## How to Use
1.  **Enter your name** on the starting screen.
2.  **Create a Board**: Tap "Create New Draw Board" to generate a 5-digit code.
3.  **Invite a Friend**: Share the code via the "Share" button in the settings tab.
4.  **Join a Board**: Have your friend enter the code on their device and tap "Connect Board".
5.  **Draw & Send**: Draw on the canvas and hit "Send Out" to share it instantly.

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
