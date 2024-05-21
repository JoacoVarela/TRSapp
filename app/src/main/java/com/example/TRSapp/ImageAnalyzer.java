package com.example.TRSapp;


import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import android.widget.TextView;

import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;
import com.google.mediapipe.solutions.hands.HandsResult;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class ImageAnalyzer {
    private Interpreter tflite;
    private Activity activity;
    private List<List<Keypoint>> secuenciaKeypoints = new ArrayList<>();
    private static final String[] CLASS_NAMES = {"A", "B", "C", "D", "E", "F", "I", "K", "L", "M", "N", "O", "P", "R", "T", "U", "V", "W"};
    private Hands hands;
    private TextView resultTextView;

    public ImageAnalyzer(Interpreter tflite, Activity activity, TextView resultTextView) {
        this.tflite = tflite;
        this.activity = activity;
        this.resultTextView = resultTextView;
    }

    private ByteBuffer convertToByteBuffer(List<List<Keypoint>> secuencia) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(20 * 21 * 3 * Float.BYTES);
        byteBuffer.order(ByteOrder.nativeOrder());
        for (List<Keypoint> frame : secuencia) {
            for (Keypoint keypoint : frame) {
                byteBuffer.putFloat(keypoint.x);
                byteBuffer.putFloat(keypoint.y);
                byteBuffer.putFloat(keypoint.z);
            }
        }
        return byteBuffer;
    }

    private int argMax(float[] elements) {
        int maxIndex = 0;
        float maxValue = elements[0];
        for (int i = 1; i < elements.length; i++) {
            if (elements[i] > maxValue) {
                maxIndex = i;
                maxValue = elements[i];
            }
        }
        return maxIndex;
    }

    private List<Keypoint> extractKeypoints(HandsResult handsResult) {
        if (handsResult == null || handsResult.multiHandLandmarks().isEmpty()) {
            Log.e("MediaPipe", "No se detectaron manos");
            return null;
        }

        List<Keypoint> keypoints = new ArrayList<>();
        for (LandmarkProto.NormalizedLandmark landmark : handsResult.multiHandLandmarks().get(0).getLandmarkList()) {
            Keypoint point = new Keypoint(landmark.getX(), landmark.getY(), landmark.getZ());
            keypoints.add(point);
        }

        return keypoints;
    }

    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, originalBitmap.getWidth() / 1, originalBitmap.getHeight() / 1, true);

        Matrix matrix = new Matrix();
        matrix.postRotate(-90);
        matrix.postScale(-1, 1);
        return Bitmap.createBitmap(resizedBitmap, 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);
    }

    public void analyzeImage(@NonNull ImageProxy image) {
        Bitmap bitmap = convertImageProxyToBitmap(image);
        if (bitmap != null) {
            hands.send(bitmap, image.getImageInfo().getTimestamp());
            hands.setResultListener(handsResult -> {
                List<Keypoint> keypoints = extractKeypoints(handsResult);
                if (keypoints != null && !keypoints.isEmpty()) {
                    if (secuenciaKeypoints.size() >= 20) {
                        secuenciaKeypoints.remove(0);
                    }
                    secuenciaKeypoints.add(keypoints);
                    if (secuenciaKeypoints.size() == 20) {
                        ByteBuffer inputBuffer = convertToByteBuffer(secuenciaKeypoints);
                        float[][] output = new float[1][CLASS_NAMES.length];
                        tflite.run(inputBuffer, output);
                        int predictedClass = argMax(output[0]);
                        float confidence = output[0][predictedClass];
                        activity.runOnUiThread(() -> {
                            if (confidence > 0.7) {
                                String resultText = CLASS_NAMES[predictedClass] + " - Confianza: " + confidence;
                                resultTextView.setText(resultText);
                            } else {
                                resultTextView.setText("");
                            }
                        });
                    }
                }else {
                    System.out.println("Manos no detectadas");
                    if (resultTextView.getText().length() > 0) {
                        char lastCharacter = resultTextView.getText().toString().charAt(resultTextView.getText().toString().length() - 1);
                        if (lastCharacter != ' ') {
                            resultTextView.append(" ");
                        }
                    }
                }
            });
        }
        image.close();
    }

    public  void setHands(Context context, HandsOptions options) {
       this.hands = new Hands(context, options);
    }
    public  Hands getHands() {
        return this.hands;
    }
}

