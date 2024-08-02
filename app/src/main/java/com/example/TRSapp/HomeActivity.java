// Modulo para manejar la vista de inicio
package com.example.TRSapp;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class HomeActivity extends AppCompatActivity {
    private static final int TEXT_CONFIG_REQUEST_CODE = 1;
    private TextConfig textConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        textConfig = new TextConfig();

        Button iniciarTraduccionButton = findViewById(R.id.iniciarTraduccionButton);
        iniciarTraduccionButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.putExtra("textConfig", textConfig);
            startActivity(intent);
        });

        Button iniciarLecturaTraduccionButton = findViewById(R.id.iniciarLecturaButton);
        iniciarLecturaTraduccionButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, OnlyTranslateActivity.class);
            startActivity(intent);
        });


        ImageButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TextConfigActivity.class);
            intent.putExtra("textConfig", textConfig);
            startActivityForResult(intent, TEXT_CONFIG_REQUEST_CODE);
        });

        ImageButton questionButton = findViewById(R.id.userManual);
        questionButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, UserManualActivity.class);
           startActivity(intent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TEXT_CONFIG_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            textConfig = data.getParcelableExtra("textConfig");
        }
    }
}
