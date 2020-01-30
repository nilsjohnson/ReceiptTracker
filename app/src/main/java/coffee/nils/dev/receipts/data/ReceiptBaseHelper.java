package coffee.nils.dev.receipts.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.AbstractMap;

import static coffee.nils.dev.receipts.data.DAO.getStoreHashTableContentValues;
import static coffee.nils.dev.receipts.data.ReceiptDBSchema.*;
import static coffee.nils.dev.receipts.data.ReceiptDBSchema.ReceiptTable.COLS.STORE_NAME;
import static coffee.nils.dev.receipts.data.ReceiptDBSchema.ReceiptTable.MAX_FIELD_LENGTH_DEFAULT;


public class ReceiptBaseHelper extends SQLiteOpenHelper
{
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "receiptBase.db";
    private static final String TAG = "ReceiptBaseHelper";

    public ReceiptBaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("create table " + ReceiptTable.NAME + "(" +
                "_id integer primary key autoincrement, " +
                ReceiptTable.COLS.UUID + " INTEGER, " +
                STORE_NAME + " varchar(" + MAX_FIELD_LENGTH_DEFAULT + "), " +
                ReceiptTable.COLS.DATE + " INTEGER, " +
                ReceiptTable.COLS.AMOUNT + " INTEGER, " +
                ReceiptTable.COLS.IMAGE_IS_CROPPED + " INTEGER " +
                ")"
        );

        db.execSQL("create table " + StoreNameHashTable.NAME + "(" +
                "_id integer primary key autoincrement, " +
                StoreNameHashTable.COLS.KEY + " varchar(" + StoreNameHashTable.MAX_FIELD_LENGTH_DEFAULT + "), " +
                StoreNameHashTable.COLS.VALUE + " varchar(" + StoreNameHashTable.MAX_FIELD_LENGTH_DEFAULT + ") " +
                ")"
        );

        Log.d(TAG, "Preloading Store Names into " + StoreNameHashTable.NAME);
        for(AbstractMap.SimpleEntry entry : Constants.KNOWN_STORES)
        {
            ContentValues values = getStoreHashTableContentValues(entry);
            db.insert(StoreNameHashTable.NAME, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    { }
}
