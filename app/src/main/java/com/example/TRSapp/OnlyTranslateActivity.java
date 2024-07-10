// Modulo para manejar la traduccion en tiempo real desde otro dispositivo
package com.example.TRSapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OnlyTranslateActivity extends AppCompatActivity {

    private TextView translatedTextView;
    private DatabaseReference myRef;
    private ImageButton backButton;
    private Button cleanText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_only_translate); // Asegúrate de tener el layout correspondiente

        translatedTextView = findViewById(R.id.translatedTextView); // Asegúrate de que el ID coincida con el de tu layout
        backButton = findViewById(R.id.backButton);
        cleanText = findViewById(R.id.cleanButton);

        // Inicializa Firebase
        FirebaseApp.initializeApp(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://translationtextdb-default-rtdb.firebaseio.com/");
        myRef = database.getReference("translatedText");

        setupDatabaseListener();

        backButton.setOnClickListener(v -> startActivity(new Intent(OnlyTranslateActivity.this, HomeActivity.class)));
        cleanText.setOnClickListener(v -> clearDatabase());
    }

    // Método para limpiar la base de datos
    private void clearDatabase() {
        myRef.setValue(null)
                .addOnSuccessListener(aVoid -> {
                    Log.i("Firebase", "Datos limpiados exitosamente");
                    // Limpia el EditText
                    translatedTextView.setText("");
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Error al limpiar datos", e));
    }

    private void setupDatabaseListener() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Obtén el valor actualizado
                String translatedText = dataSnapshot.getValue(String.class);
                if (translatedText != null) {
                    translatedTextView.setText(translatedText); // Actualiza el EditText con el nuevo texto
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Manejo de errores
                Log.e("Firebase", "Error al leer datos", error.toException());
            }
        });
    }
}
