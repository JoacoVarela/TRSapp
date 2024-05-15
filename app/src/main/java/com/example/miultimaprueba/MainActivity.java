package com.example.miultimaprueba;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.camera.view.PreviewView;

import org.tensorflow.lite.Interpreter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView resultTextView;
    private ImageButton backButton;
    private ImageButton rotateCameraButton;
    private ExecutorService executorService;
    private CameraManager cameraManager;
    private ModelLoader modelLoader;
    private ImageAnalyzer imageAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);
        rotateCameraButton = findViewById(R.id.rotateCameraButton);
        backButton = findViewById(R.id.backButton);
        modelLoader = new ModelLoader(this, "Modelo.tflite");
        initializeMediaPipe();
        imageAnalyzer = new ImageAnalyzer(modelLoader.getTfLite(), this, resultTextView);
        cameraManager = new CameraManager(previewView, imageAnalyzer::analyzeImage, executorService);
        initializeCamera();
        backButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HomeActivity.class)));
        rotateCameraButton.setOnClickListener(v -> cameraManager.switchCamera(this));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, HomeActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeMediaPipe() {
        HandsOptions options = HandsOptions.builder().setStaticImageMode(false).setMaxNumHands(1).build();
        imageAnalyzer.setHands(this, options);
        executorService = Executors.newSingleThreadExecutor();
    }

    private void initializeCamera() {
        cameraManager = new CameraManager(previewView, imageAnalyzer::analyzeImage,executorService);
        cameraManager.initializeCamera(this, previewView, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (modelLoader.getTfLite() != null) {
            modelLoader.getTfLite().close();
        }
    }
}
