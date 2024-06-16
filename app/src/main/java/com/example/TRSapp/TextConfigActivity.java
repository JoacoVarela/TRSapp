package com.example.TRSapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TextConfigActivity extends AppCompatActivity {
    private TextView previewTextView;
    private Spinner colorSpinner;
    private Spinner sizeSpinner;
    private Spinner typefaceSpinner;
    private TextConfig textConfig;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);

        previewTextView = findViewById(R.id.previewTextView);
        colorSpinner = findViewById(R.id.colorSpinner);
        sizeSpinner = findViewById(R.id.sizeSpinner);
        typefaceSpinner = findViewById(R.id.typefaceSpinner);
        backButton = findViewById(R.id.backButton);

        textConfig = getIntent().getParcelableExtra("textConfig");
        backButton.setOnClickListener(v -> startActivity(new Intent(TextConfigActivity.this, HomeActivity.class)));


        setupSpinners();
        setupInitialConfig();

        findViewById(R.id.saveButton).setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("textConfig", textConfig);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void setupSpinners() {
        // Configurar el Spinner de colores
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.color_options, R.layout.spinner_item);
        colorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);

        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedColor = (String) parent.getItemAtPosition(position);
                int color = Color.BLACK;
                switch (selectedColor) {
                    case "Negro":
                        color = Color.BLACK;
                        break;
                    case "Rojo":
                        color = Color.RED;
                        break;
                    case "Azul":
                        color = Color.BLUE;
                        break;
                    case "Blanco":
                        color = Color.WHITE;
                        break;
                    case "Verde":
                        color = Color.GREEN;
                        break;
                    case "Gris":
                        color = Color.GRAY;
                        break;
                    default:
                        color = Color.BLACK;
                        break;
                }

                textConfig.setColor(color);
                textConfig.applyConfig(previewTextView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Configurar el Spinner de tamaños
        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(this,
                R.array.size_options, R.layout.spinner_item);
        sizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeAdapter);

        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSize = (String) parent.getItemAtPosition(position);
                System.out.println("slectedSize: " + selectedSize);
                float size = 14f;
                switch (selectedSize) {
                    case "14sp":
                        size = 14f;
                        break;
                    case "16sp":
                        size = 16f;
                        break;
                    case "18sp":
                        size = 18f;
                        break;
                    case "22sp":
                        size = 22f;
                        break;
                    case "26sp":
                        size = 26f;
                        break;
                    case "30sp":
                        size = 30f;
                        break;
                    default:
                        size = 18f;
                        break;
                }
                textConfig.setSize(size);
                textConfig.applyConfig(previewTextView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Configurar el Spinner de tipos de letra
        ArrayAdapter<CharSequence> typefaceAdapter = ArrayAdapter.createFromResource(this,
                R.array.typeface_options, R.layout.spinner_item);
        typefaceAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        typefaceSpinner.setAdapter(typefaceAdapter);

        typefaceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTypeface = (String) parent.getItemAtPosition(position);
                textConfig.setTypefaceName(selectedTypeface.toLowerCase());
                textConfig.applyConfig(previewTextView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupInitialConfig() {
        // Aplicar la configuración inicial a la vista previa
        if (textConfig != null) {
            textConfig.applyConfig(previewTextView);

            // Inicializar los spinners con los valores actuales
            switch (textConfig.getColor()) {
                case Color.BLACK:
                    colorSpinner.setSelection(0);
                    break;
                case Color.RED:
                    colorSpinner.setSelection(1);
                    break;
                case Color.BLUE:
                    colorSpinner.setSelection(2);
                    break;
                case Color.WHITE:
                    colorSpinner.setSelection(3);
                    break;
                case Color.GREEN:
                    colorSpinner.setSelection(4);
                    break;
                case Color.GRAY:
                    colorSpinner.setSelection(5);
                    break;
            }

            if (textConfig.getSize() == 14f) {
                sizeSpinner.setSelection(0);
            } else if (textConfig.getSize() == 16f) {
                sizeSpinner.setSelection(1);
            } else if (textConfig.getSize() == 18f) {
                sizeSpinner.setSelection(2);
            } else if(textConfig.getSize()==22f){
                sizeSpinner.setSelection(3);
            } else if (textConfig.getSize()==26f) {
                sizeSpinner.setSelection(4);
            } else if (textConfig.getSize()==30f) {
                sizeSpinner.setSelection(5);
            }

            // Comparar nombres de las fuentes
            switch (textConfig.getTypefaceName()) {
                case "sans":
                    typefaceSpinner.setSelection(0);
                    break;
                case "serif":
                    typefaceSpinner.setSelection(1);
                    break;
                case "monospace":
                    typefaceSpinner.setSelection(2);
                    break;
            }
        }
    }
}
