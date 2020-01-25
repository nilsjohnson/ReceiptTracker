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

    // defines how we show date to user
    private static SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

    public Receipt()
    {
        this(UUID.randomUUID());
    }

    public Receipt(UUID id)
    {
        this.id = id;
        this.date = new Date();
    }


    public UUID getId()
    {
        return id;
    }

    /**
     * @return The name of the file. Does not include the full path.
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

    public String getSimpleDate()
    {
        return sdf.format(this.date);
    }

    public void setHasBeenReviewed(boolean val)
    {
        this.hasBeenReviewd = val;
    }

    // TODO, update database to reflect this name, instead of "isCropped"
    public boolean hasBeenReviewd()
    {
        return this.hasBeenReviewd;
    }

}
