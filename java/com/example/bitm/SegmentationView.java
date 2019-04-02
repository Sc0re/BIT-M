package com.example.bitm;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class SegmentationView extends AppCompatActivity {

    private SegmentationApp segApp;

    // For the point state
    private boolean pickingPoint = false;
    private String pointType = "Bg";

    private final Scalar COLOR_RED = new Scalar(255, 0, 0);

    private final Scalar COLOR_GREEN = new Scalar(0, 255, 0);

    private final Scalar COLOR_BLUE = new Scalar(0, 0, 255);

    // For the rect state
    private boolean pickingRect = true;
    private int stateRect = 0;
    private Point rectPoint;

    // For view
    ProgressDialog dlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Init the activity and set the layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.segmentation_activity);

        // Create a intent to get the photoPath from Main activity
        // TODO fix this
        Intent returnIntent = getIntent();
        String photoPath = returnIntent.getStringExtra("photoPath");

        if(photoPath == null) {
            Log.d("[DEBUG]", "Emppty path");
        }

        dlg = new ProgressDialog(this);

        // Start the segApp app
        startSegApp(photoPath);

        // Display the segmented img on screen
        showImageOnScreen();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // Get the coordinate adjusted for the Mat
        View imageView = findViewById(R.id.imageView2);

        double x = e.getX() - imageView.getX();
        double y = e.getY() - imageView.getY();

        if(x < 0 || y < 0)
            return false;

        // Check in which state are
        // Cant mark a point till you run the rect
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            if (pickingPoint && !pickingRect) {
                // Offset for cursor
                x -= 100.0;
                y -= 100.0;

                markPoint(x, y);

                drawCursor(x, y);

                showImageOnScreen();
                cleanCursor();
            }

            if (!pickingPoint && pickingRect) {
                markRect(x, y);

                showImageOnScreen();
            }
        }

        return false;
    }

    public void changePointType (View view) {
        if(pointType.equals("Bg"))
            pointType = "Fg";
        else
            pointType = "Bg";
    }

    public void startPointPicking (View view) {
        if(pickingPoint)
            pickingPoint = false;
        else
            pickingPoint = true;
    }

    public void resetRect (View view) {
        segApp.cleanViewMat();

        stateRect = 0;
        pickingRect = true;

        showImageOnScreen();
    }

    public void runSegmentation (View view) {
        new ProcessImageTask().execute();
    }

    private void cleanCursor() {
        segApp.cleanCursor(); }

    private void drawCursor(double x, double y) {
        segApp.drawCursor(x, y, COLOR_RED);

    }

    private void drawPoint(double x, double y) {
        if(pointType.equals("Fg"))
            segApp.drawPoint(x, y, COLOR_GREEN);
        else if(pointType.equals("Bg"))
            segApp.drawPoint(x, y, COLOR_BLUE);
    }

    private void drawRect(double xi, double yi, double xf, double yf) {
        segApp.drawRect(xi, yi, xf, yf);

    }

    private void markPoint(double x, double y) {
        drawPoint(x, y);

        if (pointType.equals("Fg"))
            segApp.addFgPoint(x, y);

        else if (pointType.equals("Bg"))
            segApp.addBgPoint(x, y);
    }

    private void markRect(double x, double y) {
        // Pick the rect

        // If has nothing, mark a point
        if (stateRect == 0) {
            drawPoint(x, y);

            rectPoint = new Point(x, y);

            stateRect = 1;
        }

        // If has just a point, build the rect
        else if (stateRect == 1) {
            drawPoint(x, y);
            drawRect(rectPoint.x, rectPoint.y, x, y);

            segApp.setSegRect(rectPoint.x, rectPoint.y, x, y);

            stateRect = 2;
        }

        // If rect already set, then update
        else if (stateRect == 2) {
            segApp.cleanViewMat();

            drawPoint(rectPoint.x, rectPoint.y);
            drawPoint(x, y);


            drawRect(rectPoint.x, rectPoint.y, x, y);

            segApp.setSegRect(rectPoint.x, rectPoint.y, x, y);
        }
    }

    private void startSegApp (String photoPath) {
        if (photoPath == null) {
            return;
        }

        segApp = new SegmentationApp(
                photoPath,
                8.0 * Resources.getSystem().getDisplayMetrics().widthPixels / 10.0,
                9.5 * Resources.getSystem().getDisplayMetrics().heightPixels / 10.0
        );
    }

    private void showImageOnScreen () {
        // Underlay the segmented image over the original mat
        ImageView imgView1 = findViewById(R.id.imageView);
        ImageView imgView2 = findViewById(R.id.imageView2);

        imgView1.setImageBitmap(segApp.MatToBmp(segApp.getSegmentedMat()));
        imgView2.setImageBitmap(segApp.MatToBmp(segApp.getViewMat()));
    }

    public void galleryAddPic(View view) {
        segApp.exportBmpToGallery(this);
    }


    /*
    / Background running Segmentation
    */

    private class ProcessImageTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg.setMessage("Processing Image...");
            dlg.setCancelable(false);
            dlg.setIndeterminate(true);
            dlg.show();
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            segApp.run();

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            if(pickingRect)
                pickingRect = false;

            segApp.cleanViewMat();
            showImageOnScreen();

            dlg.dismiss();
        }
    }
}