# SAE 5.01 - Application de Reconnaissance d'Émotions

Ce projet est une application complète composée d'un backend (Spring Boot), d'une application mobile (Android/Kotlin) et d'un module d'apprentissage automatique (Python/TensorFlow) pour la détection et l'interprétation des émotions faciales.

## 🏗 Architecture Technique

Le projet est divisé en deux parties principales :

### 📱 Application Android (`app`)

* **Langage :** Kotlin
* **Interface :** Jetpack Compose (Material 3)
* **Détection Faciale :** Google ML Kit Face Detection
* **Modèle ML :** TensorFlow Lite (avec support GPU)
* **Caméra :** CameraX
* **Réseau :** Retrofit & OkHttp
* **Persistance locale :** Room Database
* **Chargement d'images :** Coil

### 🖥 Backend & API (`backend`)

* **Framework :** Spring Boot 3.2.0 (Kotlin)
* **Base de données :** MySQL
* **Sécurité :** Spring Security & JWT (Json Web Token)
* **Apprentissage Machine (Serveur) :** Script Python intégré avec TensorFlow et NumPy

---

## ⚙️ Prérequis

* **Java :** JDK 17 (pour le Backend) et JDK 11+ (pour Android).
* **Android SDK :** API 36 (Compile SDK), API 24 (Min SDK).
* **Base de données :** Serveur MySQL local.
* **Python :** Python 3.x avec les dépendances listées (pour le ré-entraînement des modèles côté serveur).

---

## 🚀 Installation et Configuration

### 1. Configuration du Backend

1. **Base de données :**
Assurez-vous d'avoir un serveur MySQL en cours d'exécution. Le backend tentera de se connecter à :
* **URL :** `jdbc:mysql://localhost:3306/sae501_db`
* **Utilisateur :** `root`
* **Mot de passe :** *(vide par défaut)*
* *Note : La base de données `sae501_db` sera créée automatiquement si elle n'existe pas.*


2. **Environnement Python (pour l'entraînement) :**
Installez les dépendances Python requises pour le script d'entraînement :
```bash
cd backend
pip install -r requirements.txt
```


*Les dépendances incluent `tensorflow`, `numpy`, `pillow` et `mysql-connector-python`.*
3. **Lancer le serveur :**
```bash
./gradlew bootRun
```


Le serveur démarrera sur le port **8081**.

### 2. Configuration de l'Application Android

1. **Configuration de l'adresse IP de l'API :**
Par défaut, l'application pointe vers une adresse IP locale (`192.168.1.42`). Pour faire fonctionner l'application avec votre propre serveur local (ou émulateur), vous devez créer ou modifier le fichier `local.properties` à la racine du projet Android :
```properties
# Pour un émulateur Android (loopback vers l'hôte)
api.base.url=http://10.0.2.2:8081/api/

# OU pour un appareil physique (remplacez par l'IP de votre PC)
api.base.url=http://192.168.X.X:8081/api/
```


*Le fichier `build.gradle.kts` est configuré pour lire cette propriété.*
2. **Compilation et Lancement :**
Ouvrez le projet dans Android Studio et lancez l'application sur un émulateur ou un appareil physique.

---

## 📦 Fonctionnalités Principales

* **Authentification :** Système de connexion et d'inscription sécurisé via JWT.
* **Détection en temps réel :** Utilisation de la caméra pour détecter les visages et analyser les émotions via un modèle `.tflite` embarqué.
* **Historique :** Sauvegarde locale (Room) et synchronisation distante des émotions détectées.
* **Entraînement :** Module permettant de lancer un ré-entraînement du modèle personnalisé via le backend (interaction Python/Kotlin).
* **Gestion de profil :** Modification des paramètres utilisateur.

## 🔒 Sécurité

* Les mots de passe sont hachés (implémentation Spring Security).
* Les échanges API sont protégés par un filtre d'authentification JWT.
* Le secret JWT est configuré dans `application.yml` (Note : Pour la production, changez la clé secrète par défaut).

## 🛠 Commandes Utiles

* **Lancer les tests backend :** `./gradlew test`
* **Vérifier les dépendances (OWASP) :** `./gradlew dependencyCheckAnalyze`