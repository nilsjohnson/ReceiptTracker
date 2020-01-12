package coffee.nils.dev.receipts.data;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DAO
{
    private static DAO dao;

    private Context context;
    private List<Receipt> receiptList;

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
        this.context = context;
        receiptList = new ArrayList<>();

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
        receiptList.add(r);
        return r;
    }

    /**
     @return The file containing the receipt's image.
     This method belongs to the DAO and not the Receipt class because it requires the
     application context.
     */
    public File getPhotoFile(Receipt receipt)
    {
        File fileDir = this.context.getFilesDir();
        return new File(fileDir, receipt.getFileName());
    }

    public void updateReceipt(Receipt receipt)
    {
        for (Receipt r : receiptList)
        {
            if (r.getId().equals(receipt.getId()))
            {
                r = receipt;
            }
        }

    }
}
