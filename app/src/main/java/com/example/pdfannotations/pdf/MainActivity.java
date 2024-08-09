package com.example.pdfannotations.pdf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pdfannotations.R;
import com.example.pdfannotations.databinding.ActivityMainBinding;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnRenderListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    PDDocument document;
    ImageView markerView;
    int mPageCount = 0;
    boolean isMarkerSet = true;
    List<MarkerModel> markers = new ArrayList<>();
    boolean isZoomable = true;

    Uri file_uri;
    ActivityResultLauncher<Intent> pdfLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Intent intent = result.getData();
                    assert intent != null;
                    Uri uri = intent.getData();
                    file_uri = uri;
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        document = PDDocument.load(inputStream);
                        mPageCount = document.getNumberOfPages();


                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //initialize Library
        PDFBoxResourceLoader.init(getApplicationContext());

        try {
            document = PDDocument.load(getAssets().open("timer.pdf"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mPageCount = document.getNumberOfPages();
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



//        Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
//        intent1.setType("application/pdf");
//        pdfLauncher.launch(intent1);
//        InputStream inputStream;
//        try {
//             inputStream = getContentResolver().openInputStream(file_uri);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }

        binding.pdfView.fromAsset("timer.pdf")
                .defaultPage(0)
                .onTap(new OnTapListener() {
                    @Override
                    public boolean onTap(MotionEvent event) {
                        if (isMarkerSet == true){
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN: {
                                    binding.buttons.setVisibility(View.VISIBLE);
                                    addMarker(event.getX(), event.getY());
//                                    isMarkerSet = false;
                                    return true;
                                }
                            }

                        }else {
                            Toast.makeText(MainActivity.this, "First set a Marker", Toast.LENGTH_SHORT).show();
                        }

                        return false;
                    }
                })
                .onDraw(new OnDrawListener() {
                    @Override
                    public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {


                    }
                }).onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {

                    }
                }).onRender(new OnRenderListener() {
                    @Override
                    public void onInitiallyRendered(int nbPages) {

                    }
                }).fitEachPage(true)
                .load();


        binding.btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    markers.add(new MarkerModel(markerView.getX(),markerView.getY()));

                    binding.pdfView.fromAsset("timer.pdf")
                            .onDraw(new OnDrawListener() {
                                @Override
                                public void onLayerDrawn(Canvas canvas, float pageWidth, float pageHeight, int displayedPage) {
                               for (MarkerModel model : markers){
                                   Paint paint = new Paint();
                                   Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_mark);
                                   canvas.drawBitmap(bitmap,model.getxCoordinate(),model.getyCoordinate(),paint);
                               }
                               markerView.setVisibility(View.GONE);
                                }
                            })
                            .onTap(new OnTapListener() {
                                @Override
                                public boolean onTap(MotionEvent event) {
                                    if (isMarkerSet == true){
                                        switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN: {
                                                binding.buttons.setVisibility(View.VISIBLE);
                                                addMarker(event.getX(), event.getY());
//                                    isMarkerSet = false;
                                                return true;
                                            }
                                        }

                                    }else {
                                        Toast.makeText(MainActivity.this, "First set a Marker", Toast.LENGTH_SHORT).show();
                                    }

                                    return false;
                                }
                            })
                            .load();

                    isMarkerSet = true;
                } catch (Exception e) {
                    Log.d("MAIN", e.toString());
                    Toast.makeText(MainActivity.this, "Try again! Marker is not set", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markerView.setVisibility(View.GONE);
                binding.buttons.setVisibility(View.GONE);
                isMarkerSet = true;

            }
        });

    }
    private void addMarker(float x, float y){
        markerView = new ImageView(this);
        markerView.setImageResource(R.drawable.ic_mark);
        markerView.setMaxHeight(40);
        markerView.setMaxWidth(40);
        markerView.setAdjustViewBounds(true);
        markerView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        markerView.setX(x);
        markerView.setY(y);

        markerView.setLayoutParams(params);
        binding.container.addView(markerView);

        enableMarkerDrag(markerView);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void enableMarkerDrag(final ImageView markerView) {
        markerView.setOnTouchListener(markerViewTouchListener);
    }

    View.OnTouchListener markerViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float dX = 0, dY = 0;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();

                    Log.d("MAIN", "dX: " + dX);
                    Log.d("MAIN", "dY: " + dY);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX;
                    float newY = event.getRawY() + dY;
                    v.setX(newX);
                    v.setY(newY);
                    //set value
                    return true;
//                case MotionEvent.ACTION_UP:
//                    try {
//                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mark);
//                        PDImageXObject setImage = JPEGFactory.createFromImage(document, bitmap);
//                        setImage.setWidth(40);
//                        setImage.setHeight(40);
//                        PDPage pdPage = document.getDocumentCatalog().getPages().get(0);
//                        PDPageContentStream contentStream = new PDPageContentStream(document, pdPage, PDPageContentStream.AppendMode.APPEND, true, true);
//
//                        contentStream.drawImage(setImage,v.getX(),binding.imageView.getHeight() - v.getY() -40);
//                        contentStream.close();
//                        v.setVisibility(View.GONE);
//                        renderPdf();
//                    }catch (Exception e){
//
//                    }
//                    return true;
            }


            return false;
        }
    };
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