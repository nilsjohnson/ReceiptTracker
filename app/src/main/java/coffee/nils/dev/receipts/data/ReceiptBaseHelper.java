package coffee.nils.dev.receipts.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static coffee.nils.dev.receipts.data.ReceiptDBSchema.*;
import static coffee.nils.dev.receipts.data.ReceiptDBSchema.ReceiptTable.MAX_FIELD_LENGTH_DEFAULT;


public class ReceiptBaseHelper extends SQLiteOpenHelper
{
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "receiptBase.db";

    public ReceiptBaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("create table " + ReceiptTable.NAME + "(" +
                "_id integer primary key autoincrement, " +
                ReceiptTable.Cols.UUID + " INTEGER, " +
                ReceiptTable.Cols.STORE_NAME + " varchar(" + MAX_FIELD_LENGTH_DEFAULT + "), " +
                ReceiptTable.Cols.DATE + " INTEGER, " +
                ReceiptTable.Cols.IMAGE_IS_CROPPED + " INTEGER " +
                ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }
}
