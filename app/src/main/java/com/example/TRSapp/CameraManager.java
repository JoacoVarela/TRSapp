// Modulo para el manejo de la camara
package com.example.TRSapp;
import android.content.Context;
import android.util.Log;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class CameraManager {
    private ProcessCameraProvider cameraProvider;
    private ExecutorService executorService;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;
    private PreviewView previewView;
    private ImageAnalysis.Analyzer analyzer;

    public CameraManager(PreviewView previewView, ImageAnalysis.Analyzer analyzer, ExecutorService executorService) {
        this.previewView = previewView;
        this.analyzer = analyzer;
        this.executorService = executorService;
    }

    public void initializeCamera(Context context, PreviewView previewView, LifecycleOwner lifecycleOwner) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(lifecycleOwner);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraXApp", "Error al iniciar la cámara", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void bindCameraUseCases(LifecycleOwner lifecycleOwner) {
        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(this.executorService, this.analyzer);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
    }


    public void switchCamera(LifecycleOwner lifecycleOwner) {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT) ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
        bindCameraUseCases(lifecycleOwner);
    }
    public int getActiveCameraFacing() {
        return lensFacing;
    }

}