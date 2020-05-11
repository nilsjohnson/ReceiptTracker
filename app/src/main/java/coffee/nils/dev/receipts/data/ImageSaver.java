package coffee.nils.dev.receipts.data;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

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

        String serObj = new String("lols");

        try {

            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(serObj);
            objectOut.close();
            System.out.println("The Object  was succesfully written to a file");

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }


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
