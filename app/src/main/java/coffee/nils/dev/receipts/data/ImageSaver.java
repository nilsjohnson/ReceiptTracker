package coffee.nils.dev.receipts.data;

import android.media.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */
class ImageSaver implements Runnable {

    /**
     * The JPEG image
     */
    private final Image image;

    /**
     * The file we save the image into.
     */
    private final File file;

    ImageSaver(Image image, File file)
    {
        this.image = image;
        this.file = file;
    }

    @Override
    public void run()
    {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = null;

        try
        {
            output = new FileOutputStream(file);
            output.write(bytes);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            image.close();
            if (null != output)
            {
                try
                {
                    output.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
