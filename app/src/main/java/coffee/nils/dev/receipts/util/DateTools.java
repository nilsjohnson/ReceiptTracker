package coffee.nils.dev.receipts.util;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTools
{
    public static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
    private static Calendar cal = Calendar.getInstance();

    public static String toSimpleFormat(Date date)
    {
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    public static Date setToStartOfDay(Date date)
    {
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    public static Date setToNoon(Date date)
    {
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
        cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
        cal.set(Calendar.MILLISECOND, cal.getMaximum(Calendar.MILLISECOND));

        return cal.getTime();
    }

    public static int getDay(Date date)
    {
        cal.setTime(date);
        return  cal.get(Calendar.DAY_OF_MONTH);
    }

    public static int getMonth(Date date)
    {
        cal.setTime(date);
        return cal.get(Calendar.MONTH);
    }

    public static int getYear(Date date)
    {
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }



    public static Date setToEndOfDay(Date date)
    {
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        return cal.getTime();
    }

    public static boolean isSameDay(Date date_1, Date date_2)
    {
        if(        getDay(date_1) == getDay(date_2)
                && getMonth(date_1) == getMonth(date_2)
                && getYear(date_1) == getYear(date_2)
                && getDay(date_1) == getDay(date_2)
                && getMonth(date_1) == getMonth(date_2)
                && getYear(date_1) == getYear(date_2))
        {
            return true;
        }
        return false;
    }
}
