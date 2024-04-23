package com.example.miultimaprueba;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import org.tensorflow.lite.Interpreter;

import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import android.content.res.AssetFileDescriptor;
import java.io.FileInputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsResult;
import com.google.mediapipe.solutions.hands.HandsOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private PreviewView previewView;
    private Hands hands;
    private ExecutorService mediaPipeExecutor;
    private Interpreter tflite;
    // Lista para almacenar las secuencias de keypoints
    private List<List<Keypoint>> secuenciaKeypoints = new ArrayList<>();
    private TextView resultTextView; // Agregado para mostrar resultados

    // Nombres de las clases
    private static final String[] CLASS_NAMES = {"A", "B", "C", "D", "E", "F", "I", "K", "L", "M", "N", "O", "P", "R", "T", "U","V", "W" };
    private ImageView debugImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Inicializar vistas
        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView); // Referencia al TextView
        debugImageView = findViewById(R.id.debugImageView);
        initializeMediaPipe();
        initializeCamera();
        loadModel(); // Cargar el modelo TensorFlow Lite
    }
    private void loadModel() {
        try {
            AssetFileDescriptor fileDescriptor = this.getAssets().openFd("Modelo.tflite");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            tflite = new Interpreter(tfliteModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void initializeMediaPipe() {
        hands = new Hands(this, HandsOptions.builder().setStaticImageMode(false).setMaxNumHands(1).build());
        mediaPipeExecutor = Executors.newSingleThreadExecutor();
    }

    private void initializeCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraXApp", "Error al iniciar la cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(mediaPipeExecutor, this::analyzeImage);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        // Convertir ImageProxy a Bitmap
        Bitmap bitmap = convertImageProxyToBitmap(image);
        if (bitmap != null) {
            long timestamp = image.getImageInfo().getTimestamp();
            hands.send(bitmap, timestamp);
            hands.setResultListener(handsResult -> {
                List<Keypoint> keypoints = extractKeypoints(handsResult);
                if (keypoints != null && !keypoints.isEmpty()) {
                    // Asegurarse de que la secuencia de keypoints tenga el tamaño adecuado
                    if (secuenciaKeypoints.size() >= 20) {
                        secuenciaKeypoints.remove(0);
                    }
                    secuenciaKeypoints.add(keypoints);

                    if (secuenciaKeypoints.size() == 20) {
                        // Preparar inputBuffer para la inferencia
                        ByteBuffer inputBuffer = convertToByteBuffer(secuenciaKeypoints);

                        // Ejecutar la inferencia con el modelo TensorFlow Lite
                        float[][] output = new float[1][CLASS_NAMES.length];
                        tflite.run(inputBuffer, output);

                        // Mostrar los resultados
                        int predictedClass = argMax(output[0]);
                        float confidence = output[0][predictedClass];
                        runOnUiThread(() -> {
                            debugImageView.setImageBitmap(bitmap);
                            if (confidence > 0.7) {  // Comprobar si la confianza es mayor al 90%
                                String resultText = CLASS_NAMES[predictedClass] + " - Confianza: " + confidence;
                                resultTextView.setText(resultText);
                            } else {
                                resultTextView.setText(""); // Limpiar el texto si la confianza es baja
                            }
                        });
                    }
                }
            });
        }
        image.close();
    }

    private void processOutput(float[][] output) {
        // Asumiendo que output es un array 2D donde output[0] contiene las probabilidades de cada clase
        for (int i = 0; i < output[0].length; i++) {
            Log.d("Inferencia", "Clase " + i + ": " + output[0][i] * 100 + "%");
        }

        // Encuentra el índice de la clase con la probabilidad más alta
        int maxIndex = argMax(output[0]);
        Log.d("Inferencia", "Clase predicha: " + maxIndex + " con una confianza de " + output[0][maxIndex] * 100 + "%");
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
    private ByteBuffer convertToByteBuffer(List<List<Keypoint>> secuencia) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * 20 * 63 * 4); // Para float utiliza 4 bytes
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
    private float[][] keypointsToArray(List<Keypoint> keypoints) {
        float[][] array = new float[1][63]; // 21 keypoints * 3 valores (x, y, z)
        int index = 0;
        for (Keypoint point : keypoints) {
            array[0][index++] = point.x;
            array[0][index++] = point.y;
            array[0][index++] = point.z;
        }
        return array;
    }
    public class Keypoint {
        public float x;
        public float y;
        public float z;

        public Keypoint(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }


    private List<Keypoint> extractKeypoints(HandsResult handsResult) {
        if (handsResult == null || handsResult.multiHandLandmarks().isEmpty()) {
            Log.e("MediaPipe", "No hands detected");
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

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        // Reducir el tamaño del bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, originalBitmap.getWidth() / 1, originalBitmap.getHeight() / 1, true);

        // Rotar la imagen
        Matrix matrix = new Matrix();
        matrix.postRotate(-90);
        matrix.postScale(-1, 1);
        return Bitmap.createBitmap(resizedBitmap, 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPipeExecutor != null && !mediaPipeExecutor.isShutdown()) {
            mediaPipeExecutor.shutdown();
        }
        if (tflite != null) {
            tflite.close();
        }

    }

}