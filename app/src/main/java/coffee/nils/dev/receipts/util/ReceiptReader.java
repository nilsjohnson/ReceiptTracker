package coffee.nils.dev.receipts.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.util.Patterns;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.net.MalformedURLException;
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


public class ReceiptReader
{
    private static final String NO_NAME_FOUND_TEXT = "Please Enter Store Name";
    private static String TAG = "ReceiptReader";
    public static final int STORE_NAME_KEY_MAX_LENGTH = 10;
    private Bitmap image;
    private TextRecognizer textRecognizer;
    private DAO dao;

    // values we want to find for the user
    private String storeName;
    private double highestDollarAmt;
    private Date purchaseDate;

    // identifiers we pull from the receipt
    private String firstLine;
    private ArrayList<String> urlList = new ArrayList<>();
    private ArrayList<String> phoneNumList = new ArrayList<>();
    private String textAfterThankYou;

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
        String phonePattern_str = "(\\(?\\d\\d\\d\\)?(\\s+|-)\\d\\d\\d[\\-|\\s]+\\d\\d\\d\\d)";
        Pattern phonePattern = Pattern.compile(phonePattern_str);

        // pattern for finding "thank you for shopping at..."
        String thankYouPattern_str = "(Thank(s)?\\s+(you)?\\s+For\\s+Shopping(\\s+At)?)";
        Pattern thankYouPattern = Pattern.compile(thankYouPattern_str, Pattern.CASE_INSENSITIVE);

        // read the receipt into TextBlocks
        Frame imageFrame = new Frame.Builder().setBitmap(this.image).build();
        SparseArray<TextBlock> textBlocks = this.textRecognizer.detect(imageFrame);

        // iterate over TextBlocks looking for information
        for (int blockNum = 0; blockNum < textBlocks.size(); blockNum++)
        {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(blockNum));
            curBlockText = textBlock.getValue();
            lines = curBlockText.split("\n");

            // for start and end indices of identifiers
            int startChar, endChar;

            for(int lineNum = 0; lineNum < lines.length; lineNum++)
            {
                String line = lines[lineNum];
                Log.d(TAG, line);

                // if this is the very first line of the receipt
                if(blockNum == 0 && lineNum == 0)
                {
                    this.firstLine = line;
                }

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
                    }
                    catch (Exception e)
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

                // check for "thank you for shopping at"
                Matcher thankYouMatcher = thankYouPattern.matcher(line);
                if(thankYouMatcher.find())
                {
                    endChar = thankYouMatcher.end();
                    int lastChar = line.length();

                    // The line must be sufficiently long to likely be a store name
                    if(lastChar - endChar > 5)
                    {
                        this.textAfterThankYou = line.substring(endChar, lastChar);
                    }
                    // otherwise try to peak ahead at the next line
                    else if(lineNum +1 < lines.length)
                    {
                        this.textAfterThankYou = lines[lineNum+1].trim();
                    }
                    // or the first line of the next block
                    else if(blockNum+1 < textBlocks.size())
                    {
                        this.textAfterThankYou = textBlocks.get(blockNum+1).getValue().split("\n")[0];
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
        }

        if(this.firstLine != null)
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
        if(this.textAfterThankYou != null)
        {
            addPossibleStore(this.textAfterThankYou);
        }

        setMostLikelyStoreName();

        // if we can't recover a store name from any of the receipt's identifiers, we guess
        if(this.storeName == null)
        {
            // first choice
            if(this.textAfterThankYou != null)
            {
                this.storeName = this.textAfterThankYou;
            }
            // second choice
            else if(this.firstLine != null)
            {
                this.storeName = firstLine;
            }
            // third choice
            else if(this.urlList.size() > 0)
            {
                this.storeName = urlList.get(0);
            }
            // We have no idea...
            else
            {
                this.storeName = NO_NAME_FOUND_TEXT;
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
            return;
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







