package coffee.nils.dev.receipts.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

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

    private Bitmap image;
    private TextRecognizer textRecognizer;
    private DAO dao;

    // for storing the storeName in the HashTable
    private String firstLine;

    // For holding receipt values
    private String storeName;
    private double highestDollarAmt;
    private Date purchaseDate;

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

        // for matching dates
        String datePattern_str = "(([0-9][0-9]|[0-9])['/']([0-9][0-9]|[0-9])['/']([0-9][0-9][0-9][0-9]|[0-9][0-9]))";
        Pattern totalAmountPattern = Pattern.compile(totalAmntPattern_str);
        Pattern datePattern = Pattern.compile(datePattern_str);

        // read the receipt into TextBlocks
        Frame imageFrame = new Frame.Builder().setBitmap(this.image).build();
        SparseArray<TextBlock> textBlocks = this.textRecognizer.detect(imageFrame);

        // iterate over TextBlocks looking for information
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
            curBlockText = textBlock.getValue();
            lines = curBlockText.split("\n");

            // if this is not the first block
            if(i > 0)
            {
                for(String line : lines)
                {
                    // check for dollar amounts
                    Matcher totalAmountMatcher = totalAmountPattern.matcher(line);
                    while(totalAmountMatcher.find())
                    {
                        int startChar = totalAmountMatcher.start();
                        int endChar = totalAmountMatcher.end();
                        String dollarAmnt_str = line.substring(startChar, endChar);
                        try
                        {
                            double curAmnt = Double.parseDouble(dollarAmnt_str);
                            if(curAmnt > this.highestDollarAmt)
                            {
                                this.highestDollarAmt = curAmnt;
                            }
                        }
                        catch(Exception e)
                        {
                            Log.d(TAG, e.getMessage());
                        }
                    }

                    // check for dates - take the first date found
                    Matcher dateMatcher = datePattern.matcher(line);
                    while(dateMatcher.find())
                    {
                        int startChar = dateMatcher.start();
                        int endChar = dateMatcher.end();

                        String date_str = line.substring(startChar, endChar);
                        Log.d(TAG, "Date Found: " + date_str);

                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                        try
                        {
                            cal.setTime(sdf.parse(date_str));
                        }
                        catch (ParseException e)
                        {
                            e.printStackTrace();
                        }

                        this.purchaseDate = cal.getTime();
                        break;
                    }
                }
            }
            // if it is the first block
            else if(i == 0)
            {
                // get the first block..
                String firstBlock = textBlocks.get(textBlocks.keyAt(i)).getValue();
               // Log.d(TAG, "First block of text on receipt:\n" + firstBlock);

                // get the first line
                try
                {
                    this.firstLine = firstBlock.substring(0, firstBlock.indexOf('\n'));
                }
                catch(StringIndexOutOfBoundsException e)
                {
                    Log.d(TAG, " The first block of text didnt not contain a new line character. Taking the entire first block line as the first line.\n" + e.getMessage());
                    this.firstLine = firstBlock;
                }

                // if the firstLine is the hash of a known store
                if(dao.getStoreNameByFirstLine(firstLine) != null)
                //if(storeMap.get(firstLine) != null)
                {
                    Log.d(TAG, "This is a known receipt for " + dao.getStoreNameByFirstLine(firstLine) + "!");
                    this.storeName = dao.getStoreNameByFirstLine(firstLine);
                    this.resolved = true;
                }
                // take the first line as the store name
                else
                {
                    this.storeName = firstLine;
                    this.resolved = false;
                }
            }
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
        if(!this.resolved)
        {
            Log.d(TAG, "Remembering " + this.firstLine + " as " + this.storeName);
            dao.addStoreNameFirstLine(this.firstLine, this.storeName);
            // storeMap.put(this.firstLine, this.storeName);
        }
        else
        {
            Log.d(TAG, "This firstLine was already in storeMap, so we dont need to learn it :)");
        }
    }

    public Date getDate()
    {
        if(this.purchaseDate != null)
        {
            return this.purchaseDate;
        }

        return new Date();
    }
}







