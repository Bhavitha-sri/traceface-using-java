package com.traceface.service;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

import java.util.ArrayList;
import java.util.List;

/**
 * Face detection (YuNet) and face recognition (SFace) using OpenCV.
 * The OpenCV face objects hold internal state, so every public method is synchronized.
 */
public class FaceService {

    private final FaceDetectorYN detector;
    private final FaceRecognizerSF recognizer;

    public FaceService(String detectorModel, String recognizerModel) {
        // score threshold 0.9, NMS threshold 0.3, top-K 5000
        this.detector = FaceDetectorYN.create(detectorModel, "", new Size(320, 320), 0.9f, 0.3f, 5000);
        this.recognizer = FaceRecognizerSF.create(recognizerModel, "");
    }

    /** Detect all faces. Every returned Mat is one 1x15 face row. */
    public synchronized List<Mat> detectFaces(Mat image) {
        List<Mat> result = new ArrayList<>();
        if (image == null || image.empty()) {
            return result;
        }

        detector.setInputSize(new Size(image.width(), image.height()));

        Mat faces = new Mat();
        detector.detect(image, faces);

        for (int i = 0; i < faces.rows(); i++) {
            result.add(faces.row(i).clone());
        }
        faces.release();
        return result;
    }

    /** Align the face and create its 128-dimension SFace feature. Returns null on failure. */
    public synchronized Mat createFaceFeature(Mat image, Mat face) {
        try {
            Mat aligned = new Mat();
            recognizer.alignCrop(image, face, aligned);

            Mat feature = new Mat();
            recognizer.feature(aligned, feature);

            Mat copy = feature.clone();
            aligned.release();
            feature.release();
            return copy;
        } catch (Exception e) {
            return null;
        }
    }

    /** Feature of the largest face in the uploaded image. */
    public synchronized Mat createQueryFeature(Mat image) throws FaceProcessingException {
        List<Mat> faces = detectFaces(image);
        if (faces.isEmpty()) {
            throw new FaceProcessingException("No face detected.");
        }

        Mat largest = faces.get(0);
        double largestArea = area(largest);
        for (Mat face : faces) {
            double a = area(face);
            if (a > largestArea) {
                largest = face;
                largestArea = a;
            }
        }

        Mat feature = createFaceFeature(image, largest);
        if (feature == null) {
            throw new FaceProcessingException("Could not create face feature.");
        }
        return feature;
    }

    /**
     * Compare the query feature with every face in a candidate image.
     * Returns the best cosine score, or null if the image has no usable face.
     */
    public synchronized Double compareQueryWithImage(Mat queryFeature, Mat candidateImage) {
        List<Mat> faces = detectFaces(candidateImage);
        if (faces.isEmpty()) {
            return null;
        }

        Double best = null;
        for (Mat face : faces) {
            Mat candidateFeature = createFaceFeature(candidateImage, face);
            if (candidateFeature == null) {
                continue;
            }
            double score = recognizer.match(queryFeature, candidateFeature, FaceRecognizerSF.FR_COSINE);
            candidateFeature.release();
            if (best == null || score > best) {
                best = score;
            }
        }
        return best;
    }

    private static double area(Mat face) {
        return face.get(0, 2)[0] * face.get(0, 3)[0];   // width * height
    }
}
