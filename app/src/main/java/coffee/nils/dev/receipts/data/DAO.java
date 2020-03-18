package coffee.nils.dev.receipts.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;

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
    public void saveImage(Bitmap bitmap, String filename) throws IOException
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
    }

    /**
     * @return The file containing the receipt's image.
     * This method belongs to the ReceiptDAO and not the Receipt class because it requires the
     * application context.
     */
    public File getPhotoFile(Receipt receipt)
    {
        File fileDir = this.context.getFilesDir();
        return new File(fileDir, receipt.getFileName());
    }

}
