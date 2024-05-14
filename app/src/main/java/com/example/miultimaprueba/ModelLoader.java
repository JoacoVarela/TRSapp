package com.example.miultimaprueba;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ModelLoader {
    private Interpreter tflite;

    public ModelLoader(Context context, String modelFileName, Interpreter tflite) {
        this.tflite = tflite;
        loadModel(context, modelFileName);
    }

    private void loadModel(Context context, String modelFileName) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            this.tflite = new Interpreter(tfliteModel);
            System.out.println("Cargue el modelo");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

