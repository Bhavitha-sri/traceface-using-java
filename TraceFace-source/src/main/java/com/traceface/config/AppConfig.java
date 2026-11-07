package com.traceface.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central configuration. Secrets are read from environment variables or
 * from a ".env" file located in the TraceFace home directory.
 */
public final class AppConfig {

    /** Cosine similarity threshold recommended for SFace. */
    public static final double MATCH_THRESHOLD = 0.363;

    /** Maximum number of web images downloaded and compared. */
    public static final int MAX_WEB_CANDIDATES = 20;

    /** SerpApi image upload limit (500 KB). */
    public static final long MAX_UPLOAD_SIZE = 500L * 1024L;

    /** Maximum size of one downloaded web image (8 MB). */
    public static final long MAX_DOWNLOAD_SIZE = 8L * 1024L * 1024L;

    public static final String SERPAPI_IMAGE_URL  = "https://serpapi.com/image";
    public static final String SERPAPI_SEARCH_URL = "https://serpapi.com/search";

    public static final String BLOCKCHAIN_PREFIX = "TRACEFACE|SHA256:";

    private static final Path HOME = resolveHome();
    private static final Dotenv DOTENV = Dotenv.configure()
            .directory(HOME.toString())
            .ignoreIfMissing()
            .load();

    private AppConfig() { }

    private static Path resolveHome() {
        String home = System.getProperty("traceface.home");
        if (home == null || home.isBlank()) {
            home = System.getenv("TRACEFACE_HOME");
        }
        if (home == null || home.isBlank()) {
            home = System.getProperty("user.dir");
        }
        return Paths.get(home).toAbsolutePath();
    }

    public static Path home()            { return HOME; }
    public static Path uploadDir()       { return HOME.resolve("uploads"); }
    public static Path dataDir()         { return HOME.resolve("data"); }
    public static Path webResultsFile()  { return dataDir().resolve("web_results.json"); }
    public static Path evidenceFile()    { return dataDir().resolve("evidence_records.json"); }
    public static Path detectorModel()   { return HOME.resolve("models").resolve("face_detection_yunet.onnx"); }
    public static Path recognizerModel() { return HOME.resolve("models").resolve("face_recognition_sface.onnx"); }

    /** Environment variable first, then .env file. */
    public static String get(String key) {
        String value = DOTENV.get(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    public static String require(String key) {
        String value = get(key);
        if (value == null) {
            throw new IllegalStateException(key + " is missing from .env");
        }
        return value;
    }
}
