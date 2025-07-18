NousGuard - Your Offline Healthcare Assistant
NousGuard is an Android application designed to be a personal, offline healthcare assistant. Its primary focus is to provide support for mental well-being through a secure journaling feature and an intelligent chatbot that operates entirely without an internet connection for its core functions.

✨ Features
Offline Chatbot: A core chatbot that provides responses based on pre-trained models, ensuring functionality even without an internet connection. This is designed to be a supportive companion for mental health queries.

Secure Journaling: A dedicated section for users to record their thoughts, moods, and feelings securely. This data is stored locally using Room Database.

Chat History: View all past conversations with the chatbot, complete with timestamps, stored securely on the device.

Offline Data Persistence: All user data (journal entries, chat history) is stored locally using Room Database, ensuring privacy and accessibility without internet.

🚀 Technology Stack
Kotlin: The primary programming language for Android development.

Jetpack Compose: Modern Android UI toolkit for building native UIs.

Room Persistence Library: For local, offline data storage (Journal entries, Chat History).

AndroidX Navigation Compose: For managing navigation between screens.

Biometric Authentication: For secure access to the app.

ONNX Runtime: For running the pre-trained Intent Classification model offline.

TensorFlow Lite: For running the pre-trained Emotion Classification model offline.

🛠️ Setup Instructions
Follow these steps to get NousGuard up and running on your local machine.

Prerequisites
Android Studio: Latest stable version (e.g., Flamingo, Giraffe, Hedgehog).

Java Development Kit (JDK): Version 17 or higher.

Kotlin Plugin: Ensure the Kotlin plugin is installed in Android Studio.

Getting Started
Clone the repository:

git clone https://github.com/Mspanti/NousGuard.git
cd NousGuard

Open in Android Studio:

Open Android Studio.

Select File > Open... and navigate to the NousGuard project directory.

Gradle Sync:

Android Studio should automatically perform a Gradle sync. If not, click the "Sync Project with Gradle Files" button (looks like a Gradle elephant with arrows) in the toolbar.

Important: Ensure your project-level (build.gradle.kts) and module-level (app/build.gradle.kts) Gradle files match the standard Android project setup, especially the plugins and dependencies blocks.

Place Models in Assets:

Ensure that your pre-trained ONNX (intent_model_quantized.onnx) and TFLite (emotion_model.tflite) models, along with their respective tokenizer (tokenizer.json) and label map (intent_label_map.json, emotion_label_map.json) files, are placed in the app/src/main/assets folder of your Android project. These files are crucial for the offline chatbot functionality.

Run the Application:

Connect an Android device or start an Android Emulator.

Click the "Run 'app'" button (green play icon) in the Android Studio toolbar.

🧠 Offline AI Agent Strategy
The core philosophy of NousGuard's chatbot is to operate entirely offline. To achieve intelligence without constant internet access, the application leverages pre-trained machine learning models:

Intent Classification (ONNX Model):

A pre-trained ONNX model (e.g., intent_model_quantized.onnx) is used to classify the user's input into specific intents (e.g., "greeting", "share_feeling", "seeking_help").

This model, along with its tokenizer (tokenizer.json) and label mapping (intent_label_map.json), is bundled directly within the app's assets.

Emotion Classification (TFLite Model):

A pre-trained TensorFlow Lite (TFLite) model (e.g., emotion_model.tflite) is used to detect the emotional tone of the user's input (e.g., "sadness", "anxiety", "joy").

This model also comes with its own tokenizer (which might be shared with the intent model) and label mapping (emotion_label_map.json), all stored in the app's assets.

Hybrid Response Generation:

The ChatbotBrain.kt class orchestrates the inference from both the ONNX (intent) and TFLite (emotion) models.

Based on the predicted intent and emotion, the chatbot generates a contextually appropriate and empathetic response using a set of pre-defined rules. This hybrid approach allows for more nuanced and helpful interactions without requiring an internet connection.

🤝 Contributing
We welcome contributions to the NousGuard project! If you have ideas for features, bug fixes, or improvements, please feel free to:

Fork the repository.

Create a new branch (git checkout -b feature/your-feature-name).

Make your changes.

Commit your changes (git commit -m 'Add new feature').

Push to the branch (git push origin feature/your-feature-name).

Open a Pull Request.

📄 License
This project is licensed under the MIT License.
