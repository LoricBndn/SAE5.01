-- SAE 5.01 - Database Schema for Emotion Recognition System
-- Migration from Firebase to MySQL

-- Table 1: Users (for authentication)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at BIGINT NOT NULL,
    last_login BIGINT NOT NULL,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table 2: Emotion Categories
CREATE TABLE IF NOT EXISTS emotion_categories (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    emoji VARCHAR(10),
    color VARCHAR(7),
    description TEXT,
    image_count INT DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table 3: Custom Models
CREATE TABLE IF NOT EXISTS custom_models (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    accuracy FLOAT NOT NULL,
    metadata JSON NOT NULL,
    model_data MEDIUMBLOB NOT NULL,
    version VARCHAR(50) NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table 4: Training Images (for emotion categories)
CREATE TABLE IF NOT EXISTS training_images (
    id VARCHAR(255) PRIMARY KEY,
    category_id VARCHAR(50) NOT NULL,
    custom_model_id VARCHAR(255) NOT NULL,
    image_data MEDIUMBLOB NOT NULL,
    file_name VARCHAR(255),
    uploaded_at BIGINT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES emotion_categories(id) ON DELETE CASCADE,
    FOREIGN KEY (custom_model_id) REFERENCES custom_models(id) ON DELETE CASCADE,
    INDEX idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table 5: Recognition Results
CREATE TABLE IF NOT EXISTS recognition_results (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    image_data MEDIUMBLOB NOT NULL,
    detected_at BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table 6: Recognized Emotions (one-to-many with recognition_results)
CREATE TABLE IF NOT EXISTS recognized_emotions (
    id VARCHAR(255) PRIMARY KEY,
    recognition_id VARCHAR(255) NOT NULL,
    emotion_id VARCHAR(50) NOT NULL,
    confidence FLOAT NOT NULL,
    detected_at BIGINT NOT NULL,
    FOREIGN KEY (recognition_id) REFERENCES recognition_results(id) ON DELETE CASCADE,
    FOREIGN KEY (emotion_id) REFERENCES emotion_categories(id) ON DELETE CASCADE,
    INDEX idx_recognition_id (recognition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default FER-2013 emotion categories
INSERT INTO emotion_categories (id, name, name_en, emoji, color, description, image_count, created_at, updated_at) VALUES
('angry', 'Colère', 'Angry', '😠', '#FF5252', 'Émotion de colère', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('disgust', 'Dégoût', 'Disgust', '🤢', '#9C27B0', 'Émotion de dégoût', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('fear', 'Peur', 'Fear', '😨', '#673AB7', 'Émotion de peur', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('happy', 'Joie', 'Happy', '😄', '#FFC107', 'Émotion de joie', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('sad', 'Tristesse', 'Sad', '😢', '#2196F3', 'Émotion de tristesse', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('surprise', 'Surpris', 'Surprise', '😲', '#FF9800', 'Émotion de surprise', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000),
('neutral', 'Neutre', 'Neutral', '😐', '#9E9E9E', 'Émotion neutre', 0, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000)
ON DUPLICATE KEY UPDATE name=name; -- Avoid duplicates if already exists
