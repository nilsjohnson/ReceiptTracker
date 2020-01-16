package coffee.nils.dev.receipts.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.opencv.core.Mat;

import java.util.ArrayList;

import coffee.nils.dev.receipts.data.GuessableReceiptValues;

import static android.graphics.Bitmap.Config.ARGB_8888;

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

    public static GuessableReceiptValues guessProperties(Bitmap bitmap, Context context)
    {
        GuessableReceiptValues grv = new GuessableReceiptValues();

        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();

        Frame imageFrame = new Frame.Builder()

                .setBitmap(bitmap)                 // your image bitmap
                .build();

        String imageText = "";


        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
            imageText = textBlock.getValue();
            if(i == 0)
            {
                grv.storeName = imageText;
            }

            Log.d("image utils", imageText);
        }

        return grv;

    }

}
