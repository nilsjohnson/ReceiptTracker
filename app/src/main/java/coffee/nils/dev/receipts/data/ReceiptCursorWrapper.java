package coffee.nils.dev.receipts.data;

import android.database.Cursor;
import android.database.CursorWrapper;

import java.util.AbstractMap;
import java.util.Date;
import java.util.UUID;

public class ReceiptCursorWrapper extends CursorWrapper
{
    public ReceiptCursorWrapper(Cursor cursor)
    {
        super(cursor);
    }

    public Receipt getReceipt()
    {
        String uuidString = getString(getColumnIndex(ReceiptDBSchema.ReceiptTable.COLS.UUID));
        String storeName = getString(getColumnIndex(ReceiptDBSchema.ReceiptTable.COLS.STORE_NAME));
        double amount = getDouble(getColumnIndex(ReceiptDBSchema.ReceiptTable.COLS.AMOUNT));
        long date = getLong(getColumnIndex(ReceiptDBSchema.ReceiptTable.COLS.DATE));
        int isReviewed = getInt(getColumnIndex(ReceiptDBSchema.ReceiptTable.COLS.RECEIPT_BEEN_REVIEWED));
        String category = getString(getColumnIndex(ReceiptDBSchema.ReceiptTable.COLS.CATEGORY));

        Receipt receipt = new Receipt(UUID.fromString(uuidString));

        receipt.setStoreName(storeName);
        receipt.setTotalAmount(amount);
        receipt.setDate(new Date(date));
        receipt.setHasBeenReviewed(isReviewed != 0);
        receipt.setCategory(category);

        return receipt;
    }

    public AbstractMap.SimpleEntry getStoreKVpair()
    {
        String key = getString(getColumnIndex(ReceiptDBSchema.StoreNameHashTable.COLS.KEY));
        String value = getString(getColumnIndex(ReceiptDBSchema.StoreNameHashTable.COLS.VALUE));
        return new AbstractMap.SimpleEntry(key, value);
    }

    public AbstractMap.SimpleEntry getCategoryKVpair()
    {
        String key = getString(getColumnIndex(ReceiptDBSchema.CategoryHashTable.COLS.KEY));
        String value = getString(getColumnIndex(ReceiptDBSchema.CategoryHashTable.COLS.VALUE));
        return new AbstractMap.SimpleEntry(key, value);
    }
}
