package com.example.TRSapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.view.View;

import com.google.mediapipe.solutions.hands.HandsOptions;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private PreviewView previewView;
    private EditText resultTextView;
    private ConstraintLayout mainLayout;
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

        // Inicializa los elementos de la UI
        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);
        rotateCameraButton = findViewById(R.id.rotateCameraButton);
        backButton = findViewById(R.id.backButton);

        // Verifica que los elementos de la UI no sean nulos
        if (previewView == null || resultTextView == null || rotateCameraButton == null || backButton == null) {
            throw new RuntimeException("Error: uno o más elementos de la UI no se han inicializado correctamente.");
        }

        // Inicializa el modelo y obtén el intérprete
        modelLoader = new ModelLoader(this, "Modelo.tflite");
         Interpreter tflite = modelLoader.getTfLite();
        if (tflite == null) {
            throw new RuntimeException("Error: el modelo TFLite no se ha cargado correctamente.");
        }
        imageAnalyzer = new ImageAnalyzer(tflite, this, resultTextView);
        initializeMediaPipe();
        executorService = Executors.newSingleThreadExecutor(); // Asegúrate de inicializar esto antes de usarlo
        cameraManager = new CameraManager(previewView, imageAnalyzer::analyzeImage, executorService);

        // Solicita permisos de cámara si no están concedidos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }

        resultTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
        resultTextView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        backButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HomeActivity.class)));
        rotateCameraButton.setOnClickListener(v -> cameraManager.switchCamera(this));

        mainLayout = findViewById(R.id.mainLayout);

        setupUI(mainLayout);
    }


    private void setupUI(View view) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideSoftKeyboard();
                resultTextView.clearFocus();
                return false;
            });
        }

        // If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, inicializa la cámara
                initializeCamera();
            } else {
                // Permiso denegado, muestra un mensaje
                Toast.makeText(this, "Cámara denegada. La aplicación no puede funcionar sin este permiso.", Toast.LENGTH_LONG).show();
            }
        }
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
