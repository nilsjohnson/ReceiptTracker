package coffee.nils.dev.receipts.data;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


import coffee.nils.dev.receipts.R;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */
class ImageSaver implements Runnable
{
    private static String TAG = "ImageSaver";
    /**
     * The JPEG image
     */
    private final Bitmap image;

    /**
     * The file we save the image into.
     */
    private final File file;

    ImageSaver(Bitmap image, File file)
    {
        this.image = image;
        this.file = file;
    }

    @Override
    public void run()
    {
        OutputStream outStream = null;

        try
        {
            outStream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            outStream.flush();
            outStream.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, e.getMessage());
        }
    }
}
