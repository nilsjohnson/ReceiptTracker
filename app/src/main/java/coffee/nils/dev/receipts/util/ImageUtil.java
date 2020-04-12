package coffee.nils.dev.receipts.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.Image;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;

public class ImageUtil
{
    public static Bitmap getScaledBitmap(String path, Activity activity)
    {
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay()
                .getSize(size);

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

    /*
    TODO scale this return value
     */
    public static Bitmap imageToBitmap(Image image)
    {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
    }

}
