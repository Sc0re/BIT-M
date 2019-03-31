package com.example.bitm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;


public class PhotoView extends AppCompatActivity {

    private PhotoApp photoapp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        photoapp = new PhotoApp(this);
    }

    public void takePhoto(View view) {
        photoapp.getPhoto();

        Log.d("DEBUG", photoapp.getPhotoPath());

        Intent returnIntent = new Intent();
        returnIntent.putExtra("PhotoPath", photoapp.getPhotoPath());
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}
