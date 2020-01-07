package coffee.nils.dev.receipttracker.data;

import java.io.File;
import java.util.UUID;

public class Receipt
{
    private UUID id;
    private String storeName;
    private int totalAmount;

    public Receipt()
    {
        this.id = UUID.randomUUID();
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

    public int getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount)
    {
        this.totalAmount = totalAmount;
    }

}
