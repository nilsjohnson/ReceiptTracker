package coffee.nils.dev.receipts.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Patterns;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import coffee.nils.dev.receipts.data.DAO;

/**
 * This class maintains a static HashMap<k,v> where k is the text of the first block of text of a
 * receipt, and v is the name of the store, as confirmed by a user. When a receipt is parsed, we
 * use the HashMap to see if we know the store name.
 */
public class ReceiptReader
{
    private static String TAG = "ReceiptReader";
    public static final int STORE_NAME_KEY_MAX_LENGTH = 10;
    private Bitmap image;
    private TextRecognizer textRecognizer;
    private DAO dao;

    // values we "guess"
    private String storeName;
    private double highestDollarAmt;
    private Date purchaseDate;

    // for lookup to identify store name
    private String firstLine;
    private ArrayList<String> urlList = new ArrayList<>();
    private ArrayList<String> phoneNumList = new ArrayList<>();

    // to see what possible stores this belongs to
    HashMap<String, Integer> foundStores = new HashMap<>();

    // every receipt we read, we either get the storeName from storeMap,
    // or we parse to find it. If we parse it, we flag it as !resolved until
    // the user confirms the actual storeName, then we remember.
    private boolean resolved = false;

    public ReceiptReader(Bitmap image, Context context)
    {
        this.dao = DAO.get(context);
        this.image = image;
        textRecognizer = new TextRecognizer.Builder(context).build();
        this.parse();
    }

    private void parse()
    {
        // for iterating over text
        String curBlockText;
        String[] lines;

        // for matching total currency amount
        String totalAmntPattern_str = "([0-9]*['.'][0-9][0-9])";
        Pattern totalAmountPattern = Pattern.compile(totalAmntPattern_str);

        // for matching dates
        String datePattern_str = "(([0-9][0-9]|[0-9])['/']([0-9][0-9]|[0-9])['/']([0-9][0-9][0-9][0-9]|[0-9][0-9]))";
        Pattern datePattern = Pattern.compile(datePattern_str);

        // for matching a website
        Pattern urlPattern = Patterns.WEB_URL;

        // patern for matching a phone number
        String phonePattern_str = "(\\(?\\d\\d\\d\\)?(\\s|-)\\d\\d\\d[\\-|\\s]\\d\\d\\d\\d)";
        Pattern phonePattern = Pattern.compile(phonePattern_str);

        // read the receipt into TextBlocks
        Frame imageFrame = new Frame.Builder().setBitmap(this.image).build();
        SparseArray<TextBlock> textBlocks = this.textRecognizer.detect(imageFrame);

        // iterate over TextBlocks looking for information
        for (int i = 0; i < textBlocks.size(); i++)
        {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
            curBlockText = textBlock.getValue();
            lines = curBlockText.split("\n");

            // for start and end indices of identifiers
            int startChar, endChar;

            for (String line : lines)
            {
                // check for dollar amounts
                Matcher totalAmountMatcher = totalAmountPattern.matcher(line);
                while (totalAmountMatcher.find())
                {
                    startChar = totalAmountMatcher.start();
                    endChar = totalAmountMatcher.end();
                    String dollarAmnt_str = line.substring(startChar, endChar);
                    try
                    {
                        double curAmnt = Double.parseDouble(dollarAmnt_str);
                        if (curAmnt > this.highestDollarAmt)
                        {
                            this.highestDollarAmt = curAmnt;
                        }
                    } catch (Exception e)
                    {
                        Log.d(TAG, e.getMessage());
                    }
                }

                // check for dates if we haven't found one yet
                if(this.purchaseDate == null)
                {
                    Matcher dateMatcher = datePattern.matcher(line);
                    while (dateMatcher.find())
                    {
                        startChar = dateMatcher.start();
                        endChar = dateMatcher.end();

                        String date_str = line.substring(startChar, endChar);
                        Log.d(TAG, "Date Found: " + date_str);

                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                        try
                        {
                            cal.setTime(sdf.parse(date_str));
                            this.purchaseDate = cal.getTime();
                        }
                        catch (ParseException e)
                        {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }


                // a lot of urls for some reason on receipts are in the form of www. example .com
                // so we remove spaces after www. and before .com
                String urlLine = line.replace("www. ", "www.");
                urlLine = urlLine.replace(" .com", ".com");
                // check for a url
                Matcher urlMatcher = urlPattern.matcher(urlLine);
                while (urlMatcher.find())
                {
                    startChar = urlMatcher.start();
                    endChar = urlMatcher.end();

                    String url = urlLine.substring(startChar, endChar);
                    // URI class requires a protocol
                    if(!url.startsWith("http")) // this also gets https
                    {
                        url = "http://" + url;
                    }

                    Log.d(TAG, "found url: " + url);
                    String domain;
                    try
                    {
                        URL uri = new URL(url);
                        domain = uri.getHost();
                        this.urlList.add(domain.startsWith("www.") ? domain.substring(4) : domain);
                    }
                    catch (MalformedURLException e)
                    {
                        Log.e(TAG, "Problem with URL " + "'" + url + "', " + e.getMessage());
                    }
                }

                // check for a phone number
                Matcher phoneMatcher = phonePattern.matcher(line);
                while(phoneMatcher.find())
                {
                    startChar = phoneMatcher.start();
                    endChar = phoneMatcher.end();
                    phoneNumList.add(line.substring(startChar, endChar));
                }

            }
            // if it is the first block. Often, the first line is the name of the store
            if (i == 0)
            {
                // get the first line
                try
                {
                    this.firstLine = curBlockText.substring(0, curBlockText.indexOf('\n'));
                }
                catch (StringIndexOutOfBoundsException e)
                {
                    Log.d(TAG, " The first block of text didnt not contain a new line character. Taking the entire first block line as the first line.\n" + e.getMessage());
                    this.firstLine = curBlockText;
                }
            }
        }

        if(storeNameIsIdentifiable(this.firstLine))
        {
            addPossibleStore(dao.getStoreByKey(toKey(this.firstLine)));
        }
        if(urlList != null)
        {
            for(String url: urlList)
            {
                addPossibleStore(dao.getStoreByKey(toKey(url)));
            }
        }
        if(phoneNumList != null)
        {
            for(String phoneNum : phoneNumList)
            {
                addPossibleStore(dao.getStoreByKey(toKey(phoneNum)));
            }
        }

        setMostLikelyStoreName();
        if(this.storeName == null)
        {
            if(this.urlList.size() > 0)
            {
                this.storeName = urlList.get(0);
            }
            else if(this.firstLine != null)
            {
                this.storeName = firstLine;
            }
            else
            {
                this.storeName = "Store Name";
            }
        }
    }

    private void setMostLikelyStoreName()
    {
        String bestStore = null;
        int numOccurrences = 0;

        for(String storeName: foundStores.keySet())
        {
            if(numOccurrences < foundStores.get(storeName))
            {
                numOccurrences = foundStores.get(storeName);
                bestStore = storeName;
            }
        }

        if(bestStore != null)
        {
            this.storeName = bestStore;
        }

    }

    private void addPossibleStore(String storeNAme)
    {
        if(storeNAme == null)
        {
            return; //
        }

        Integer numOccurrances = this.foundStores.get(storeNAme);

        if(numOccurrances == null)
        {
            this.foundStores.put(storeNAme, 1);
        }
        else
        {
            this.foundStores.put(storeNAme, ++numOccurrances);
        }
    }

    private boolean storeNameIsIdentifiable(String str)
    {
        if(dao.getStoreByKey(toKey(str)) != null)
        {
            return true;
        }
        return false;
    }

    public double getTotalAmount()
    {
        return this.highestDollarAmt;
    }

    public String getStoreName()
    {
        return this.storeName;
    }

    public void setCorrectStoreName(String storeName)
    {
        this.storeName = storeName;
    }

    public void resolve()
    {
        Log.d(TAG, "Remembering " + toKey(this.firstLine) + " as " + this.storeName);
        dao.addStoreNameKvPair(toKey(firstLine), this.storeName);

        for(String url : this.urlList)
        {
            Log.d(TAG, "Remembering " + toKey(url) + " as " + this.storeName);
            dao.addStoreNameKvPair(toKey(url), this.storeName);
        }
        for(String phoneNum : this.phoneNumList)
        {
            Log.d(TAG, "Remembering " + toKey(phoneNum) + " as " + this.storeName);
            dao.addStoreNameKvPair(toKey(phoneNum), this.storeName);
        }
    }

    public Date getDate()
    {
        if (this.purchaseDate != null)
        {
            return this.purchaseDate;
        }

        return new Date();
    }

    /**
     * @return The key value for the storeMap. Keys must:
     * -have no whitespace
     * -be uppercase
     * -no punctuation
     * Example: Forever 21 --> FOREVER
     * Stop And Shop --> STOPANDSHOP
     * O'Reilly Auto Parts --> OREILLYAUTOPARTS
     *
     */
    private String toKey(String str)
    {
        String key = str
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");

        try
        {
            return key.substring(0, STORE_NAME_KEY_MAX_LENGTH);
        }
        catch (StringIndexOutOfBoundsException e)
        {
            Log.d(TAG, "Key was already shorter than " + STORE_NAME_KEY_MAX_LENGTH + ". Will not shorten");
        }

        return key;
    }

}







