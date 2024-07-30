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
import java.util.LinkedList;
import java.util.List;

public class ImageAnalyzer {
    private Interpreter tflite;
    private Interpreter tfliteTwoHands;
    private Activity activity;
    private List<List<Keypoint>> secuenciaKeypointsUnaMano = new LinkedList<>();
    private List<List<Keypoint>> secuenciaKeypointsDosManos = new LinkedList<>();
    private static final String[] CLASS_NAMES = {"A", "B", "C", "CH", "D", "E", "F", "G", "H","I", "J", "K", "L", "LL", "M", "N", "NI", "O", "P", "Q", "R", "RR","S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] CLASS_NAMES1 = {  "bienvenidos", "buenos dias ", "compañeros ", "estudiantes ", "muchas gracias "};
    private Hands hands;
    private TextView resultTextView;

    private List<Keypoint> prevKeypoints = null;
    private List<Keypoint> lastKeypoints = null;

    private int framesToSkip = 0;

    private boolean shouldProcessFrames = true;
    private int activeCamera;
    private int noHandDetectedCount = 0; // Contador de frames sin detección de manos
    private static final int NO_HAND_DETECTED_THRESHOLD = 4; // Umbral para agregar un espacio

    private int frameCounter = 0; // Contador de frames
    private int inferenceInterval = 2; // Intervalo de inferencia (ajustable)
    private int twoHandsDetectionCount = 0;
    private static final int TWO_HANDS_DETECTION_THRESHOLD =2; // Número de frames consecutivos con detección de dos manos


    public ImageAnalyzer(Interpreter tflite, Interpreter tfliteTwoHands, Activity activity, TextView resultTextView) {
        this.tflite = tflite;
        this.tfliteTwoHands = tfliteTwoHands;
        this.activity = activity;
        this.resultTextView = resultTextView;
    }

    public void setActiveCamera(int activeCamera) {
        this.activeCamera = activeCamera;
        System.out.println("La camara es> " + activeCamera);
    }

    // Convertir una lista de keypoints a un ByteBuffer para la inferencia
    private ByteBuffer convertToByteBuffer(List<List<Keypoint>> secuencia, int numKeypoints) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(20 * numKeypoints * 3 * Float.BYTES);
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

    // Encontrar el índice del valor máximo en un array de floats
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

    // Extraer keypoints de una mano detectada
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

    // Extraer keypoints de dos manos detectadas
    private List<List<Keypoint>> extractKeypointsForTwoHands(HandsResult handsResult) {
        if (handsResult == null || handsResult.multiHandLandmarks().isEmpty() || handsResult.multiHandLandmarks().size() < 2) {
            Log.e("MediaPipe", "No se detectaron dos manos");
            return null;
        }

        List<Keypoint> keypointsRightHand = new ArrayList<>();
        List<Keypoint> keypointsLeftHand = new ArrayList<>();

        for (int handIndex = 0; handIndex < handsResult.multiHandLandmarks().size(); handIndex++) {
            List<Keypoint> keypoints = new ArrayList<>();
            for (LandmarkProto.NormalizedLandmark landmark : handsResult.multiHandLandmarks().get(handIndex).getLandmarkList()) {
                Keypoint point = new Keypoint(landmark.getX(), landmark.getY(), landmark.getZ());
                keypoints.add(point);
            }

            // Determinar si es la mano derecha o izquierda
            String label = handsResult.multiHandedness().get(handIndex).getLabel();
            if (label.equals("Right")) {
                keypointsRightHand = keypoints;
            } else if (label.equals("Left")) {
                keypointsLeftHand = keypoints;
            }
        }

        List<List<Keypoint>> keypointsBothHands = new ArrayList<>();
        keypointsBothHands.add(keypointsRightHand);
        keypointsBothHands.add(keypointsLeftHand);

        return keypointsBothHands;
    }

    // Convertir un ImageProxy a un Bitmap
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
        if (activeCamera == 0) {
            matrix.postRotate(-90);
            matrix.postScale(-1, 1); // Espejo horizontal para la cámara frontal
        } else {
            matrix.postRotate(90); // Ajusta esto según la orientación de tu cámara trasera
            matrix.postScale(-1, 1); // Espejo horizontal para la cámara trasera
        }
        return Bitmap.createBitmap(resizedBitmap, 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);
    }


    public void analyzeImage(@NonNull ImageProxy image) {
        if (shouldProcessFrames) {
            if (framesToSkip > 0) {
                secuenciaKeypointsUnaMano.clear();
                secuenciaKeypointsDosManos.clear();
                framesToSkip--;
                image.close();
                return;
            }
            Bitmap bitmap = convertImageProxyToBitmap(image);
            if (bitmap != null) {
                long timestamp = image.getImageInfo().getTimestamp();
                hands.send(bitmap, timestamp);
                hands.setResultListener(handsResult -> {
                    int handCount = handsResult.multiHandLandmarks().size();
                    if (handCount == 1) {
                        twoHandsDetectionCount = 0; // Resetear el contador si se detecta una sola mano
                        // Limpiar el buffer de dos manos si se detecta una sola mano después de detectar dos manos
                        if (!secuenciaKeypointsDosManos.isEmpty() && secuenciaKeypointsDosManos.size()>=16) {
                            // Obtener el último frame de la secuencia de dos manos
                            List<Keypoint> lastFrame = secuenciaKeypointsDosManos.get(secuenciaKeypointsDosManos.size() - 1);

                            // Copiar el último frame hasta completar 20 frames
                            while (secuenciaKeypointsDosManos.size() < 20) {
                                secuenciaKeypointsDosManos.add(new ArrayList<>(lastFrame));
                            }

                            // Realizar la inferencia con la secuencia completada
                            doInference(tfliteTwoHands, CLASS_NAMES1, lastFrame, 42, secuenciaKeypointsDosManos);
                        }
                        List<Keypoint> keypoints = extractKeypoints(handsResult);
                        if (keypoints != null) {
                            processSingleHand(keypoints);
                        }
                    } else if (handCount == 2 && handsResult.multiHandLandmarks().get(1).getLandmarkList().get(0).getX() > 0.0) {
                        twoHandsDetectionCount++;
                        if (twoHandsDetectionCount >= TWO_HANDS_DETECTION_THRESHOLD) {
                            List<List<Keypoint>> keypointsBothHands = extractKeypointsForTwoHands(handsResult);
                            if (keypointsBothHands != null) {
                                processTwoHands(keypointsBothHands);
                            }
                        }
                    } else {
                        // Si no se detecta una mano
                        handleNoHandsDetected();
                        twoHandsDetectionCount = 0;
                    }
                });
            }
        }
        image.close();
    }



    // Procesar keypoints de una mano
    private void processSingleHand(List<Keypoint> keypoints) {
        if (keypoints != null && !keypoints.isEmpty()) {
            noHandDetectedCount = 0; // Resetear el contador si se detecta una mano

            if (prevKeypoints != null && lastKeypoints != null) {
                secuenciaKeypointsUnaMano.add(keypoints);
                if (secuenciaKeypointsUnaMano.size() > 20) {
                    secuenciaKeypointsUnaMano.remove(0);
                }

                frameCounter++;
                if (frameCounter % inferenceInterval == 0) { // Realizar inferencia cada 'inferenceInterval' frames
                    doInference(tflite, CLASS_NAMES, keypoints, 21, secuenciaKeypointsUnaMano);
                    frameCounter = 0;
                }
            }
            lastKeypoints = prevKeypoints;
            prevKeypoints = keypoints;
        }
    }

    // Procesar keypoints de dos manos
    // Añadir este método para imprimir los keypoints
    private void printKeypoints(List<Keypoint> keypoints, String handLabel) {
        System.out.println("Coordenadas de la " + handLabel + ":");
        for (int i = 0; i < keypoints.size(); i++) {
            Keypoint keypoint = keypoints.get(i);
            System.out.println("coordenada Keypoint " + i + ": x=" + keypoint.x + ", y=" + keypoint.y + ", z=" + keypoint.z);
        }
    }

    // Procesar keypoints de dos manos
    // Verificar si la segunda mano es razonable
    private boolean isReasonableSecondHand(List<Keypoint> firstHand, List<Keypoint> secondHand) {
        if (firstHand == null || secondHand == null || firstHand.isEmpty() || secondHand.isEmpty()) {
            return false;
        }

        double distancia = calcularDistancia(firstHand.get(0), secondHand.get(0));
        double umbralMin = 0.05; // Umbral mínimo de distancia entre las manos (ajustar según sea necesario)
        double umbralMax = 0.8; // Umbral máximo de distancia entre las manos (ajustar según sea necesario)
        System.out.println("distancia " + distancia);
        return distancia > umbralMin && distancia < umbralMax;
    }

    // Procesar keypoints de dos manos
    private void processTwoHands(List<List<Keypoint>> keypointsBothHands) {
        if (keypointsBothHands != null && keypointsBothHands.size() == 2 && !keypointsBothHands.get(0).isEmpty() && !keypointsBothHands.get(1).isEmpty()) {
            noHandDetectedCount = 0; // Resetear el contador si se detectan dos manos

            // Verificar si la segunda mano es razonable
            if (!isReasonableSecondHand(keypointsBothHands.get(0), keypointsBothHands.get(1))) {
                Log.d("Detección", "Detección de segunda mano inestable, descartando frame");
                return;
            }

            List<Keypoint> combinedKeypoints = new ArrayList<>(keypointsBothHands.get(0));
            combinedKeypoints.addAll(keypointsBothHands.get(1));

            secuenciaKeypointsDosManos.add(combinedKeypoints);
            System.out.println("el valor de secuenciaKeypointsDosManos es " + secuenciaKeypointsDosManos.size());
            if (secuenciaKeypointsDosManos.size() > 20) {
                secuenciaKeypointsDosManos.remove(0);
            }

            // Imprimir las coordenadas de los keypoints de ambas manos
            printKeypoints(keypointsBothHands.get(0), "mano derecha");
            printKeypoints(keypointsBothHands.get(1), "mano izquierda");

            frameCounter++;
            if (frameCounter % 3 == 0) { // Realizar inferencia cada 'inferenceInterval' frames
                doInference(tfliteTwoHands, CLASS_NAMES1, combinedKeypoints, 42, secuenciaKeypointsDosManos);
                frameCounter = 0;
            }

            lastKeypoints = prevKeypoints;
            prevKeypoints = combinedKeypoints;
        }
    }

    // Manejar la ausencia de manos detectadas
    private void handleNoHandsDetected() {
        Log.e("MediaPipe", "No se detectaron manos");
        noHandDetectedCount++;

        if (noHandDetectedCount > NO_HAND_DETECTED_THRESHOLD) {
            secuenciaKeypointsUnaMano.clear();
            secuenciaKeypointsDosManos.clear();
            activity.runOnUiThread(() -> {
                if (resultTextView.getText().length() > 0) {

                    framesToSkip = 3;

                    char lastCharacter = resultTextView.getText().toString().charAt(resultTextView.getText().toString().length() - 1);
                    if (lastCharacter != ' ') {
                        resultTextView.append(" ");
                    }
                }
            });
            noHandDetectedCount = 0; // Resetear el contador después de agregar el espacio
        }
    }

    // Normalizar keypoints
    private List<Keypoint> normalizarKeypoints(List<Keypoint> keypoints) {
        float distanciaReferencia = (float) calcularDistancia(keypoints.get(0), keypoints.get(12));
        List<Keypoint> keypointsNormalizados = new ArrayList<>();

        for (Keypoint keypoint : keypoints) {
            Keypoint keypointNormalizado = new Keypoint(
                    keypoint.x / distanciaReferencia,
                    keypoint.y / distanciaReferencia,
                    keypoint.z / distanciaReferencia
            );
            keypointsNormalizados.add(keypointNormalizado);
        }

        return keypointsNormalizados;
    }

    // Verificar diferencia entre "U" y "V"
    private String verificarDiferenciaUV(List<Keypoint> keypoints) {
        keypoints = normalizarKeypoints(keypoints);

        double distancia_8_12 = calcularDistancia(keypoints.get(8), keypoints.get(12));
        double distancia_6_10 = calcularDistancia(keypoints.get(5), keypoints.get(9));

        double diferencia = Math.abs(distancia_8_12 - distancia_6_10);
        double umbral = 0.08; // Ajusta este valor según sea necesario
        System.out.println("el umbral es: " + diferencia);
        if (diferencia > umbral) {
            return "V";
        } else {
            return "U";
        }
    }

    // Verificar diferencia en el eje Z entre "T" y "F"
    private String verificarDiferenciaTF(List<Keypoint> keypoints) {
        double z_3 = keypoints.get(3).z;
        double z_4 = keypoints.get(4).z;
        double z_6 = keypoints.get(6).z;
        System.out.println("el valor de la coordenada es: " + z_3 + " - " + z_4 +" - "  + z_6);
        if (z_4 > z_6 || z_3> z_6) {
            return "F";
        } else {
            return "T";
        }
    }

    // Calcular la distancia entre dos keypoints
    private double calcularDistancia(Keypoint p1, Keypoint p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2) + Math.pow(p1.z - p2.z, 2));
    }

    // Realizar la inferencia y actualizar la UI
    private void doInference(Interpreter interpreter, String[] classNames, List<Keypoint> keypoints, int keypointsPerHand, List<List<Keypoint>> secuenciaKeypoints) {
        if (secuenciaKeypoints.size() < 20) {
            return;
        }

        ByteBuffer inputBuffer = convertToByteBuffer(secuenciaKeypoints, keypointsPerHand);
        float[][] output = new float[1][classNames.length];
        interpreter.run(inputBuffer, output);
        for (int i = 0; i < classNames.length; i++) {
            Log.d("Inferencia", "Clase " + classNames[i] + ": " + output[0][i]);
        }
        int predictedClass = argMax(output[0]);
        float confidence = output[0][predictedClass];

        String resultText = classNames[predictedClass];

        resultText = verificarResultadosEspeciales(resultText, keypoints, secuenciaKeypoints);
        secuenciaKeypointsUnaMano.clear();
        secuenciaKeypointsDosManos.clear();
        final String finalResultText = resultText;
        activity.runOnUiThread(() -> actualizarUI(confidence, finalResultText));
    }

    // Verificar resultados especiales para ciertas letras
    private String verificarResultadosEspeciales(String resultText, List<Keypoint> keypoints, List<List<Keypoint>> secuenciaKeypoints) {
        switch (resultText) {
            case "U":
            case "V":
                return verificarDiferenciaUV(keypoints);


            case "T":
            case "F":
                return verificarDiferenciaTF(keypoints);


            case "L":
            case "LL":
                return verificarVariacion3Frames(secuenciaKeypoints) ? "LL" : "L";

            case "N":
            case "NI":
                return verificarVariacion3Frames(secuenciaKeypoints) ? "Ñ" : "N";


            case "R":
            case "RR":
                return verificarVariacion3Frames(secuenciaKeypoints) ? "RR" : "R";

            default:
                return resultText;

        }

    }

    // Actualizar la UI con el resultado de la inferencia
    private void actualizarUI(float confidence, String finalResultText) {
        if (confidence > 0.60) {

            framesToSkip = 18;

            if (finalResultText != null && !finalResultText.isEmpty()) {
                if (resultTextView.getText().length() == 0) {
                    resultTextView.append(finalResultText);
                } else {
                    char lastCharacter = resultTextView.getText().toString().charAt(resultTextView.getText().toString().length() - 1);
                    String lastCharacterAsString = String.valueOf(lastCharacter);
                    if (!lastCharacterAsString.equalsIgnoreCase(finalResultText)) {
                        resultTextView.append(finalResultText.toLowerCase());
                    }
                }
            }
            secuenciaKeypointsUnaMano.clear();
            secuenciaKeypointsDosManos.clear();

        }
    }

    // Verificar variación en los últimos 3 frames
    private boolean verificarVariacion3Frames(List<List<Keypoint>> secuencia) {

        if (secuencia.size() < 3) {
            return false;
        }
        List<Keypoint> keypoints1 = secuencia.get(secuencia.size() - 1);
        List<Keypoint> keypoints2 = secuencia.get(secuencia.size() - 2);
        List<Keypoint> keypoints3 = secuencia.get(secuencia.size() - 3);

        double variacionTotal = 0;
        for (int i = 0; i < keypoints1.size(); i++) {
            double variacion1 = calcularDistancia(keypoints1.get(i), keypoints2.get(i));
            double variacion2 = calcularDistancia(keypoints2.get(i), keypoints3.get(i));
            variacionTotal += variacion1 + variacion2;
        }

        System.out.println("la variacion total es: " + variacionTotal);
        double umbral = 0.3; // Ajusta este valor según sea necesario
        return variacionTotal > umbral;
    }

    public void setHands(Context context, HandsOptions options) {
        this.hands = new Hands(context, options);
    }

    public Hands getHands() {
        return this.hands;
    }
}
