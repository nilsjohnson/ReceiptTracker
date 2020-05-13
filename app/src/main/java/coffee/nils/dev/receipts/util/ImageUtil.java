package coffee.nils.dev.receipts.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
//import android.graphics.Point;
import android.media.Image;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.RETR_CCOMP;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;

public class ImageUtil
{
    private static final String TAG = ImageUtil.class.getSimpleName();

    static
    {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    private static native long autoCrop(long addr);

    public static Bitmap getScaledBitmap(String path, Activity activity)
    {
        android.graphics.Point size = new android.graphics.Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);

        return getScaledBitmap(path, size.x, size.y);
    }

    public static Bitmap getEmptyBitmap(Mat mat)
    {
        int height = mat.rows();
        int width = mat.cols();

        Bitmap.Config config = Bitmap.Config.ARGB_8888;

        Bitmap bmp = Bitmap.createBitmap(width, height, config);
        return bmp;
    }

    public static Bitmap getScaledBitmap(Mat mat, Activity activity)
    {
        android.graphics.Point size = new android.graphics.Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);

        double matWidth = mat.cols();
        double matHeight = mat.rows();
        double screenWidth = size.x;
        double resizeFactor = screenWidth / matWidth;

        if(resizeFactor > 1)
        {
            resizeFactor = 1;
        }

        int newWidth = (int) (matWidth*resizeFactor);
        int newHeight = (int) (matHeight*resizeFactor);
        Size s = new Size(newWidth, newHeight);

        String str = "\nMat Width: " + matWidth + "\nMat Height: " + matHeight + "\n resizeFactor: " + resizeFactor;
        Log.d(TAG, "Mat Width" + str);

        // make a new Mat so that we can save the original in its full size
        Mat resized = new Mat();
        resize(mat, resized, s);

        // back to the Android Bitmap
        cvtColor(resized, resized, COLOR_BGR2RGB);

        // finally, convert to bitmap
        Bitmap bitmap = getEmptyBitmap(resized);
        Utils.matToBitmap(resized, bitmap);

        return bitmap;
    }


    public static Bitmap getScaledBitmap(String path, int destWidth, int destHeight)
    {
        // read in the dimensions of the image on disk
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;

        int inSampleSize = 1;
        if (srcHeight > destHeight || srcWidth > destWidth)
        {
            if (srcWidth > srcHeight)
            {
                inSampleSize = Math.round(srcHeight / destHeight);
            } else
            {
                inSampleSize = Math.round(srcWidth / destWidth);
            }
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        return BitmapFactory.decodeFile(path, options);
    }

    public static Mat imageToMat(Image image)
    {
        ByteBuffer bb = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[bb.remaining()];
        bb.get(data);

        return Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_UNCHANGED);
    }

    public static Mat autoCrop(Bitmap bitmap)
    {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        return autoCrop(mat);

    }


    public static Mat readMat(String path)
    {
        return Imgcodecs.imread(path);
    }

    public static Mat autoCrop(Mat image)
    {
        long addr = autoCrop(image.getNativeObjAddr());
        return new Mat(addr);
    }
}
