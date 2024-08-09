package com.example.pdfannotations.pdf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pdfannotations.R;
import com.example.pdfannotations.databinding.ActivityNewBinding;
import com.example.pdfannotations.databinding.ActivityPdfBinding;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnRenderListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class NewActivity extends AppCompatActivity {

    ActivityNewBinding binding;
    int mPageCount = 0;
    int currentPageNum = 0;
    ImageView markerView;
    PDDocument document;
    Uri fileUri = null;
    boolean isMarkerSet = true;
    ActivityResultLauncher<Intent> pdfLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Intent intent = result.getData();
                    Uri uri = intent.getData();

                    fileUri = uri;
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        document = PDDocument.load(inputStream);
                        mPageCount = document.getNumberOfPages();

                        renderPdf(currentPageNum);


                        binding.btnOpenPdf.setVisibility(View.GONE);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNewBinding.inflate(getLayoutInflater());
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

        //initialize Library
        PDFBoxResourceLoader.init(getApplicationContext());

        try {
            document = PDDocument.load(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"new.pdf"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        binding.btnOpenPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(Intent.ACTION_GET_CONTENT);
                intent1.setType("application/pdf");
                pdfLauncher.launch(intent1);
            }
        });


        binding.pdfView.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"new.pdf"))
                .defaultPage(0)
                .pages(0)
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
                            Toast.makeText(NewActivity.this, "First set a Marker", Toast.LENGTH_SHORT).show();
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
                    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_mark);
                    PDImageXObject setImage = JPEGFactory.createFromImage(document, bitmap);
                    setImage.setWidth(40);
                    setImage.setHeight(40);

                    PDPage pdPage = document.getDocumentCatalog().getPages().get(currentPageNum);
                    PDPageContentStream contentStream = new PDPageContentStream(document, pdPage, PDPageContentStream.AppendMode.APPEND, true, true);
                    contentStream.drawImage(setImage, markerView.getX() -40,binding.pdfView.getHeight() - markerView.getY());
                    contentStream.saveGraphicsState();
                    contentStream.close();
                    //Save PDF in Downloads
                    document.save(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "new.pdf"));

                    markerView.setVisibility(View.GONE);
                    binding.buttons.setVisibility(View.GONE);
                    Toast.makeText(NewActivity.this, "Marker is set", Toast.LENGTH_SHORT).show();

                    renderPdf(currentPageNum);
                    isMarkerSet = true;
                } catch (Exception e) {
                    Log.d("MAIN", e.toString());
                    Toast.makeText(NewActivity.this, "Try again! Marker is not set", Toast.LENGTH_SHORT).show();
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

    private void addMarker(float x, float y) {
        markerView = new ImageView(this);
        markerView.setImageResource(R.drawable.ic_mark);

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

    private void renderPdf(int position) {
        try {
            mPageCount = document.getNumberOfPages();

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            binding.pdfView.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"new.pdf"))
                    .defaultPage(0)
                    .pages(0)
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
                                Toast.makeText(NewActivity.this, "First set a Marker", Toast.LENGTH_SHORT).show();
                            }

                            return false;
                        }
                    })
                    .load();
            if (position < mPageCount) {
                Bitmap bitmap = pdfRenderer.renderImage(position, 1, ImageType.RGB);
//                binding.imageView.setImageBitmap(bitmap);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (document != null) {
                document.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
