package coffee.nils.dev.receipts.data;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static coffee.nils.dev.receipts.data.ReceiptDBSchema.*;

// TODO deal with min/max with deletes

/**
 * This singleton manages all the data for this app.
 */
public class ReceiptDAO extends DAO
{
    private static final String TAG = "ReceiptDAO";

    private static ReceiptDAO receiptDAO;

    private Filter filter;

    // the master list of all receipts
    private ArrayList<Receipt> receiptList = new ArrayList<>();
    // the list of all categories the user has made, ie. Groceries, Entertainment, etc
    private ArrayList<String> categoryList = new ArrayList<>();

    // maps identifiers to store names, ie. <www.homedepot.com><Home Depot>
    private HashMap<String, String> storeMap = new HashMap<>();
    // maps store name to last category chosen for that store, ie <Home Depot><Household>
    private HashMap<String, String> categoryMap = new HashMap<>();

    // keeps track for the date range
    private Receipt lowestDateReceipt;
    private Receipt highestDateReceipDate;


    /**
     * @param context the application context
     * @return The, one and only, ReceiptDAO
     */
    public static ReceiptDAO get(Context context)
    {
        if (receiptDAO == null)
        {
            receiptDAO = new ReceiptDAO(context);
        }

        return receiptDAO;
    }

    /**
     * @param context the application context
     * Loads the database into memory.
     */
    private ReceiptDAO(Context context)
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
                //receiptList.add(r);
                addReceipt(r);
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

    /**
     * @param receipt to add to list. Does not insert into database.
     */
    private void addReceipt(Receipt receipt)
    {
        updateReceiptRange(receipt);
        receiptList.add(receipt);
    }

    /**
     * @param receipt to check and set as either min or/and max, with respect to date.
     */
    private void updateReceiptRange(Receipt receipt)
    {
        if(receipt.getDate() != null)
        {
            if(this.highestDateReceipDate == null || receipt.getDate().getTime() > this.highestDateReceipDate.getDate().getTime())
            {
                this.highestDateReceipDate = receipt;
            }

            if(this.lowestDateReceipt == null || receipt.getDate().getTime() < this.lowestDateReceipt.getDate().getTime())
            {
                this.lowestDateReceipt = receipt;
            }
        }
    }

    /**
     * @return The receipt list. Will return a filtered list if filter is set.
     */
    public List<Receipt> getReceiptList()
    {
        if(filter == null)
        {
            return this.receiptList;
        }

        Date startDate = filter.startDate;
        Date endDate = filter.endDate;

        ArrayList<Receipt> filteredReceiptList = new ArrayList<>();

        for (Receipt r : receiptList)
        {
            // using ! to make this inclusive
            if (!r.getDate().before(startDate) && !r.getDate().after(endDate))
            {
                filteredReceiptList.add(r);
            }
        }
        return filteredReceiptList;
    }

    /**
     *
     * @param id a receipt's id
     * @return the receipt
     */
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

    /**
     *
     * @return A fresh receipt, in the database with a uid and date of now.
     */
    public Receipt createReceipt()
    {
        Receipt r = new Receipt();
        ContentValues values = getReceiptContentValues(r);
        database.insert(ReceiptTable.NAME, null, values);
        receiptList.add(0, r);
        return r;
    }



    public void updateReceipt(Receipt receipt)
    {
        String uuidString = receipt.getId().toString();
        ContentValues values = getReceiptContentValues(receipt);

        database.update(ReceiptTable.NAME, values, ReceiptTable.COLS.UUID + " = ?",
                new String[]{uuidString});

       addCategory(receipt.getCategory());
       addStoreCategoryPair(receipt.getStoreName(), receipt.getCategory());

       // if the receipt was the highest or lowest, we now no longe
        // can trust our high low flags, so unfortunetly we have to recheck each one
       if(receipt == highestDateReceipDate || receipt == lowestDateReceipt)
       {
            resetHighestHowestDates();
       }
       else
       {
           updateReceiptRange(receipt);
       }

    }

    /**
     * iterates over entire receipt list to set the highest and lowest.
     */
    private void resetHighestHowestDates()
    {
        highestDateReceipDate = null;
        lowestDateReceipt = null;

        for (Receipt receipt : receiptList)
        {
            if(highestDateReceipDate == null || receipt.getDate().after(highestDateReceipDate.getDate()))
            {
                highestDateReceipDate = receipt;
            }
            if(lowestDateReceipt == null || receipt.getDate().before(lowestDateReceipt.getDate()))
            {
                lowestDateReceipt = receipt;
            }
        }
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

        resetHighestHowestDates();
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
        if(storeName == null || category == null)
        {
            return;
        }

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

    public Date getHighestDate()
    {
        return highestDateReceipDate.getDate();
    }

    public Date getLowestDate()
    {
        return lowestDateReceipt.getDate();
    }

    public void setFilter(Filter filter) throws Exception
    {
        if(filter.startDate == null || filter.endDate == null)
        {
            throw new Exception("All filter fields must be specified");
        }

        this.filter = filter;
    }

    public void removeFilter()
    {
        this.filter = null;
    }

    public Filter getFilter()
    {
        return this.filter;
    }
}
