// Modulo que maneja la configuracuion del texto
package com.example.TRSapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.TextView;

public class TextConfig implements Parcelable {
    private int color;
    private float size;
    private String typefaceName;

    public TextConfig() {
        // Valores predeterminados
        this.color = Color.BLACK;
        this.size = 16f;
        this.typefaceName = "sans"; // Por defecto sans-serif
    }

    protected TextConfig(Parcel in) {
        color = in.readInt();
        size = in.readFloat();
        typefaceName = in.readString();
    }

    public static final Creator<TextConfig> CREATOR = new Creator<TextConfig>() {
        @Override
        public TextConfig createFromParcel(Parcel in) {
            return new TextConfig(in);
        }

        @Override
        public TextConfig[] newArray(int size) {
            return new TextConfig[size];
        }
    };

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public String getTypefaceName() {
        return typefaceName;
    }

    public void setTypefaceName(String typefaceName) {
        this.typefaceName = typefaceName;
    }

    public Typeface getTypeface() {
        switch (typefaceName) {
            case "serif":
                return Typeface.SERIF;
            case "monospace":
                return Typeface.MONOSPACE;
            case "sans":
            default:
                return Typeface.SANS_SERIF;
        }
    }

    public void applyConfig(TextView textView) {
        textView.setTextColor(color);
        textView.setTextSize(size);
        textView.setTypeface(getTypeface());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(color);
        dest.writeFloat(size);
        dest.writeString(typefaceName);
    }
}
