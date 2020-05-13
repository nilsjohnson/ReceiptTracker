package coffee.nils.dev.receipts.data;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

/**
 * Saves a Mat to file.
 */
class ImageSaver implements Runnable
{
    private static String TAG = "ImageSaver";
    private final Mat image;
    private final String path;

    ImageSaver(Mat image, File file)
    {
        this.image = image;
        this.path = file.toString();
    }

    @Override
    public void run() {
        if(Imgcodecs.imwrite(path, image))
        {
            Log.d(TAG, path + " saved.");
            // explicitly release right away to save resources
            image.release();
        }
        else
        {
            Log.d(TAG, path + " not saved.");
        }
    }
}
