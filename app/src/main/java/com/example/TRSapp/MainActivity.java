// Modulo para manejar la vista de traduccion
package com.example.TRSapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1002;
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
    private int activeCamera;
    private Button cleanLastCharacter;
    private Button cleanText;
    private TextToSpeech textToSpeech;
    private ImageButton speakButton;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getElements();
        initializeUI();
        initializeTextToSpeech();
        initializeFirebase();
        initializeCameraManager();
        setupButtonListeners();
        setupUI(mainLayout);
    }

    private void getElements() {
        previewView = findViewById(R.id.previewView);
        resultTextView = findViewById(R.id.resultTextView);
        rotateCameraButton = findViewById(R.id.rotateCameraButton);
        backButton = findViewById(R.id.backButton);
        cleanText = findViewById(R.id.cleanButton);
        cleanLastCharacter = findViewById(R.id.cleanLastCharacter);
        mainLayout = findViewById(R.id.mainLayout);
        speakButton = findViewById(R.id.speakButton);
        saveButton = findViewById(R.id.saveButton);
    }

    private void initializeUI() {
        resultTextView.setHorizontallyScrolling(false);
        resultTextView.setSingleLine(false);
        resultTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        resultTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Verifica que los elementos de la UI no sean nulos
        if (previewView == null || resultTextView == null || rotateCameraButton == null || backButton == null) {
            throw new RuntimeException("Error: uno o más elementos de la UI no se han inicializado correctamente.");
        }

        TextConfig textConfig = getIntent().getParcelableExtra("textConfig");
        if (textConfig != null) {
            textConfig.applyConfig(resultTextView);
        }
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("es", "ES"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                } else {
                    speakButton.setEnabled(true);
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });
    }

    private void initializeFirebase() {
        FirebaseApp.initializeApp(this);
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://translationtextdb-default-rtdb.firebaseio.com/");
        myRef = database.getReference("translatedText");
        setupEditTextListener();
        setupDatabaseListener();
    }

    private void initializeCameraManager() {
        // Inicializa el modelo y obtén el intérprete
        modelLoader = new ModelLoader(this, "Modelo.tflite", "ModeloTwoHands.tflite");
        Interpreter tflite = modelLoader.getTfLite();
        Interpreter tflite1 = modelLoader.getTfLiteTwoHands();
        if (tflite == null) {
            throw new RuntimeException("Error: el modelo TFLite no se ha cargado correctamente.");
        }
        imageAnalyzer = new ImageAnalyzer(tflite, tflite1, this, resultTextView);
        initializeMediaPipe();
        executorService = Executors.newSingleThreadExecutor();
        cameraManager = new CameraManager(previewView, imageAnalyzer::analyzeImage, executorService);

        // Solicita permisos de cámara si no están concedidos
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    CAMERA_PERMISSION_REQUEST_CODE);
        }
        else {
            initializeCamera();
        }
    }

    private void setupButtonListeners() {
        speakButton.setOnClickListener(v -> {
            String text = resultTextView.getText().toString();
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        });

        saveButton.setOnClickListener(v -> saveTextToFile(resultTextView.getText().toString()));

        cleanLastCharacter.setOnClickListener(v -> {
            String currentText = resultTextView.getText().toString();
            if (currentText.length() > 0) {
                String newText = currentText.substring(0, currentText.length() - 1);
                resultTextView.setText(newText);
                myRef.setValue(newText)
                        .addOnSuccessListener(aVoid -> Log.i("Firebase", "Texto actualizado exitosamente"))
                        .addOnFailureListener(e -> Log.e("Firebase", "Error al actualizar texto", e));
            }
        });

        cleanText.setOnClickListener(v -> clearDatabase());

        backButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HomeActivity.class)));
        rotateCameraButton.setOnClickListener(v -> {
            cameraManager.switchCamera(this);
            previewView.postDelayed(this::updateCameraState, 500);
        });
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

    private void clearDatabase() {
        myRef.setValue(null)
                .addOnSuccessListener(aVoid -> {
                    Log.i("Firebase", "Datos limpiados exitosamente");
                    // Limpia el EditText
                    resultTextView.setText("");
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error al limpiar datos", e));
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
                showCustomToast( "Cámara denegada. La aplicación no puede funcionar sin este permiso.");
            }
        }
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCustomToast("Permiso de almacenamiento concedido");
            } else {
                showCustomToast("Permiso de almacenamiento denegado");
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
        HandsOptions options = HandsOptions.builder()
                .setMaxNumHands(2)
                .setMinDetectionConfidence(0.5f) //saber si la mano es considerada valida
                .setMinTrackingConfidence(0.5f)
                .setModelComplexity(1)
                .build();
        imageAnalyzer.setHands(this, options);
    }

    private void initializeCamera() {
        cameraManager.initializeCamera(this, previewView, this);
        updateCameraState();
    }

    private void updateCameraState() {
        int newActiveCamera = cameraManager.getActiveCameraFacing();
        if (activeCamera != newActiveCamera) {
            activeCamera = newActiveCamera;
            imageAnalyzer.setActiveCamera(activeCamera);
            Log.d("MainActivity", "Active camera updated to: " + activeCamera);
        }
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


    private void saveTextToFile(String text) {
        if (text.isEmpty()) {
            showCustomToast("El campo de texto está vacío. No se guardó ningún archivo.");
            return; // No proceder con el guardado si el texto está vacío
        }
        if (isExternalStorageWritable()) {
            File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TRS");

            // Crea la carpeta TRS si no existe
            if (!path.exists()) {
                path.mkdirs();
            }

            // Determina el nombre del archivo secuencialmente
            int fileIndex = 1;
            File file;
            do {
                file = new File(path, "conversacion_" + fileIndex + ".txt");
                fileIndex++;
            } while (file.exists());

            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(text.getBytes());
                fos.close();
                showCustomToast("Texto guardado en " + file.getAbsolutePath());
            } catch (IOException e) {
                Log.e("MainActivity", "Error al guardar texto en archivo", e);
                showCustomToast("Error al guardar texto");
            }
        } else {
            showCustomToast("El almacenamiento externo no está disponible");
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast, findViewById(R.id.customToastText));

        TextView textView = layout.findViewById(R.id.customToastText);
        textView.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
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
        if (modelLoader.getTfLiteTwoHands() != null) {
            modelLoader.getTfLiteTwoHands().close();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
