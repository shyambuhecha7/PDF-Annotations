package com.example.pdfannotations.pdf;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pdfannotations.R;
import com.example.pdfannotations.databinding.ActivitySampleBinding;
import com.github.barteksc.pdfviewer.listener.OnLongPressListener;

import java.util.ArrayList;
import java.util.List;

public class SampleActivity extends AppCompatActivity {

    ActivitySampleBinding binding;
    List<View> mMarkers = new ArrayList<>();
    ImageView markerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySampleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("MyPermissions", MODE_PRIVATE);
        boolean permissionRequested = prefs.getBoolean("isGiven", false);

        if (!permissionRequested) {
            requestPermission();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isGiven", true);
            editor.apply();
        } else {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }

        binding.pdfView.fromAsset("timer.pdf")
                .defaultPage(0)
                .onLongPress(new OnLongPressListener() {
                    @Override
                    public void onLongPress(MotionEvent e) {
                        Toast.makeText(SampleActivity.this, "" + e.getX() + " = " + e.getY(), Toast.LENGTH_SHORT).show();
                    }
                })
                .load();


        ImageView imageView = new ImageView(this);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mark);
        imageView.setImageBitmap(bitmap);


    }

    private void requestPermission() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", this.getPackageName(), null);
        intent.setData(uri);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

}