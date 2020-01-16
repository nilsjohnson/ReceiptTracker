package coffee.nils.dev.receipts.data;

import android.database.Cursor;
import android.database.CursorWrapper;

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
        String uuidString = getString(getColumnIndex(ReceiptDBSchema.ReceiptTable.Cols.UUID));
        String storeName = getString(getColumnIndex(ReceiptDBSchema.ReceiptTable.Cols.STORE_NAME));
        double amount = getDouble(getColumnIndex(ReceiptDBSchema.ReceiptTable.Cols.AMOUNT));
        long date = getLong(getColumnIndex(ReceiptDBSchema.ReceiptTable.Cols.DATE));
        int isCropped = getInt(getColumnIndex(ReceiptDBSchema.ReceiptTable.Cols.IMAGE_IS_CROPPED));

        Receipt receipt = new Receipt(UUID.fromString(uuidString));

        receipt.setStoreName(storeName);
        receipt.setTotalAmount(amount);
        receipt.setDate(new Date(date));
        receipt.SetImageIsCropped(isCropped != 0);

        return receipt;
    }
}
