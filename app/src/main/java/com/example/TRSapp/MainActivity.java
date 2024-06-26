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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
    private DatabaseReference myRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa los elementos de la UI
        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);
        resultTextView.setHorizontallyScrolling(false);
        resultTextView.setSingleLine(false);
        resultTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        resultTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        TextConfig textConfig = getIntent().getParcelableExtra("textConfig");


        if (textConfig != null) {
            textConfig.applyConfig(resultTextView);
        }
        rotateCameraButton = findViewById(R.id.rotateCameraButton);
        backButton = findViewById(R.id.backButton);

        // Verifica que los elementos de la UI no sean nulos
        if (previewView == null || resultTextView == null || rotateCameraButton == null || backButton == null) {
            throw new RuntimeException("Error: uno o más elementos de la UI no se han inicializado correctamente.");
        }

        // Inicializa el modelo y obtén el intérprete
        modelLoader = new ModelLoader(this, "Modelo.tflite", "Modelo1.tflite", "clasificador_gestos.tflite");
        Interpreter tflite = modelLoader.getTfLite();
        Interpreter tflite1 = modelLoader.getTfLite1();
        Interpreter clasificador = modelLoader.getTfLiteClasificador();
        if (tflite == null) {
            throw new RuntimeException("Error: el modelo TFLite no se ha cargado correctamente.");
        }
        imageAnalyzer = new ImageAnalyzer(tflite, tflite1, this, resultTextView);
        initializeMediaPipe();
        executorService = Executors.newSingleThreadExecutor(); // Asegúrate de inicializar esto antes de usarlo
        cameraManager = new CameraManager(previewView, imageAnalyzer::analyzeImage, executorService);

        // Solicita permisos de cámara si no están concedidos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }

        backButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HomeActivity.class)));
        rotateCameraButton.setOnClickListener(v -> cameraManager.switchCamera(this));

        mainLayout = findViewById(R.id.mainLayout);
        setupUI(mainLayout);

        FirebaseApp.initializeApp(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://translationtextdb-default-rtdb.firebaseio.com/");
        myRef = database.getReference("translatedText");
        setupEditTextListener();
        setupDatabaseListener();
    }

    private void setupUI(View view) {
        // Configura el listener de toque para ocultar el teclado
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideSoftKeyboard();
                resultTextView.clearFocus();
                return false;
            });
        }

        // Si es un contenedor de layout, itera sobre los hijos y aplica recursivamente
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

    @Override
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
        cameraManager = new CameraManager(previewView, imageAnalyzer::analyzeImage, executorService);
        cameraManager.initializeCamera(this, previewView, this);
    }

    private void setupEditTextListener() {
        resultTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No necesitas hacer nada aquí
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Envía el texto a Firebase en tiempo real
                sendTranslatedText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No necesitas hacer nada aquí
            }
        });
    }

    private void sendTranslatedText(String text) {
        myRef.setValue(text);
    }

    private void setupDatabaseListener() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Obtén el valor actualizado
                String translatedText = dataSnapshot.getValue(String.class);
                System.out.println("TRANSLATEDTEXT>>>>>>>" +translatedText);
                // Actualiza el TextView con el nuevo texto
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Manejo de errores
                Log.e("Firebase", "Error al leer datos", error.toException());
            }
        });
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
        if (modelLoader.getTfLite1() != null) {
            modelLoader.getTfLite1().close();
        }
    }
}
