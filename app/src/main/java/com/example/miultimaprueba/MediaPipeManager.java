package com.example.miultimaprueba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mediapipe.solutions.hands.Hands;
import com.google.mediapipe.solutions.hands.HandsOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPipeManager {
    private Hands hands;
    private ExecutorService executorService;


    public MediaPipeManager(Context context, ImageAnalysis.Analyzer analyzer, ExecutorService executorService, Hands hands) {
        this.hands = hands;
        this.executorService = executorService;
        initializeMediaPipe(context, analyzer);

    }

    private void initializeMediaPipe(Context context, ImageAnalysis.Analyzer analyzer) {
        HandsOptions options = HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(1)
                .build();
        hands = new Hands(context, options);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    private Bitmap convertImageProxyToBitmap(ImageProxy image) {
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, originalBitmap.getWidth() / 1, originalBitmap.getHeight() / 1, true);

        Matrix matrix = new Matrix();
        matrix.postRotate(-90);
        matrix.postScale(-1, 1);
        return Bitmap.createBitmap(resizedBitmap, 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);
    }
}
