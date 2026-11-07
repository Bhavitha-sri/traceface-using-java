package com.traceface.service;

import com.traceface.config.AppConfig;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Downloads a web image and decodes it with OpenCV. */
public class ImageDownloader {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
          + "(KHTML, like Gecko) Chrome/140 Safari/537.36";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /** Returns the decoded image, or null if it cannot be downloaded / decoded. */
    public Mat download(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || !isSafePublicUrl(imageUrl)) {
            return null;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                response.body().close();
                return null;
            }

            // Accept image responses, but do not reject a missing Content-Type header.
            String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
            if (!contentType.isEmpty() && !contentType.contains("image")) {
                response.body().close();
                return null;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = response.body()) {
                byte[] chunk = new byte[65536];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                    if (buffer.size() > AppConfig.MAX_DOWNLOAD_SIZE) {
                        return null;                       // too large
                    }
                }
            }

            if (buffer.size() == 0) {
                return null;
            }

            MatOfByte raw = new MatOfByte(buffer.toByteArray());
            Mat image = Imgcodecs.imdecode(raw, Imgcodecs.IMREAD_COLOR);
            raw.release();
            return (image == null || image.empty()) ? null : image;

        } catch (Exception e) {
            return null;
        }
    }

    /** Only http(s) URLs that resolve to a public address are fetched (basic SSRF protection). */
    static boolean isSafePublicUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            InetAddress address = InetAddress.getByName(uri.getHost());
            return !(address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress());
        } catch (Exception e) {
            return false;
        }
    }
}
