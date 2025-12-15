# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a full-stack emotion detection system consisting of an Android mobile application and a Spring Boot REST API backend. The Android app uses the device camera for real-time facial expression recognition via TensorFlow Lite (FER-2013 dataset with 7 emotions). Detection results and training images are stored in a MySQL database via the backend API. Users can authenticate, view history, and manage custom emotion training categories.

**Android App Package:** `com.ltb.sae501`
**Min SDK:** 24 | **Target SDK:** 36 | **Compile SDK:** 36

**Backend Package:** `com.ltb.sae501`
**Framework:** Spring Boot 3.2.0 | **Java:** 17 | **Database:** MySQL

## Build Commands

### Android App
```bash
# Build debug APK
gradlew assembleDebug

# Build release APK
gradlew assembleRelease

# Clean build
gradlew clean

# Install and run on connected device
gradlew installDebug

# Run unit tests
gradlew test

# Run instrumented tests (requires connected device/emulator)
gradlew connectedAndroidTest

# Run specific test class
gradlew test --tests com.ltb.sae501.ExampleUnitTest

# Run lint checks
gradlew lint
```

### Backend (Spring Boot)
```bash
# Start backend server (from project root)
cd backend
..\gradlew.bat bootRun

# Or on Linux/Mac
cd backend
../gradlew bootRun

# Run backend tests
cd backend
..\gradlew.bat test

# Build backend JAR
cd backend
..\gradlew.bat build
```

The backend runs on **http://localhost:8080** and requires MySQL with user `root` and no password.

## Architecture

This is a client-server architecture with clear separation between the Android app (client) and Spring Boot backend (server).

### System Components

**Client (Android App):**
1. **CameraX** (`EcranCamera.kt`) - Captures real-time video frames from front/back camera
2. **ML Kit Face Detection** (`EmotionDetector.kt`) - Detects faces in frames, extracts bounding boxes
3. **TensorFlow Lite Model** (`face_interpretation_model.tflite`) - Classifies emotions on 48x48 grayscale face images (runs on-device)
4. **Retrofit API Client** (`RetrofitClient.kt`, `ApiService.kt`) - Communicates with backend REST API
5. **RemoteDataSource** (`RemoteDataSource.kt`) - Wraps API calls for repository pattern

**Server (Spring Boot Backend):**
1. **REST Controllers** (`AuthController`, `RecognitionController`, `CategoryController`, `FileController`) - Expose API endpoints
2. **Services** (`AuthService`, `RecognitionService`, `CategoryService`, `JwtService`) - Business logic layer
3. **JPA Repositories** - Database access layer using Spring Data JPA
4. **Entities** (`User`, `RecognitionResult`, `RecognizedEmotion`, `EmotionCategory`, `TrainingImage`) - Database models
5. **Security** (`SecurityConfig`, `JwtAuthenticationFilter`) - JWT-based authentication with BCrypt password hashing

**Supported Emotions (FER-2013 dataset):**
- Angry (Colère) 😠
- Disgust (Dégoût) 🤢
- Fear (Peur) 😨
- Happy (Joie) 😄
- Sad (Tristesse) 😢
- Surprise (Surprise) 😲
- Neutral (Neutre) 😐

### Navigation Structure

The app uses a simple screen-based navigation system with bottom navigation bar:

- **HomeScreen** - Landing page with detection button
- **CameraScreen** - Real-time camera preview with emotion overlay (wraps `EcranCamera`)
- **HistoryScreen** - Displays past emotion recognitions from backend API
- **SettingsScreen** - App settings including dark mode and category management link
- **CategoryManagementScreen** - Upload/manage training images for emotion categories

Navigation managed in `MainActivity.kt` using mutableStateOf for currentScreen.

### Data Flow

**Recognition Process (End-to-End):**
1. **Client-side (Android):**
   - Camera frame captured via CameraX → ML Kit face detection → Face bounding boxes
   - For each detected face: Extract region → Resize to 48x48 → Convert to grayscale → Normalize [0,1]
   - TensorFlow Lite inference (on-device) → Emotion probabilities → Highest confidence selected
   - User captures → Image + emotions prepared for upload

2. **Server-side (Backend):**
   - Receive multipart request with image file + emotion data JSON
   - Store image as BLOB in MySQL database (MEDIUMBLOB column)
   - Save RecognitionResult entity with relationships to RecognizedEmotions
   - Return recognition ID and metadata to client

**Authentication Flow:**
1. User registers/logs in via Android app → POST to `/api/auth/register` or `/api/auth/login`
2. Backend validates credentials with BCrypt → Generates JWT token (24hr expiration)
3. Client stores token and includes in `Authorization: Bearer <token>` header for authenticated requests
4. `JwtAuthenticationFilter` validates token on protected endpoints

**Data Models:**

*Android (Kotlin):*
- `RecognitionResult` - Contains id, timestamp, imageData, list of RecognizedEmotions, userId
- `RecognizedEmotion` - emotion name, confidence score, bounding box coordinates
- `EmotionCategory` - Category with id, name, emoji, color, training images

*Backend (JPA Entities):*
- `User` - id, username, password (hashed), email, createdAt, lastLogin, recognitions (OneToMany)
- `RecognitionResult` - id, timestamp, imageData (BLOB), user (ManyToOne), recognizedEmotions (OneToMany)
- `RecognizedEmotion` - id, emotion, confidence, boundingBox, recognitionResult (ManyToOne)
- `EmotionCategory` - id, name, nameEn, emoji, color, description, imageCount, trainingImages (OneToMany)
- `TrainingImage` - id, filename, filePath, uploadedAt, category (ManyToOne)

### Database Schema

**MySQL Tables (via JPA):**
- `users` - User accounts with hashed passwords
- `recognition_results` - Stored detection images (MEDIUMBLOB) with timestamps
- `recognized_emotions` - Individual emotion detections with confidence scores and bounding boxes
- `emotion_categories` - Emotion definitions with metadata (7 FER-2013 categories)
- `training_images` - User-uploaded training images linked to categories

Files are stored on the backend filesystem in `./uploads/` directory (configurable via `application.yml`).

### Key Dependencies

**Android App:**
- **Jetpack Compose** - UI framework (Material 3)
- **CameraX** (1.4.0) - Camera lifecycle and preview
- **ML Kit Face Detection** (16.1.7) - Face detection
- **TensorFlow Lite** (2.14.0) - On-device ML inference
- **Retrofit** (2.9.0) - REST API client with Gson converter
- **OkHttp** (4.12.0) - HTTP client with logging interceptor
- **Coil** (2.5.0) - Async image loading
- **Kotlin Coroutines** (1.7.3) - Asynchronous operations

**Backend:**
- **Spring Boot** (3.2.0) - Web framework with Java 17 runtime
- **Spring Data JPA** - Database access with Hibernate
- **Spring Security** - Authentication and authorization
- **JWT** (jjwt 0.12.3) - Token-based authentication
- **MySQL Connector** - Database driver
- **Jackson Kotlin Module** - JSON serialization

**Note:** Android app targets Java 11 (`jvmTarget = "11"`), while backend requires Java 17 runtime.

## Development Notes

### Emotion Detection Model

The TensorFlow Lite model (`face_interpretation_model.tflite`) expects:
- **Input:** 48x48 grayscale image, normalized to [0, 1], Float32
- **Output:** 7-element Float array (probabilities for each emotion)
- **Labels:** Loaded from `labels.txt` (must match model output order)

Model loading and inference is handled in `EmotionDetector.kt:43-164`.

### Camera Implementation

CameraX is configured in `EcranCamera.kt` with:
- Real-time preview using `PreviewView`
- Image analysis for continuous face detection
- Image capture for saving detections
- Front/back camera switching via `isFrontCamera` state

The image analyzer runs on a separate executor and processes frames using ML Kit's `FaceDetector`.

### API Configuration

**Base URL Configuration:**
The Android app's API base URL is configured via `local.properties` in the project root:

```properties
# For emulator (10.0.2.2 = host machine's localhost)
api.base.url=http://10.0.2.2:8080/api/

# For physical device (replace with your computer's IP)
# api.base.url=http://192.168.1.XX:8080/api/
```

The value is injected at build time as `BuildConfig.API_BASE_URL` and used by `RetrofitClient.kt`. Default is `http://10.0.2.2:8080/api/` for emulators.

**Finding your IP address:** Run `ipconfig` (Windows) or `ifconfig` (Linux/Mac) and use the IPv4 address. Ensure the backend is running and both devices are on the same network.

### Backend API Operations

All API calls in `RemoteDataSource.kt` use Kotlin coroutines (suspend functions):
- **Authentication:** `register()`, `login()` - Return AuthResponse with JWT token
- **Recognitions:** `saveRecognition()` (multipart upload), `getAllRecognitions()`, `deleteRecognition()`
- **Categories:** `getAllCategories()`, `getCategoryById()`, `initializeCategories()`
- **Training Images:** `uploadTrainingImage()` (multipart), `deleteTrainingImage()`

The backend uses CORS configuration to allow all origins (configured in `SecurityConfig.kt`).

### Extension Functions

Important extension in `Extensions.kt`:
- `ImageProxy.versBitmap()` - Converts CameraX ImageProxy to Bitmap (required for emotion detection)

### UI Components

- `BottomNavBar.kt` - Bottom navigation with icons for all main screens
- `FaceOverlay.kt` - Draws face detection boxes and emotion labels over camera preview

### Backend Configuration

**Database Setup (`application.yml`):**
- MySQL database: `sae501_db` (auto-created if not exists)
- Default connection: `localhost:3306` with user `root` and no password
- JPA configured with `ddl-auto: update` (auto-creates/updates tables from entities)
- File uploads limited to 10MB per request
- Upload directory: `./uploads/` (relative to backend working directory)

**JWT Configuration:**
- Secret key: `SAE501SecretKeyForJWTTokenGenerationAndValidation2025` (configurable in `application.yml`)
- Token expiration: 24 hours (86400000 milliseconds)
- Tokens generated by `JwtService.kt` using HMAC-SHA256

**Security Notes:**
- Passwords hashed with BCrypt (configured in `SecurityConfig.kt`)
- Most endpoints currently set to `permitAll()` in SecurityConfig - can be changed to `authenticated()` for production
- Public endpoints: `/api/auth/**`, `/api/files/**`, `/api/categories/initialize`
- CORS configured to allow all origins (`*`) - suitable for development but should be restricted for production

**Image Storage:**
- All images (recognition results and training images) are stored as database BLOBs using JPA `@Lob` annotation
- Images stored in `MEDIUMBLOB` columns (max ~16MB per image)
- The `file.upload-dir: ./uploads/` configuration in `application.yml` is present but images are served directly from database, not filesystem
- FileController serves images from database via `/api/files/` endpoints

### API Endpoints

**Authentication:**
- `POST /api/auth/register` - Register new user (username, password, email)
- `POST /api/auth/login` - Login and receive JWT token
- `GET /api/auth/validate` - Validate JWT token (requires Authorization header)

**Recognitions:**
- `POST /api/recognitions` - Save recognition (multipart: image file + emotions JSON)
- `GET /api/recognitions` - Get all recognitions for authenticated user
- `GET /api/recognitions/{id}` - Get specific recognition by ID
- `DELETE /api/recognitions/{id}` - Delete recognition

**Categories:**
- `GET /api/categories` - Get all emotion categories
- `GET /api/categories/{id}` - Get specific category with training images
- `POST /api/categories/initialize` - Initialize default FER-2013 categories (idempotent)

**Training Images:**
- `POST /api/categories/{categoryId}/training-images` - Upload training image (multipart)
- `DELETE /api/categories/{categoryId}/training-images/{imageId}` - Delete training image

**Files:**
- `GET /api/files/{filename}` - Retrieve uploaded file (images served from database BLOBs)

### Permission Handling

Camera permission is requested in `MainActivity.onCreate()` using ActivityCompat. The app requires:
- `android.permission.CAMERA` (declared in AndroidManifest.xml)
- `android.permission.INTERNET` (for API communication)

### Code Conventions

**Naming Convention:**
The codebase uses a mix of French and English naming:
- **French**: UI-related and domain-specific components (e.g., `EcranCamera`, `detecteurEmotion`, `versBitmap()`)
- **English**: Technical/framework components (e.g., `EmotionDetector`, `RemoteDataSource`, API endpoints)
- This mixed convention is intentional and reflects the bilingual nature of the project (French UI for users, English for technical infrastructure)

## Common Setup Issues

**App can't connect to backend:**
1. Verify backend is running: `curl http://localhost:8080/api/categories/initialize`
2. Check `local.properties` has correct `api.base.url` for your setup (emulator vs physical device)
3. For physical devices: Ensure both are on same WiFi, firewall allows port 8080, and IP address is correct
4. Rebuild Android app after changing `local.properties`: `gradlew clean assembleDebug`

**Backend won't start:**
1. Verify MySQL is running and accessible on `localhost:3306`
2. Check MySQL user `root` with no password can connect
3. Ensure Java 17 is installed: `java -version`
4. Database `sae501_db` will be created automatically on first run

**Categories not showing in app:**
1. Navigate to Settings → Modify AI in the app
2. This should trigger category initialization via the backend
3. Check backend logs for errors
4. Verify database has entries in `emotion_categories` table
