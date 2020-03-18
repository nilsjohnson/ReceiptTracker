package coffee.nils.dev.receipts.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Receipt
{
    private UUID id;
    private String storeName;
    private double totalAmount;
    private Date date;
    private boolean hasBeenReviewd = false;
    private String category;

    /**
     * For making a brand new receipt. See ReceiptDAO.createReceipt() for best practice of
     * creating new receipts.
     */
    public Receipt()
    {
        this(UUID.randomUUID());
    }

    /**
     * For loading receipts for storage into memory
     * @param id
     */
    public Receipt(UUID id)
    {
        this.id = id;
        this.date = new Date(); // safeguards against receipts coming from storage that might not have dates.
    }

    public UUID getId()
    {
        return id;
    }

    /**
     * @return The name of the file. Does not include the full path. Get that from the DAO.
     */
    public String getFileName()
    {
        return this.id.toString() + ".jpg";
    }

    public String getStoreName()
    {
        return storeName;
    }

    public void setStoreName(String storeName)
    {
        this.storeName = storeName;
    }

    public double getTotalAmount()
    {
        return this.totalAmount;
    }

    public Date getDate()
    {
        return this.date;
    }

    public void setDate(Date d)
    {
        this.date = d;
    }

    public void setTotalAmount(double val)
    {
        this.totalAmount = val;
    }

    public void setHasBeenReviewed(boolean val)
    {
        this.hasBeenReviewd = val;
    }

    public boolean hasBeenReviewd()
    {
        return this.hasBeenReviewd;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getCategory()
    {
        return this.category;
    }

    @Override
    public String toString()
    {
        String str = "Date: " + this.date.toString() + "\n" +
                "Store: " + this.storeName + "\n";

        return str;
    }
}
