package coffee.nils.dev.receipts.data;

import java.util.Date;

public class Filter
{
    public Filter(Date start, Date end)
    {
        this.startDate = start;
        this.endDate = end;
    }

    public Date startDate;
    public Date endDate;
}
