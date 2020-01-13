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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static coffee.nils.dev.receipts.data.ReceiptDBSchema.*;

/**
 * This singleton manages all the data for this app.
 */
public class DAO
{
    private static final String TAG = "Dao";
    private static DAO dao;

    private Context context;
    private List<Receipt> receiptList;
    private SQLiteDatabase database;

    public static DAO get(Context context)
    {
        if ((dao == null))
        {
            dao = new DAO(context);
        }

        return dao;
    }

    private DAO(Context context)
    {
        this.context = context.getApplicationContext();
        database = new ReceiptBaseHelper(context).getWritableDatabase();

        receiptList = new ArrayList<>();
        ReceiptCursorWrapper cursor = queryReceipts(null, null);

        try
        {
            cursor.moveToFirst();
            while(!cursor.isAfterLast())
            {
                receiptList.add(cursor.getReceipt());
                cursor.moveToNext();
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, "Problem reading in db.\n" + e.getMessage());
        }
        finally
        {
            cursor.close();
        }
    }

    private static ContentValues getContentValues(Receipt receipt)
    {
        ContentValues values = new ContentValues();
        values.put(ReceiptTable.Cols.UUID, receipt.getId().toString());
        values.put(ReceiptTable.Cols.STORE_NAME, receipt.getStoreName());
        values.put(ReceiptTable.Cols.DATE, receipt.getDate().getTime());
        values.put(ReceiptTable.Cols.IMAGE_IS_CROPPED, receipt.imageIsCropped() ? 1 : 0);

        return values;
    }

    public List<Receipt> getReceiptList()
    {
        return this.receiptList;
    }

    public Receipt getReceiptById(UUID id)
    {
        for (Receipt p : receiptList)
        {
            if (p.getId().equals(id))
            {
                return p;
            }
        }

        return null;
    }

    public Receipt createReceipt()
    {
        Receipt r = new Receipt();
        ContentValues values = getContentValues(r);
        database.insert(ReceiptTable.NAME, null, values);
        receiptList.add(r);
        return r;
    }

    /**
     * @return The file containing the receipt's image.
     * This method belongs to the DAO and not the Receipt class because it requires the
     * application context.
     */
    public File getPhotoFile(Receipt receipt)
    {
        File fileDir = this.context.getFilesDir();
        return new File(fileDir, receipt.getFileName());
    }

    public void updateReceipt(Receipt receipt)
    {
        String uuidString = receipt.getId().toString();

        ContentValues values = getContentValues(receipt);
        database.update(ReceiptTable.NAME, values, ReceiptTable.Cols.UUID + " = ?",
                new String[]{uuidString});


        for (Receipt r : receiptList)
        {
            if (r.getId().equals(receipt.getId()))
            {
                r = receipt;
            }
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
        } catch (IOException e)
        {
            throw e;
        }
    }

    private ReceiptCursorWrapper queryReceipts(String whereClause, String[] whereArgs)
    {
        Cursor cursor = database.query(
                ReceiptTable.NAME,
                null, // selects all cols
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new ReceiptCursorWrapper(cursor);
    }
}
