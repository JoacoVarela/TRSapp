package com.example.TRSapp;

import android.app.Activity;
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
    private Interpreter tfliteClasificador; // Añadir esta línea

    public ModelLoader(Context context, String modelFileName, String modelFileName1, String modelFileNameClasificador) {
        loadModel(context, modelFileName);
        loadModel1(context, modelFileName1);
        loadModelClasificador(context, modelFileNameClasificador); // Añadir esta línea
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

    private void loadModel1(Context context, String modelFileName1) {
        try {
            AssetFileDescriptor fileDescriptor1 = context.getAssets().openFd(modelFileName1);
            FileInputStream inputStream1 = new FileInputStream(fileDescriptor1.getFileDescriptor());
            FileChannel fileChannel1 = inputStream1.getChannel();
            long startOffset1 = fileDescriptor1.getStartOffset();
            long declaredLength1 = fileDescriptor1.getDeclaredLength();
            MappedByteBuffer tfliteModel1 = fileChannel1.map(FileChannel.MapMode.READ_ONLY, startOffset1, declaredLength1);
            setTfLite1(tfliteModel1);
            tflite1 = new Interpreter(tfliteModel1);
            System.out.println("Cargue el modelo1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Añadir este método para cargar el clasificador
    private void loadModelClasificador(Context context, String modelFileNameClasificador) {
        try {
            AssetFileDescriptor fileDescriptorClasificador = context.getAssets().openFd(modelFileNameClasificador);
            FileInputStream inputStreamClasificador = new FileInputStream(fileDescriptorClasificador.getFileDescriptor());
            FileChannel fileChannelClasificador = inputStreamClasificador.getChannel();
            long startOffsetClasificador = fileDescriptorClasificador.getStartOffset();
            long declaredLengthClasificador = fileDescriptorClasificador.getDeclaredLength();
            MappedByteBuffer tfliteModelClasificador = fileChannelClasificador.map(FileChannel.MapMode.READ_ONLY, startOffsetClasificador, declaredLengthClasificador);
            setTfLiteClasificador(tfliteModelClasificador);
            tfliteClasificador = new Interpreter(tfliteModelClasificador); // Añadir esta línea
            System.out.println("Cargue el modeloClasificador");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Interpreter setTfLite(MappedByteBuffer tfliteModel) {
        return this.tflite = new Interpreter(tfliteModel);
    }

    public Interpreter getTfLite() {
        return this.tflite;
    }

    public Interpreter setTfLite1(MappedByteBuffer tfliteModel1) {
        return this.tflite1 = new Interpreter(tfliteModel1);
    }

    public Interpreter getTfLite1() {
        return this.tflite1;
    }

    public Interpreter setTfLiteClasificador(MappedByteBuffer tfliteModelClasificador) {
        return this.tfliteClasificador = new Interpreter(tfliteModelClasificador);
    }

    public Interpreter getTfLiteClasificador() {
        return this.tfliteClasificador;
    }
}
