# AutoMeg 
### Autonomous Messaging Agent for Android

AutoMeg is a sophisticated, AI-powered autonomous messaging agent designed to handle your social communications seamlessly. I2t monitors incoming notifications from major messaging platforms and generates contextually aware, personalized responses using high-performance LLMs.

---

##  Features

- **Autonomous Intelligence**: Uses Groq-powered Llama 3.3 (70B) to generate human-like, professional replies.
- **Multi-Platform Support**: Seamlessly integrates with WhatsApp, Telegram, Instagram, Messenger, and Line.
- **Glassmorphism UI**: A premium, translucent interface featuring:
  - **Dynamic Dashboard**: Toggle the agent and specific apps with a single tap.
  - **Interaction History**: View synchronized chat logs across all monitored platforms.
  - **System Terminal**: Real-time activity logs tracking agent decisions and background events.
  - **Identity Personalization**: Define your "Digital Twin" to ensure the agent speaks in your voice.
- **Privacy First**: Conversation history and system logs are stored locally on your device.

---

##  Architecture

AutoMeg follows a modular "Observe-Think-Act" architecture to ensure reliability and low latency.

### 1. Ingestion Layer (`NotificationService`)
- Uses Android's `NotificationListenerService` to observe incoming messages.
- Filters messages based on user-enabled platforms and agent status.

### 2. Context & Memory (`MemoryManager` & `MemoryStore`)
- **Conversation Tracking**: Saves incoming and outgoing messages in isolated JSON-based chat histories.
- **Context Injection**: Retrieves the last 10 messages of a conversation to provide full context to the AI.

### 3. Decision Engine (`AgentController`)
- Evaluates the incoming text to decide if an autonomous response is appropriate.
- Prevents loops and handles "double-replies" to maintain social etiquette.

### 4. Thinking Layer (`ReplyEngine`)
- **LLM Integration**: Communicates with the Groq Cloud API using `OkHttp`.
- **System Prompting**: Injects the "User Identity" into the LLM prompt to personalize the response style.
- **Model**: Leverages `llama-3.3-70b-versatile` for high-reasoning capabilities.

### 5. Execution Layer (`ExecutorEngine`)
- Extracts the "Direct Reply" action from the original notification.
- Sends the generated AI text back to the platform without requiring the user to open the app.

---

##  Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern Glassmorphism implementation)
- **Navigation**: Compose Navigation
- **Networking**: OkHttp & Gson
- **AI**: Groq API (Llama 3.3 70B)
- **Storage**: Local File System (JSON)

---

##  Getting Started

### Prerequisites
- Android 7.0 (API 24) or higher.
- A **Groq API Key** (Place this in your `local.properties` as `GROQ_API_KEY`).

### Installation
1. Clone the repository.
2. Sync Project with Gradle.
3. Build and Run on a physical device.
4. **Important**: Grant "Notification Access" when prompted in the Dashboard to allow the agent to function.

### Configuration
Go to the **Identity** tab and provide some context about yourself (e.g., "I am a project manager, keep responses brief and polite during work hours"). This ensures AutoMeg represents you accurately.

---

##  License
*Proprietary / Internal Development*
