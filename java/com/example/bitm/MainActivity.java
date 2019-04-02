package com.example.bitm;

import android.content.Intent;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private boolean hasStarted = false;

    private String photoPath;

    // Using for debug
    private int stateId = 0;

    private int PHOTO_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Switch between the stats of the app

        if (hasStarted) {
            if (stateId == 0)
                takePhoto();

            if (stateId == 1)
                startSegmentation();

            if (stateId == 2)
                ;
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode == PHOTO_REQUEST && resultCode == RESULT_OK) {
            // Get the path from the photo
            photoPath = data.getStringExtra("PhotoPath");

            stateId = 1;
        }
    }

    public void startApp(View view) {
        hasStarted = true;
        onStart();
    }

    private void takePhoto() {
        Intent photoIntent = new Intent(this, PhotoView.class);

        startActivityForResult(photoIntent, PHOTO_REQUEST);
    }

    private void startSegmentation() {
        Intent segmentationIntent = new Intent(this, SegmentationView.class);

        segmentationIntent.putExtra("photoPath", this.photoPath);

        startActivity(segmentationIntent);
    }
}