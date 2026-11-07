package com.traceface.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.traceface.config.AppConfig;
import com.traceface.model.SearchItem;
import com.traceface.model.SearchResults;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** SerpApi + Google Lens reverse image search. */
public class WebSearchService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Image to upload and whether it is a temporary compressed copy. */
    public static final class PreparedImage {
        public final Path path;
        public final boolean temporary;
        PreparedImage(Path path, boolean temporary) { this.path = path; this.temporary = temporary; }
    }

    // ------------------------------------------------------------------
    // Full search pipeline
    // ------------------------------------------------------------------

    public SearchResults search(Path uploadedImage) throws Exception {
        PreparedImage prepared = prepareImage(uploadedImage);
        try {
            String imageId = uploadImage(prepared.path);
            JsonObject raw = searchGoogleLens(imageId);
            SearchResults results = extractResults(raw);
            saveResults(results, raw);
            return results;
        } finally {
            if (prepared.temporary) {
                try { Files.deleteIfExists(prepared.path); } catch (IOException ignored) { }
            }
        }
    }

    // ------------------------------------------------------------------
    // Compress image below SerpApi's 500 KB limit
    // ------------------------------------------------------------------

    PreparedImage prepareImage(Path imagePath) throws IOException {
        if (!Files.isRegularFile(imagePath)) {
            throw new IOException("Image not found: " + imagePath);
        }
        if (Files.size(imagePath) <= AppConfig.MAX_UPLOAD_SIZE) {
            return new PreparedImage(imagePath, false);
        }

        Mat image = Imgcodecs.imread(imagePath.toString());
        if (image.empty()) {
            throw new IOException("OpenCV could not read the image.");
        }

        Path temp = Files.createTempFile("traceface-", ".jpg");
        int width = image.width();
        int height = image.height();
        int quality = 85;
        boolean success = false;

        for (int attempt = 0; attempt < 12; attempt++) {
            MatOfByte encoded = new MatOfByte();
            boolean ok = Imgcodecs.imencode(".jpg", image, encoded,
                    new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, quality));
            if (ok) {
                byte[] bytes = encoded.toArray();
                Files.write(temp, bytes);
                if (bytes.length <= AppConfig.MAX_UPLOAD_SIZE - 20 * 1024) {
                    success = true;
                    break;
                }
            }
            encoded.release();

            width = Math.max(300, (int) (width * 0.85));
            height = Math.max(300, (int) (height * 0.85));
            Mat resized = new Mat();
            Imgproc.resize(image, resized, new Size(width, height), 0, 0, Imgproc.INTER_AREA);
            image.release();
            image = resized;
            if (quality > 55) {
                quality -= 5;
            }
        }
        image.release();

        if (!success) {
            Files.deleteIfExists(temp);
            throw new IOException("Could not compress image below SerpApi's 500 KB limit.");
        }
        return new PreparedImage(temp, true);
    }

    // ------------------------------------------------------------------
    // Upload image to SerpApi (multipart/form-data)
    // ------------------------------------------------------------------

    String uploadImage(Path imagePath) throws Exception {
        String boundary = "----TraceFace" + UUID.randomUUID();
        String fileName = imagePath.getFileName().toString();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, "--" + boundary + "\r\n");
        write(body, "Content-Disposition: form-data; name=\"api_key\"\r\n\r\n");
        write(body, AppConfig.require("SERPAPI_API_KEY") + "\r\n");
        write(body, "--" + boundary + "\r\n");
        write(body, "Content-Disposition: form-data; name=\"image\"; filename=\"" + fileName + "\"\r\n");
        write(body, "Content-Type: application/octet-stream\r\n\r\n");
        body.write(Files.readAllBytes(imagePath));
        write(body, "\r\n--" + boundary + "--\r\n");

        HttpRequest request = HttpRequest.newBuilder(URI.create(AppConfig.SERPAPI_IMAGE_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject data = parseJson(response.body(), "SerpApi returned a non-JSON response.");

        if (response.statusCode() != 200 || data.has("error")) {
            String message = data.has("error") ? data.get("error").getAsString()
                                               : "HTTP " + response.statusCode();
            throw new IOException("SerpApi image upload failed: " + message);
        }
        if (!data.has("image_id")) {
            throw new IOException("SerpApi did not return an image_id.");
        }
        return data.get("image_id").getAsString();
    }

    // ------------------------------------------------------------------
    // Google Lens search
    // ------------------------------------------------------------------

    JsonObject searchGoogleLens(String imageId) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("engine", "google_lens");
        params.put("image_id", imageId);
        params.put("type", "all");
        params.put("hl", "en");
        params.put("country", "in");
        params.put("safe", "active");
        params.put("api_key", AppConfig.require("SERPAPI_API_KEY"));
        params.put("output", "json");

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (query.length() > 0) query.append('&');
            query.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                 .append('=')
                 .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(AppConfig.SERPAPI_SEARCH_URL + "?" + query))
                .timeout(Duration.ofSeconds(90))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject data = parseJson(response.body(), "SerpApi returned a non-JSON search response.");

        if (response.statusCode() != 200 || data.has("error")) {
            String message = data.has("error") ? data.get("error").getAsString()
                                               : "HTTP " + response.statusCode();
            throw new IOException("Google Lens search failed: " + message);
        }
        return data;
    }

    // ------------------------------------------------------------------
    // Extract useful fields
    // ------------------------------------------------------------------

    SearchResults extractResults(JsonObject data) {
        SearchResults results = new SearchResults();

        for (JsonElement e : array(data, "exact_matches")) {
            JsonObject o = e.getAsJsonObject();
            results.getExactMatches().add(new SearchItem(
                    str(o, "title"), str(o, "link"), str(o, "source"),
                    str(o, "image"), str(o, "thumbnail"), true));
        }

        for (JsonElement e : array(data, "visual_matches")) {
            JsonObject o = e.getAsJsonObject();
            boolean exact = o.has("exact_matches") && !o.get("exact_matches").isJsonNull()
                    && o.get("exact_matches").getAsBoolean();
            results.getVisualMatches().add(new SearchItem(
                    str(o, "title"), str(o, "link"), str(o, "source"),
                    str(o, "image"), str(o, "thumbnail"), exact));
        }

        for (JsonElement e : array(data, "related_content")) {
            JsonObject o = e.getAsJsonObject();
            results.getRelatedContent().add(new String[]{str(o, "query"), str(o, "link"), str(o, "thumbnail")});
        }
        return results;
    }

    private void saveResults(SearchResults results, JsonObject raw) throws IOException {
        JsonObject out = new JsonObject();
        out.add("exact_matches", prettyGson.toJsonTree(results.getExactMatches()));
        out.add("visual_matches", prettyGson.toJsonTree(results.getVisualMatches()));
        out.add("related_content", prettyGson.toJsonTree(results.getRelatedContent()));
        out.add("search_metadata", raw.has("search_metadata") ? raw.get("search_metadata") : new JsonObject());

        Files.createDirectories(AppConfig.dataDir());
        Files.writeString(AppConfig.webResultsFile(), prettyGson.toJson(out), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void write(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject parseJson(String text, String errorMessage) throws IOException {
        try {
            JsonElement element = JsonParser.parseString(text);
            if (!element.isJsonObject()) throw new IllegalStateException();
            return element.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException(errorMessage);
        }
    }

    private static JsonArray array(JsonObject o, String key) {
        return (o.has(key) && o.get(key).isJsonArray()) ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static String str(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }
}
