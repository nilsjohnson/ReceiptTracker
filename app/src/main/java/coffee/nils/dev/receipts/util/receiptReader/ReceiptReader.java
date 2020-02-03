package coffee.nils.dev.receipts.util.receiptReader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Patterns;
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

import static coffee.nils.dev.receipts.util.receiptReader.IdentifierType.DOMAIN;
import static coffee.nils.dev.receipts.util.receiptReader.IdentifierType.FIRST_LINE;
import static coffee.nils.dev.receipts.util.receiptReader.IdentifierType.TELPHONE;


public class ReceiptReader
{
    // todo, get use R.id
    private static final String NO_NAME_FOUND_TEXT = "Please Enter Store Name";
    private static String TAG = "ReceiptReader";
    public static final int KEY_MAX_LENGTH = 20;
    public static final int KEY_MIN_LENGTH = 3;

    private Bitmap receiptImage;
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

    // if an identifier gives us a store name, we add it to this hashmap
    // as <storeName, times identified>
    HashMap<String, Integer> possibleStoreNames = new HashMap<>();
    int highestNumMappings = 0;

    public ReceiptReader(Bitmap image, Context context)
    {
        this.dao = DAO.get(context);
        this.receiptImage = image;
        textRecognizer = new TextRecognizer.Builder(context).build();
        this.parse();
    }

    /**
     * reads every line of the receipt saving each identifier
     */
    private void parse()
    {
        // for iterating over text
        String curBlockText;
        String[] lines;

        // for matching total currency amount
        String totalAmntPattern_str = "([0-9]*['.'][0-9][0-9])";
        Pattern totalAmountPattern = Pattern.compile(totalAmntPattern_str);

        // for matching dates
        String datePattern_str = "(\\d\\d?(\\/)\\d\\d?(\\/)(\\d{4}|\\d{2}))|\\d\\d?(-)\\d\\d?(-)(\\d{4}|\\d{2})";
        Pattern datePattern = Pattern.compile(datePattern_str);

        // for matching a website
        Pattern domainPattern = Patterns.DOMAIN_NAME;

        // patern for matching a phone number
        String phonePattern_str = "(\\(?\\d{3}\\)?(\\s+|-)?\\d{3}(\\-|\\s+)\\d{4})|(\\(?\\d{3}\\)?(\\s+|-)\\d{3}(\\-|\\s+)?\\d{4})";
        Pattern phonePattern = Pattern.compile(phonePattern_str);

        // pattern for finding "thank you for shopping at..."
        String thankYouPattern_str = "(Thank(s)?\\s+(you)?\\s+For\\s+Shopping(\\s+At)?)";
        Pattern thankYouPattern = Pattern.compile(thankYouPattern_str, Pattern.CASE_INSENSITIVE);

        // read the receipt into TextBlocks
        Frame imageFrame = new Frame.Builder().setBitmap(this.receiptImage).build();
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
                        // if this is a date like 12/12/20 or 12-12-20
                        if(date_str.matches("(\\d\\d?(\\/)\\d\\d?(\\/)(\\d{2}))|\\d\\d?(-)\\d\\d?(-)(\\d{2})"))
                        {
                            // make it like 12/12/2020
                            StringBuilder sb = new StringBuilder(date_str.replace('-', '/'));
                            char last = sb.charAt(sb.length() - 1);
                            char secLast = sb.charAt(sb.length() - 2);
                            // we assume these receipts are from the 21 century
                            sb.setCharAt(sb.length()-2, '2');
                            sb.setCharAt(sb.length()-1, '0');
                            sb.append(secLast);
                            sb.append(last);
                            date_str = sb.toString();

                        }

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


                // a number of urls on receipts are in the form of www. example .com for some reason
                // so we remove spaces after www. and before .com
                // also we make it lower case all lower case for fewer errors
                String urlLine = line.toLowerCase();
                urlLine = urlLine.replace("www. ", "www.");
                urlLine = urlLine.replace(" .com", ".com");

                // if there is any space in the middle of a line, we remove it
                if(urlLine.contains("www.") && urlLine.contains(".com"))
                {
                    startChar = urlLine.lastIndexOf("www.") + 4;
                    endChar = urlLine.lastIndexOf(".com");
                    String temp = urlLine.substring(startChar, endChar);
                    String temp2 = temp.replaceAll(" ", "");
                    urlLine = urlLine.replace(temp, temp2);
                }
                // check for a url
                Matcher domainMatcher = domainPattern.matcher(urlLine);
                while (domainMatcher.find())
                {
                    startChar = domainMatcher.start();
                    endChar = domainMatcher.end();

                    String domain = urlLine.substring(startChar, endChar);
                    this.urlList.add(domain);
                }

                // check for "thank you for shopping at"
                Matcher thankYouMatcher = thankYouPattern.matcher(line);
                if(thankYouMatcher.find())
                {
                    endChar = thankYouMatcher.end();
                    int lastChar = line.length();

                    // The line must be sufficiently long to likely be a store name
                    if(lastChar - endChar > 2)
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

        printResults();

        // go though all identifiers and see what mappings we get
        findStoreName();

        // if storeName is null, that means we dont know what it is, so we have to guess
        if(this.storeName == null)
        {
            guessStoreName();
        }
    }

    /**
     * for debugging
     */
    private void printResults()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("First Line: " + firstLine + "\n");
        sb.append("Domains:\n");
        for(String str : urlList)
        {
            sb.append("\t" + str +"\n");
        }
        sb.append("Phone Numbers:\n");
        for(String str : phoneNumList)
        {
            sb.append("\t" + str + "\n");
        }
        sb.append("After Thank-you: " + this.textAfterThankYou);

        Log.d(TAG, "\n" + sb.toString());

    }

    /**
     * goes over each identifier and see's if it has a storeName associated
     */
    private void findStoreName()
    {
        addPossibleStore(dao.getStoreByKey(toKey(this.firstLine, FIRST_LINE)));
        addPossibleStore(dao.getStoreByKey(toKey(this.textAfterThankYou, TELPHONE)));
        for (String url : urlList)
        {
            addPossibleStore(dao.getStoreByKey(toKey(url, DOMAIN)));
        }
        for (String phoneNum : phoneNumList)
        {
            addPossibleStore(dao.getStoreByKey(toKey(phoneNum, TELPHONE)));
        }
    }

    /**
     * if we couldnt identify the store name, we guess it
     */
    private void guessStoreName()
    {
        // first choice
        if(this.textAfterThankYou != null)
        {
            this.storeName = getStoreNameFormatted(this.textAfterThankYou);
        }
        // second choice
        else if(this.firstLine != null)
        {
            this.storeName = getStoreNameFormatted(this.firstLine);
        }
        // third choice
        else if(this.urlList.size() > 0)
        {
            this.storeName = getStoreNameFormatted(this.urlList.get(0));
        }
        // if we have no idea...
        else
        {
            this.storeName = NO_NAME_FOUND_TEXT;
        }
    }

    /**
     * @param storeName Name of a store that could likely be where the receipt is from
     */
    private void addPossibleStore(String storeName)
    {
        if(storeName == null)
        {
            return;
        }

        // get the number of times we've identified this store
        Integer freqency = this.possibleStoreNames.get(storeName);

        // if this store was not in the map, add it w/ frequency of 1
        if(freqency == null)
        {
            freqency = 1;
            this.possibleStoreNames.put(storeName, freqency);
        }
        // if it was, update the frequency
        else
        {
            this.possibleStoreNames.put(storeName, ++freqency);
        }

        // if this store occurs the most
        if(freqency > this.highestNumMappings)
        {
            this.storeName = storeName;
            this.highestNumMappings = freqency;
        }
    }

    /**
     * @returns the highest dollar successfully parsed from receipt
     */
    public double getTotalAmount()
    {
        return this.highestDollarAmt;
    }

    /**
     * @returns what we think the store name is
     */
    public String getStoreName()
    {
        return this.storeName;
    }

    /**
     * @param storeName What the user says the store is named
     */
    public void setCorrectStoreName(String storeName)
    {
        this.storeName = storeName;
    }

    /**
     * Call this method to save receipt identifiers after the user validates it.
     */
    public void resolve()
    {
        Log.d(TAG, "Remembering " + toKey(this.firstLine, FIRST_LINE) + " as " + this.storeName);
        dao.addStoreNameKvPair(toKey(firstLine, FIRST_LINE), this.storeName);

        for(String url : this.urlList)
        {
            Log.d(TAG, "Remembering " + toKey(url, DOMAIN) + " as " + this.storeName);
            dao.addStoreNameKvPair(toKey(url, DOMAIN), this.storeName);
        }
        for(String phoneNum : this.phoneNumList)
        {
            Log.d(TAG, "Remembering " + toKey(phoneNum, TELPHONE) + " as " + this.storeName);
            dao.addStoreNameKvPair(toKey(phoneNum, TELPHONE), this.storeName);
        }
    }

    /**
     * @returns the first date found on receipt
     */
    public Date getDate()
    {
        if (this.purchaseDate != null)
        {
            return this.purchaseDate;
        }

        return new Date();
    }


    /**
     * We convert 'identifiers' to 'keys' to get less errors. Example: Due to an unusual font,
     * Costco's (a store) first line often parses as "cosTCO", "CosTCo" or other variants. This
     * method increases the chances of successfully matching an identifier to a store name by
     * making them more uniform and eliminating things that could be common errors.
     *
     * @param identifier The value that needs to be coverted to a key
     * @param type The type of IdentifierType (i.e. url, phone, first line) for this identifier
     * @return the identifer, as a key to be used in the store name look-up table
     */
    private String toKey(String identifier, IdentifierType type)
    {

        if(identifier == null)
        {
            return null;
        }

        switch (type)
        {
            case FIRST_LINE:
                // all upper-case, alphanumeric
                identifier = identifier.toUpperCase().replaceAll("[^A-Z0-9]", "");
                break;

            case DOMAIN:
                // all lower case
                identifier = identifier.toLowerCase();
                break;

            case TELPHONE:
                // just numbers
                identifier = identifier.replaceAll("[^0-9]", "");
                break;

            case TEXT_AFTER_THANK_YOU:
                identifier = getStoreNameFormatted(identifier);


            default:
                Log.d(TAG, "No coversion for " + type.toString());
        }

        // if key is in proper length range
        if(identifier.length() > KEY_MIN_LENGTH && identifier.length() < KEY_MAX_LENGTH)
        {
            return identifier;
        }
        // key is too long
        else if(identifier.length() > KEY_MAX_LENGTH)
        {
            return identifier.substring(0, KEY_MAX_LENGTH);
        }
        // key is too short
        else
        {
            return null;
        }
    }

    /**
     *
     * @param storeName A string of text that is (hopefully) the name of a store
     * @return The name of the store with the letters of first words capitalized.
     */
    private String getStoreNameFormatted(String storeName)
    {
        if(storeName == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder(storeName.trim());

        for(int i = 0; i < sb.length(); i++)
        {
            // if its the first character
            if(i == 0)
            {
                sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
            }
            // if its after the first character, and the previous character is a space
            else if(i > 0 && sb.charAt(i-1) == ' ')
            {
                sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
            }
            // if its a character in the middle of a word
            else
            {
                sb.setCharAt(i, Character.toLowerCase(sb.charAt(i)));
            }
        }

        return sb.toString();
    }
}







