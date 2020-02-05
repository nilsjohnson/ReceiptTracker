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
import java.util.ArrayList;
import java.util.HashMap;
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
    private ArrayList<Receipt> receiptList = new ArrayList<>();
    private ArrayList<String> categoryList = new ArrayList<>();
    private SQLiteDatabase database;

    // maps identifiers to store names
    private HashMap<String, String> storeMap = new HashMap<>();
    // maps store names to categories
    private HashMap<String, String> categoryMap = new HashMap<>();

    public static DAO get(Context context)
    {
        if (dao == null)
        {
            dao = new DAO(context);
        }

        return dao;
    }

    private DAO(Context context)
    {
        this.context = context.getApplicationContext();
        database = new ReceiptBaseHelper(context).getWritableDatabase();

        ReceiptCursorWrapper cursor = queryReceipts(null, null);

        // load receipts from DB
        try
        {
            cursor.moveToFirst();
            while(!cursor.isAfterLast())
            {
                Receipt r = cursor.getReceipt();
                receiptList.add(r);
                addCategory(r.getCategory());

                cursor.moveToNext();
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, "Problem reading in receipt table.\n" + e.getMessage());
        }
        finally
        {
            cursor.close();
        }

        // load the store name hash table from DB
        cursor = queryStoreNameHashTable(null, null);
        try
        {
            cursor.moveToFirst();
            while(!cursor.isAfterLast())
            {
                AbstractMap.SimpleEntry kv = cursor.getStoreKVpair();
                storeMap.put(kv.getKey().toString(), kv.getValue().toString());
                cursor.moveToNext();
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, "Problem reading in store name hash table.\n" + e.getMessage());
        }
        finally
        {
            cursor.close();
        }

        // load the categoy hash table from DB
        cursor = queryCategoryHashTable(null, null);
        try
        {
            cursor.moveToFirst();
            while(!cursor.isAfterLast())
            {
                AbstractMap.SimpleEntry kv = cursor.getStoreKVpair();
                categoryMap.put(kv.getKey().toString(), kv.getValue().toString());
                cursor.moveToNext();
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, "Problem reading in store name hash table.\n" + e.getMessage());
        }
        finally
        {
            cursor.close();
        }

    }

    private static ContentValues getReceiptContentValues(Receipt receipt)
    {
        ContentValues values = new ContentValues();
        values.put(ReceiptTable.COLS.UUID, receipt.getId().toString());
        values.put(ReceiptTable.COLS.STORE_NAME, receipt.getStoreName());
        values.put(ReceiptTable.COLS.AMOUNT, receipt.getTotalAmount());
        values.put(ReceiptTable.COLS.DATE, receipt.getDate().getTime());
        values.put(ReceiptTable.COLS.CATEGORY, receipt.getCategory());
        values.put(ReceiptTable.COLS.RECEIPT_BEEN_REVIEWED, receipt.hasBeenReviewd() ? 1 : 0);

        return values;
    }

    public static ContentValues getStoreHashTableContentValues(AbstractMap.SimpleEntry entry)
    {
        ContentValues values = new ContentValues();
        values.put(StoreNameHashTable.COLS.KEY, entry.getKey().toString());
        values.put(StoreNameHashTable.COLS.VALUE, entry.getValue().toString());

        return values;
    }

    public static ContentValues getCategoryHashTableContentValues(AbstractMap.SimpleEntry entry)
    {
        ContentValues values = new ContentValues();
        values.put(CategoryHashTable.COLS.KEY, entry.getKey().toString());
        values.put(CategoryHashTable.COLS.VALUE, entry.getValue().toString());

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
        ContentValues values = getReceiptContentValues(r);
        database.insert(ReceiptTable.NAME, null, values);
        receiptList.add(0, r);
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
        ContentValues values = getReceiptContentValues(receipt);

        database.update(ReceiptTable.NAME, values, ReceiptTable.COLS.UUID + " = ?",
                new String[]{uuidString});

       addCategory(receipt.getCategory());
       addStoreCategoryPair(receipt.getStoreName(), receipt.getCategory());
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

    private ReceiptCursorWrapper queryStoreNameHashTable(String whereClause, String[] whereArgs)
    {
        Cursor cursor = database.query(
                StoreNameHashTable.NAME,
                null, // selects all cols
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new ReceiptCursorWrapper(cursor);
    }

    private ReceiptCursorWrapper queryCategoryHashTable(String whereClause, String[] whereArgs)
    {
        Cursor cursor = database.query(
                CategoryHashTable.NAME,
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

    public String getStoreByKey(String key)
    {
        if(key != null)
        {
            return this.storeMap.get(key);
        }
        return null;
    }

    public void addStoreNameKvPair(String storeKey, String storeName)
    {
        if(storeKey == null || storeName == null)
        {
            return;
        }

        this.storeMap.put(storeKey, storeName);

        ContentValues values = getStoreHashTableContentValues(new AbstractMap.SimpleEntry(storeKey, storeName));
        database.insert(StoreNameHashTable.NAME, null, values);
        printStoreNameHashTable();
    }

    private void printStoreNameHashTable()
    {
        StringBuilder table = new StringBuilder();

        for (String key: storeMap.keySet()){
            table.append(key + ": " + storeMap.get(key) + "\n");
        }

        Log.d(TAG, table.toString());
    }

    public void deleteReceipt(UUID id)
    {
        for(int i = 0; i < receiptList.size(); i++)
        {
            if(receiptList.get(i).getId().equals(id))
            {
                database.delete(ReceiptTable.NAME, ReceiptTable.COLS.UUID + " = ?", new String[]{id.toString()});
                deleteImage(receiptList.get(i).getFileName());
                receiptList.remove(i);
                break;
            }
        }
    }

    public ArrayList<String> getCategoryList()
    {
        return this.categoryList;
    }

    private void addCategory(String category)
    {
        if(category != null && !categoryList.contains(category))
        {
            categoryList.add(category);
        }
    }

    private void addStoreCategoryPair(String storeName, String category)
    {
        ContentValues values = getCategoryHashTableContentValues(new AbstractMap.SimpleEntry(storeName, category));

        // if this is a new kv pair
        if(categoryMap.get(storeName) == null)
        {
            categoryMap.put(storeName, category);
            database.insert(CategoryHashTable.NAME, null, values);
            Log.d(TAG, "New entry to category map");
        }
        // if this is a update to a kv pair
        else if(!categoryMap.get(storeName).equals(category))
        {
            categoryMap.put(storeName, category);
            database.update(CategoryHashTable.NAME, values, CategoryHashTable.COLS.KEY + " + ?",
                    new String[]{storeName});
            Log.d(TAG, "Category map map updated");
        }
        else
        {
            Log.d(TAG, "Category map not updated");
        }
    }

    /**
     * @returns A suggested category for the store name, or null if it doesn't have one.
     */
    public String getCategory(String storeName)
    {
        if(storeName == null)
        {
            return null;
        }

        return categoryMap.get(storeName);
    }
}
