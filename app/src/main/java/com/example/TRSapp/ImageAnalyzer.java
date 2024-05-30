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
    private Interpreter tflite1;
    private Activity activity;
    private List<List<Keypoint>> secuenciaKeypoints = new ArrayList<>();
    private static final String[] CLASS_NAMES = { "G",  "J",  "LL",  "Q","X"};
    //{ "G",  "J",  "LL",  "Q"};
    private static final String[] CLASS_NAMES1 = {"A", "B", "C", "D", "E", "F","I", "K", "L", "M", "N", "O", "P", "R", "T", "U", "V", "W"};
    //{"A", "B", "C", "D", "E", "F", "G", "I", "J", "K", "L", "LL", "M", "N", "O", "P","Q", "R", "T", "U", "V", "W","X"};
    private Hands hands;
    private TextView resultTextView;

    private List<Keypoint> prevKeypoints = null;
    private List<Keypoint> lastKeypoints = null;
    private int CantidadNegativo = 0;
    private int CantidadPositivo = 0;
    private int framesToSkip = 0;

    private double SonIguales = 0;
    private boolean shouldProcessFrames = true;
    public ImageAnalyzer(Interpreter tflite,Interpreter tflite1, Activity activity, TextView resultTextView) {
        this.tflite = tflite;
        this.tflite1 = tflite1;
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


    private boolean isSignificantMotion(List<Keypoint> prevKeypoints, List<Keypoint> currentKeypoints, List<Keypoint> lastKeypoints) {
        float velocityThreshold = 0.05f;
        float accelerationThreshold = 0.03f;
        boolean significantMotionDetected = false;

        for (int i = 0; i < currentKeypoints.size(); i++) {
            double velocity = Math.sqrt(Math.pow(currentKeypoints.get(i).x - prevKeypoints.get(i).x, 2) +
                    Math.pow(currentKeypoints.get(i).y - prevKeypoints.get(i).y, 2) +
                    Math.pow(currentKeypoints.get(i).z - prevKeypoints.get(i).z, 2));

            double acceleration = velocity - Math.sqrt(Math.pow(prevKeypoints.get(i).x - lastKeypoints.get(i).x, 2) +
                    Math.pow(prevKeypoints.get(i).y - lastKeypoints.get(i).y, 2) +
                    Math.pow(prevKeypoints.get(i).z - lastKeypoints.get(i).z, 2));
            if (velocity > 0.20) {
                System.out.println("el velocityThreshold" + velocity + "accelerationThreshold" + acceleration);
                secuenciaKeypoints.clear();
                CantidadNegativo = 0;
                CantidadPositivo = 0;
                break;
            }
            if (Math.abs(velocity) > velocityThreshold || Math.abs(acceleration) > accelerationThreshold) {
                significantMotionDetected = true;
                break;
            }
        }

        return significantMotionDetected;
    }


        public void analyzeImage(@NonNull ImageProxy image) {
        if (shouldProcessFrames) {
            if (framesToSkip > 0) {
                framesToSkip--;
                image.close();
                secuenciaKeypoints.clear();
                return;
            }
            Bitmap bitmap = convertImageProxyToBitmap(image);
            if (bitmap != null) {
                long timestamp = image.getImageInfo().getTimestamp();
                hands.send(bitmap, timestamp);
                hands.setResultListener(handsResult -> {
                    List<Keypoint> keypoints = extractKeypoints(handsResult);
                    if (keypoints != null && !keypoints.isEmpty()) {
                        if (prevKeypoints != null && lastKeypoints != null) {
                            boolean significantMotion = isSignificantMotion(lastKeypoints, prevKeypoints, keypoints);

                            if (significantMotion) {
                                SonIguales += 1.5;
                                CantidadPositivo += 1;
                                System.out.println("el Movimiento significativo detectado.");
                            } else {
                                SonIguales -= 0.75;
                                CantidadNegativo += 1;
                            }
                            System.out.println("el Movimiento significativo detectado." + SonIguales + "cant pos" + CantidadPositivo + "cant neg" + CantidadNegativo);

                            secuenciaKeypoints.add(keypoints);
                            if (secuenciaKeypoints.size() > 20) {
                                secuenciaKeypoints.remove(0);
                            }

                            if (secuenciaKeypoints.size() == 20) {
                                if (CantidadPositivo > 9) {
                                    System.out.println("EL TAMAÑO ES: " + secuenciaKeypoints.size());
                                    doInference(tflite, CLASS_NAMES, keypoints);
                                } else if (CantidadNegativo > 10) {
                                    System.out.println("EL TAMAÑO ES: " + secuenciaKeypoints.size());
                                    doInference(tflite1, CLASS_NAMES1, keypoints);
                                    System.out.println("EL TAMAÑO ES: " + secuenciaKeypoints.size());
                                }
                            }
                        }

                        lastKeypoints = prevKeypoints;
                        prevKeypoints = keypoints;
                    } else {
                        // Si no se detecta una mano
                        System.out.println("Manos no detectadas");
                        activity.runOnUiThread(() -> {
                            if (resultTextView.getText().length() > 0) {
                                secuenciaKeypoints.clear();
                                framesToSkip = 6;
                                char lastCharacter = resultTextView.getText().toString().charAt(resultTextView.getText().toString().length() - 1);
                                if (lastCharacter != ' ') {
                                    resultTextView.append(" ");
                                }
                            }
                        });
                    }
                });
            }
        }
        image.close();
    }

    private void doInference(Interpreter interpreter, String[] classNames, List<Keypoint> keypoints) {
        if (secuenciaKeypoints.size() < 20) {
            return;
        }

        ByteBuffer inputBuffer = convertToByteBuffer(secuenciaKeypoints);
        float[][] output = new float[1][classNames.length];
        interpreter.run(inputBuffer, output);
        for (int i = 0; i < classNames.length; i++) {
            Log.d("el Inferencia", "Clase " + classNames[i] + ": " + output[0][i]);
        }
        int predictedClass = argMax(output[0]);
        float confidence = output[0][predictedClass];
        activity.runOnUiThread(() -> {
            if (confidence > 0.75) {
                String resultText = classNames[predictedClass];
                secuenciaKeypoints.clear();
                SonIguales = 0;
                CantidadNegativo = 0;
                CantidadPositivo = 0;
                framesToSkip = 16;

                if (resultText != null && !resultText.isEmpty()) {
                    if (resultTextView.getText().length() == 0) {
                        resultTextView.append(resultText);
                    } else {
                        char lastCharacter = resultTextView.getText().toString().charAt(resultTextView.getText().toString().length() - 1);
                        String lastCharacterAsString = String.valueOf(lastCharacter);
                        if (!lastCharacterAsString.equalsIgnoreCase(resultText)) {
                            resultTextView.append(resultText.toLowerCase());
                        }
                    }
                }
            }
        });
    }

    public  void setHands(Context context, HandsOptions options) {
        this.hands = new Hands(context, options);
    }
    public  Hands getHands() {
        return this.hands;
    }
}

