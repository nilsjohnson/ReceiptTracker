package coffee.nils.dev.receipts.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

// TODO remove sdf field from this class and use DateTools
public class Receipt
{
    private UUID id;
    private String storeName;
    private double totalAmount;
    private Date date;
    private boolean hasBeenReviewd = false;
    private String category;

    // defines how we show date to user
    public static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

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
