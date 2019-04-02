package com.example.bitm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

public class SegmentationApp {

    private Mat photoMat;
    private Mat photoMask;

    private Mat viewMat = new Mat();
    private Mat segmentedMat = new Mat();

    private Mat tmpViewMat = new Mat();

    private Rect segRect;

    private Mat fgMat = new Mat();
    private Mat bgMat = new Mat();

    private Vector<Point> bgPoints = new Vector<>();
    private Vector<Point> fgPoints = new Vector<>();

    private boolean hasRect = false;
    private boolean hasRectRun = false;

    SegmentationApp(String photoPath, double width, double height) {
        photoMat = new Mat();
        photoMat = Imgcodecs.imread(photoPath);

        Imgproc.resize(photoMat, photoMat, new Size(width, height));
        Imgproc.cvtColor(photoMat, photoMat, Imgproc.COLOR_BGR2RGB);

        photoMask = new Mat(new Size(width, height), CvType.CV_8UC1);

        photoMat.copyTo(viewMat);
        photoMat.copyTo(tmpViewMat);
        photoMat.copyTo(segmentedMat);
    }

    public void run() {
        // TODO compress image and resize again for a speed bost
        int iteration = 1;

        addPointsInMask();

        if(!hasRect)
            return;

        if(!hasRectRun) {
            Imgproc.grabCut(photoMat, photoMask, segRect, bgMat, fgMat, iteration, Imgproc.GC_INIT_WITH_RECT);

            hasRectRun = true;
        }
        else {
            Imgproc.grabCut(photoMat, photoMask, segRect, bgMat, fgMat, iteration, Imgproc.GC_INIT_WITH_MASK);

            Log.d("[DEBUG]", "Running GC with mask");
        }

        // Put the segmented Image in segmentedMat
        getImg().copyTo(segmentedMat);
    }

    public void addBgPoint(double x, double y) {
        bgPoints.add(new Point(x, y));
    }

    public void addFgPoint(double x, double y) {
        fgPoints.add(new Point(x, y));
    }

    public void cleanViewMat() { photoMat.copyTo(viewMat); }

    public void cleanCursor() { tmpViewMat.copyTo(viewMat); }

    public void drawCursor(double x, double y, Scalar color) {
        int radius = 5;

        // Make a copy to late clean the cursor
        viewMat.copyTo(tmpViewMat);

        Imgproc.circle(viewMat, new Point(x, y), radius, color, 3);
    }

    public void drawPoint(double x, double y, Scalar color) {
        int radius = 5;

        Imgproc.circle(viewMat, new Point(x, y), radius, color, -1);
    }

    public void drawRect(double xi, double yi, double xf, double yf) {
        Imgproc.rectangle(viewMat, new Point(xi, yi), new Point(xf, yf), new Scalar(255, 0, 0), 3);
    }

    public void exportBmpToGallery(Context context) {
        Bitmap imgAlpha = SegmentedToBmpAlpha();

        String path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "EXPORTED_PNG_" + timeStamp + "_";

        File file = new File(path, imageFileName+".png");

        try (FileOutputStream out = new FileOutputStream(file)) {
            imgAlpha.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a losses format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String savedImgUrl = MediaStore.Images.Media.insertImage(
                    context.getContentResolver(),
                    file.getAbsolutePath(),
                    file.getName(),
                    file.getName()
            );

            Uri.parse(savedImgUrl);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Mat getViewMat() { return this.viewMat; }

    public Mat getSegmentedMat() { return this.segmentedMat; }

    public Bitmap MatToBmp(Mat inputMat) {
        Bitmap photoBm = Bitmap.createBitmap(photoMat.cols(), photoMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(inputMat, photoBm);

        return photoBm;
    }

    public Bitmap SegmentedToBmpAlpha() {
        Mat img = getImg();

        Bitmap bmp = null;

        try {
            Imgproc.resize(img, img, new Size(img.width(), img.height()));
            //Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2BGR);

            bmp = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(img, bmp);

            // Transparent BG
            for(int x = 0; x < bmp.getWidth(); x++){
                for(int y = 0; y < bmp.getHeight(); y++){
                    if(bmp.getPixel(x, y) == Color.BLACK){
                        bmp.setPixel(x, y, Color.TRANSPARENT);
                    }
                }
            }
        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
        }

        return bmp;
    }

    public void setSegRect(double xi, double yi, double xf, double yf) {
        segRect = new Rect(new Point(xi, yi), new Point(xf, yf));

        hasRect = true;
    }

    private void addPointsInMask() {
        int radius =  5;

        for(Point point : bgPoints) {
            Imgproc.circle(photoMask, point, radius, new Scalar(Imgproc.GC_BGD), -1);
        }

        for(Point point : fgPoints) {
            Imgproc.circle(photoMask, point, radius, new Scalar(Imgproc.GC_FGD), -1);
        }
    }

    private Mat getImg() {
        Mat result;
        Mat binMask;

        result = new Mat(photoMat.size(), photoMat.type(), new Scalar(0, 0 , 0));

        binMask = getBinMask();

        photoMat.copyTo(result, binMask);

        return result;
    }

    private Mat getBinMask() {
        Mat binMask;

        binMask = new Mat(photoMask.size(), CvType.CV_8UC1);

        Mat src2 = new Mat(photoMask.size(), CvType.CV_8UC1, new Scalar(1));

        Core.bitwise_and(photoMask, src2, binMask);

        return binMask;
    }
}
