package coffee.nils.dev.receipts.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTools
{
    public static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");

    public static String toSimpleFormat(Date date)
    {
        return sdf.format(date);
    }

    public static Date setToStartOfDay(Date date)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTime();
    }

    public static Date setToEndOfDay(Date date)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);

        return c.getTime();
    }



}
