// Modulo para manejar la carga del modelo
package com.example.TRSapp;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ModelLoader {
    private Interpreter tflite;
    private Interpreter tflite1;

    public ModelLoader(Context context, String modelFileName, String modelFileName1) {
        loadModel(context, modelFileName);
        loadModelTwoHands(context, modelFileName1);
    }

    private void loadModel(Context context, String modelFileName) {
        try {
            AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            MappedByteBuffer tfliteModel = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            setTfLite(tfliteModel);
            System.out.println("Cargue el modelo");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadModelTwoHands(Context context, String modelFileName1) {
        try {
            AssetFileDescriptor fileDescriptor1 = context.getAssets().openFd(modelFileName1);
            FileInputStream inputStream1 = new FileInputStream(fileDescriptor1.getFileDescriptor());
            FileChannel fileChannel1 = inputStream1.getChannel();
            long startOffset1 = fileDescriptor1.getStartOffset();
            long declaredLength1 = fileDescriptor1.getDeclaredLength();
            MappedByteBuffer tfliteModel1 = fileChannel1.map(FileChannel.MapMode.READ_ONLY, startOffset1, declaredLength1);
            setTfLiteTwoHands(tfliteModel1);
            tflite1 = new Interpreter(tfliteModel1);
            System.out.println("Cargue el modelo1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Interpreter setTfLite(MappedByteBuffer tfliteModel) {
        return this.tflite = new Interpreter(tfliteModel);
    }

    public Interpreter getTfLite() {
        return this.tflite;
    }

    public Interpreter setTfLiteTwoHands(MappedByteBuffer tfliteModel1) {
        return this.tflite1 = new Interpreter(tfliteModel1);
    }

    public Interpreter getTfLiteTwoHands() {
        return this.tflite1;
    }


}
