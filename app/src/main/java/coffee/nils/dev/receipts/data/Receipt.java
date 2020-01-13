package coffee.nils.dev.receipts.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Receipt
{
    private UUID id;
    private String storeName;
    private int int_totalAmount;
    private Date date;
    private boolean imageIsCropped = false;

    // defines how we show date to user
    private static SimpleDateFormat sdf = new SimpleDateFormat("MMMM/dd/yyyy");

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


        return this.int_totalAmount*.01;
    }

    public void setTotalAmount(int totalAmount)
    {
        this.int_totalAmount = totalAmount;
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
        val = val*100;
        int_totalAmount = (int)Math.round(val);
    }

    public String getSimpleDate()
    {
        return sdf.format(this.date);
    }

    public void SetImageIsCropped(boolean val)
    {
        imageIsCropped = val;
    }

    public boolean imageIsCropped()
    {
        return this.imageIsCropped;
    }

}
