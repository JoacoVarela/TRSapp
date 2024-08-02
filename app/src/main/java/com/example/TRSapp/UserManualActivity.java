// Modulo encargado de cargar la data del manual de usuario
package com.example.TRSapp;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class UserManualActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manual);
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> startActivity(new Intent(UserManualActivity.this, HomeActivity.class)));

        TextView textViewPrincipalFunction = findViewById(R.id.funcionPrincipal);
        String textViewPrincipalFunctionText = "<b>Función principal</b>: El traductor permite al usuario generar texto/audio a partir de una seña capturada por la cámara de un dispositivo.";
        textViewPrincipalFunction.setText(Html.fromHtml(textViewPrincipalFunctionText));

        TextView textViewInit = findViewById(R.id.vistaInicioText);
        String textViewInitText = "- <b>Inicio</b>: es la vista de bienvenida a la app.";
        textViewInit.setText(Html.fromHtml(textViewInitText));

        TextView textViewConfig = findViewById(R.id.configText);
        String textViewCofigText = "- <b>Configuración del texto</b>: permite al usuario parametrizar tres propiedades del texto que genera la traducción (Tamaño, Color y Fuente)";
        textViewConfig.setText(Html.fromHtml(textViewCofigText));

        TextView textViewUserManual = findViewById(R.id.userManualText);
        String textViewUserManualText = "- <b>Manual de usuario</b>: Detalle sobre el funcionamiento de la aplicación.";
        textViewUserManual.setText(Html.fromHtml(textViewUserManualText));

        TextView textViewIniciarTraduccion = findViewById(R.id.iniciarTraduccion);
        String textViewIniciarTraduccionText = "- <b>Iniciar traducción</b>: genera la traducción de las señas realizadas por el usuario mediante texto/audio.";
        textViewIniciarTraduccion.setText(Html.fromHtml(textViewIniciarTraduccionText));

        TextView textViewIniciarLectura = findViewById(R.id.iniciarLecturaTxt);
        String textViewIniciarLecturaText = "- <b>Iniciar Lectura</b>: Permite visualizar la traducción generada por la app en tiempo real desde otro dispositivo.";
        textViewIniciarLectura.setText(Html.fromHtml(textViewIniciarLecturaText));





    }
}
