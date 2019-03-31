package com.example.bitm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoApp {

    private String photoPath;
    private Context context;

    public PhotoApp(Context context) {
        this.context = context;
    }

    public void getPhoto() {
        Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (photoIntent.resolveActivity(context.getPackageManager()) != null) {

            File photoFile = null;

            try {
                photoFile = createImageFile();
            }
            catch (IOException ex) {
                Log.d("[DEBUG]", ex.toString());
            }

            if(photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(context,
                        "com.example.bitm.fileprovider",
                        photoFile);

                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                context.startActivity(photoIntent);

                galleryAddPic();
            }
        }
    }

    public String getPhotoPath() {
        return this.photoPath;
    }

    private File createImageFile() throws IOException {
        // Create the image File
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        // Save the file
        photoPath = image.getAbsolutePath();

        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

}
