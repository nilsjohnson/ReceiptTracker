package coffee.nils.dev.receipts.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.*;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;

import coffee.nils.dev.receipts.R;

/**
 * Provides core DAO operations, such database queries and file i/o
 */
public abstract class DAO
{
    protected Context context;
    private static final String TAG = "DAO";
    protected SQLiteDatabase database;

    public static final String FILES_AUTHORITY = "coffee.nils.dev.receipts.fileprovider";

    protected ReceiptCursorWrapper queryReceipts(String whereClause, String[] whereArgs)
    {
        Cursor cursor = database.query(
                ReceiptDBSchema.ReceiptTable.NAME,
                null, // selects all cols
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new ReceiptCursorWrapper(cursor);
    }

    protected ReceiptCursorWrapper queryStoreNameHashTable(String whereClause, String[] whereArgs)
    {
        Cursor cursor = database.query(
                ReceiptDBSchema.StoreNameHashTable.NAME,
                null, // selects all cols
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new ReceiptCursorWrapper(cursor);
    }

    protected ReceiptCursorWrapper queryCategoryHashTable(String whereClause, String[] whereArgs)
    {
        Cursor cursor = database.query(
                ReceiptDBSchema.CategoryHashTable.NAME,
                null, // selects all cols
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        ;

        return new ReceiptCursorWrapper(cursor);
    }

    protected static ContentValues getReceiptContentValues(Receipt receipt)
    {
        ContentValues values = new ContentValues();
        values.put(ReceiptDBSchema.ReceiptTable.COLS.UUID, receipt.getId().toString());
        values.put(ReceiptDBSchema.ReceiptTable.COLS.STORE_NAME, receipt.getStoreName());
        values.put(ReceiptDBSchema.ReceiptTable.COLS.AMOUNT, receipt.getTotalAmount());
        values.put(ReceiptDBSchema.ReceiptTable.COLS.DATE, receipt.getDate().getTime());
        values.put(ReceiptDBSchema.ReceiptTable.COLS.CATEGORY, receipt.getCategory());
        values.put(ReceiptDBSchema.ReceiptTable.COLS.RECEIPT_BEEN_REVIEWED, receipt.hasBeenReviewd() ? 1 : 0);

        return values;
    }

    protected static ContentValues getStoreHashTableContentValues(AbstractMap.SimpleEntry entry)
    {
        ContentValues values = new ContentValues();
        values.put(ReceiptDBSchema.StoreNameHashTable.COLS.KEY, entry.getKey().toString());
        values.put(ReceiptDBSchema.StoreNameHashTable.COLS.VALUE, entry.getValue().toString());

        return values;
    }

    protected static ContentValues getCategoryHashTableContentValues(AbstractMap.SimpleEntry entry)
    {
        ContentValues values = new ContentValues();
        values.put(ReceiptDBSchema.CategoryHashTable.COLS.KEY, entry.getKey().toString());
        values.put(ReceiptDBSchema.CategoryHashTable.COLS.VALUE, entry.getValue().toString());

        return values;
    }


    public void deleteImage(String fileName)
    {
        File file = new File(context.getFilesDir().toString() + "/" + fileName);

        if(file.delete())
        {
            Log.d(TAG, fileName + " deleted succesffuly");
        }
        else
        {
            Log.d(TAG, fileName + " failed to delete");
        }
    }

    /**
     * @param bitmap The bitmap holding the image
     * @param filename The file's name, w/out the full path.
     * @throws IOException Throws if file does not save correctly.
     */
/*    public void saveImage(Bitmap bitmap, String filename) throws IOException
    {
        OutputStream outStream = null;
        File file = new File(context.getFilesDir().toString() + "/" + filename);

        try
        {
            outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
            outStream.flush();
            outStream.close();
        }
        catch (IOException e)
        {
            throw e;
        }
    }*/

    /**
     * @return The file containing the receipt's image.
     * This method belongs to the ReceiptDAO and not the Receipt class because it requires the
     * application context.
     */
    public File getPhotoFile(Receipt receipt)
    {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), context.getResources().getString(R.string.app_name));
        dir.mkdirs();
        return new File(dir, receipt.getFileName());
    }



    public final void saveImage(Bitmap bitmap, String title) throws Exception
    {
        // Create a path where we will place our picture in the user's
        // public pictures directory.  Note that you should be careful about
        // what you place here, since the user often manages these files.  For
        // pictures and other media owned by the application, consider
        // Context.getExternalMediaDir().
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), context.getResources().getString(R.string.app_name));
        File file = new File(dir, title);

        try {
            // Make sure the Pictures directory exists.
            dir.mkdirs();

            OutputStream outStream = null;

            try
            {
                outStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream);
                outStream.flush();
                outStream.close();
            }
            catch (IOException e)
            {
                throw e;
            }

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(context,
                    new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }

    /**
     * A copy of the Android internals StoreThumbnail method, it used with the insertImage to
     * populate the android.provider.MediaStore.Images.Media#insertImage with all the correct
     * meta data. The StoreThumbnail method is private so it must be duplicated here.
     * @see android.provider.MediaStore.Images.Media (StoreThumbnail private method)
     */
    private static final Bitmap storeThumbnail(
            ContentResolver cr,
            Bitmap source,
            long id,
            float width,
            float height,
            int kind) {

        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0,
                source.getWidth(),
                source.getHeight(), matrix,
                true
        );

        ContentValues values = new ContentValues(4);
        values.put(Images.Thumbnails.KIND,kind);
        values.put(Images.Thumbnails.IMAGE_ID,(int)id);
        values.put(Images.Thumbnails.HEIGHT,thumb.getHeight());
        values.put(Images.Thumbnails.WIDTH,thumb.getWidth());

        Uri url = cr.insert(Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

}
