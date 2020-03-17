package coffee.nils.dev.receipts.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTools
{
    public static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

    public static String toSimpleFormat(Date date)
    {
        return sdf.format(date);
    }

}
