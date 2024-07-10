// Modulo que maneja la vista de configuracion del texto
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
        setupColorSpinner();
        setupSizeSpinner();
        setupTypefaceSpinner();
    }

    private void setupColorSpinner() {
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.color_options, R.layout.spinner_item);
        colorAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);

        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedColor = (String) parent.getItemAtPosition(position);
                int color = getColorFromString(selectedColor);
                textConfig.setColor(color);
                textConfig.applyConfig(previewTextView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int getColorFromString(String colorName) {
        switch (colorName) {
            case "Negro":
                return Color.BLACK;
            case "Rojo":
                return Color.RED;
            case "Azul":
                return Color.BLUE;
            case "Blanco":
                return Color.WHITE;
            case "Verde":
                return Color.GREEN;
            case "Gris":
                return Color.GRAY;
            default:
                return Color.BLACK;
        }
    }

    private void setupSizeSpinner() {
        ArrayAdapter<CharSequence> sizeAdapter = ArrayAdapter.createFromResource(this,
                R.array.size_options, R.layout.spinner_item);
        sizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeAdapter);

        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSize = (String) parent.getItemAtPosition(position);
                float size = getSizeFromString(selectedSize);
                textConfig.setSize(size);
                textConfig.applyConfig(previewTextView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private float getSizeFromString(String sizeName) {
        switch (sizeName) {
            case "14sp":
                return 14f;
            case "16sp":
                return 16f;
            case "18sp":
                return 18f;
            case "22sp":
                return 22f;
            case "26sp":
                return 26f;
            case "30sp":
                return 30f;
            default:
                return 18f;
        }
    }

    private void setupTypefaceSpinner() {
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
        if (textConfig != null) {
            textConfig.applyConfig(previewTextView);
            initializeColorSpinner(textConfig.getColor());
            initializeSizeSpinner(textConfig.getSize());
            initializeTypefaceSpinner(textConfig.getTypefaceName());
        }
    }

    private void initializeColorSpinner(int color) {
        switch (color) {
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
    }

    private void initializeSizeSpinner(float size) {
        if (size == 14f) {
            sizeSpinner.setSelection(0);
        } else if (size == 16f) {
            sizeSpinner.setSelection(1);
        } else if (size == 18f) {
            sizeSpinner.setSelection(2);
        } else if (size == 22f) {
            sizeSpinner.setSelection(3);
        } else if (size == 26f) {
            sizeSpinner.setSelection(4);
        } else if (size == 30f) {
            sizeSpinner.setSelection(5);
        }
    }

    private void initializeTypefaceSpinner(String typefaceName) {
        switch (typefaceName) {
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
