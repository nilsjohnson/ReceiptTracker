package coffee.nils.dev.receipts;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import coffee.nils.dev.receipts.data.ReceiptDAO;
import coffee.nils.dev.receipts.data.Receipt;
import coffee.nils.dev.receipts.data.ReceiptDBSchema;
import coffee.nils.dev.receipts.util.DateTools;
import coffee.nils.dev.receipts.util.ImageUtil;
import coffee.nils.dev.receipts.util.receiptReader.ReceiptReader;

public class ReceiptActivity extends AppCompatActivity
{
    private static final String TAG = "ReceiptActivity";
    private static final String DIALOG_DATE = "DialogDate";

    // UI components
    private ImageView imageView;
    private Toolbar toolbar;
    private EditText editTextName;
    private EditText editTextTotalAmount;
    private Button btnDate;
    private Button btnOk;
    private Button btnDelete;
    private AutoCompleteTextView autoCompleteTextViewCategory;

    // things relating to the receipt
    private ReceiptDAO receiptDAO;
    private ReceiptReader receiptReader;
    private File photoFile;
    Receipt receipt;
    boolean receiptChanged = false;
    boolean validName = true;
    boolean validAmount = true;
    boolean validDate = true;

    // To temporarily hold the values from the user input fields.
    // We dont directly update the receipt, until the user
    // presses save or the back button. This prevents the DB and the cache from
    // being out of sync.
    private String storeName;
    private double totalAmount;
    private Date date;
    private String category;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        /**
         * For handling when the user changes the receipt's name.
         */
        class NameChangeListener implements TextWatcher
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                String name = s.toString();

                if(name.length() < ReceiptDBSchema.ReceiptTable.MAX_FIELD_LENGTH_DEFAULT)
                {
                    storeName = s.toString();
                    editTextName.setBackgroundColor(Color.WHITE);
                    receiptChanged = true;
                    validName = true;
                }
                else
                {
                    validName = false;
                    if(!name.equals(""))
                    {
                        editTextName.setBackgroundColor(Color.RED);
                    }
                    else
                    {
                        editTextName.setBackgroundColor(Color.WHITE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            { }
        }

        // for handling when user changes the total receipt amount
        class UpdateTotalAmountHandler implements TextWatcher
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                // try to parse a double from the string
                try
                {
                    String value_str = s.toString().trim();
                    // allows dollar signs at start, but strips them for further validation
                    if(value_str.charAt(0) == '$') // will throw StringIndexOutOfBoundsException if string is empty
                    {
                        value_str = value_str.replace("$", "");
                    }

                    double value = Double.parseDouble(value_str);
                    totalAmount = value;
                    editTextTotalAmount.setBackgroundColor(Color.WHITE);
                    receiptChanged = true;
                    validAmount = true;
                }
                catch (Exception e) // Catches out of bound or number format exception
                {
                    if (s.toString().equals("") || s.toString().equals("$"))
                    {
                        editTextTotalAmount.setBackgroundColor(Color.WHITE);
                    }
                    else
                    {
                        editTextTotalAmount.setBackgroundColor(Color.RED);
                    }

                    validAmount = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            { }
        }

        // for when the user clicks the button to change's the receipts date
        class ChooseDateHandler implements View.OnClickListener, DatePicker.OnDateChangedListener
        {
            @Override
            public void onClick(View v)
            {
                FragmentManager manager = getSupportFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(date, this);
                dialog.show(manager, DIALOG_DATE);
            }

            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                Calendar cal = Calendar.getInstance();
                cal.set(year, monthOfYear, dayOfMonth, 12, 0, 0);
                Date d = cal.getTime();
                date = d;
                receiptChanged = true;
                putDateOnButton(d);
            }
        }

        // for when the user presses OK
        class OkButtonListener implements View.OnClickListener
        {
            @Override
            public void onClick(View v)
            {
                saveReceipt();
            }
        }

        class DeleteButtonListener implements View.OnClickListener
        {
            @Override
            public void onClick(View v)
            {
                deleteReceipt();
            }
        }

        class UpdateCategoryListener implements TextWatcher, AdapterView.OnItemClickListener
        {
            private ArrayAdapter<String> adapter;

            public UpdateCategoryListener(Context context, ArrayList<String> categories)
            {
                adapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, categories);
                autoCompleteTextViewCategory.setAdapter(adapter);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                category = s.toString().trim();
                receiptChanged = true;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                // hides keyboard after making choice
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(arg1.getApplicationWindowToken(), 0);
            }
        }

        // set the view and get the ReceiptDAO
        receiptDAO = ReceiptDAO.getInstance(this.getApplicationContext());
        setContentView(R.layout.activity_receipt);

        // get the reciept
        UUID receiptId = (UUID) getIntent().getSerializableExtra(MainActivity.EXTRA_NEW_RECEIPT_ID);
        receipt = receiptDAO.getReceiptById(receiptId);

        // get the image
        photoFile = receiptDAO.getPhotoFile(receipt);
        // if there is no photo, we assume the user canceled while taking the photo
        // so we delete this receipt and go back to the calling activity
        if(!photoFile.exists())
        {
            Log.d(TAG, "Photo file not found. Deleting receipt: " + receiptId.toString());
            receiptDAO.deleteReceipt(receiptId);
            receipt = null;
            finish();
            return;
        }
        Bitmap bitmap = ImageUtil.getScaledBitmap(photoFile.getPath(), this);

        // set the image
        if(!receipt.hasBeenReviewd())
        {
            // try to guess values;
            receiptReader = new ReceiptReader(bitmap, this);
            receipt.setStoreName(receiptReader.getStoreName());
            receipt.setTotalAmount(receiptReader.getTotalAmount());
            receipt.setDate(receiptReader.getPurchaseDate());

            Toast toast = Toast.makeText(getApplicationContext(),"Please Review This Information", Toast.LENGTH_SHORT);
            toast.show();

            // try to figure out the category
            receipt.setCategory(receiptDAO.getCategory(receiptReader.getStoreName()));
        }

        // initialize these so they get saved correctly if not changed
        storeName = receipt.getStoreName();
        date = receipt.getDate();
        totalAmount = receipt.getTotalAmount();
        category = receipt.getCategory();

        // set the toolbar
        toolbar = findViewById(R.id.toolbar_receipt);
        setSupportActionBar(toolbar);

        // set the ImageView
        imageView = (ImageView) findViewById(R.id.imageView_receipt);
        photoFile = receiptDAO.getPhotoFile(receipt);
        imageView.setImageBitmap(bitmap);

        // set the name related things
        editTextName = (EditText) findViewById(R.id.editText_name);
        editTextName.setText(receipt.getStoreName());
        editTextName.addTextChangedListener(new NameChangeListener());

        // set the date related things
        btnDate = (Button) findViewById(R.id.button_date);
        btnDate.setOnClickListener(new ChooseDateHandler());
        putDateOnButton(date);

        // set the autocompleteEditText
        autoCompleteTextViewCategory = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
        autoCompleteTextViewCategory.setText(category);
        UpdateCategoryListener listener = new UpdateCategoryListener(this, receiptDAO.getCategoryList());
        autoCompleteTextViewCategory.addTextChangedListener(listener);
        autoCompleteTextViewCategory.setOnItemClickListener(listener);
        autoCompleteTextViewCategory.setThreshold(0);

        // set the receipt's total-amount related things
        double totalAmount = receipt.getTotalAmount();
        DecimalFormat df = new DecimalFormat("##.00");
        String rounded = df.format(totalAmount);
        editTextTotalAmount = (EditText) findViewById(R.id.editText_amount);
        editTextTotalAmount.setText("$" + rounded);
        editTextTotalAmount.addTextChangedListener(new UpdateTotalAmountHandler());

        // for the OK button
        btnOk = (Button) findViewById(R.id.button_ok);
        btnOk.setOnClickListener(new OkButtonListener());

        // for the delete button
        btnDelete = (Button) findViewById(R.id.button_delete);
        btnDelete.setOnClickListener(new DeleteButtonListener());
    }

    @Override
    public void onBackPressed()
    {
        if (receiptChanged)
        {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int choice)
                {
                    switch (choice)
                    {
                        // if the user wants to save the changes
                        case DialogInterface.BUTTON_POSITIVE:
                            saveReceipt();
                            break;
                        // if the user made accidental changes
                        case DialogInterface.BUTTON_NEGATIVE:
                            Toast toast = Toast.makeText(getApplicationContext(), R.string.no_changes, Toast.LENGTH_SHORT);
                            toast.show();
                            finish();
                            break;
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.are_you_sure).setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.no, dialogClickListener).show();
        }
        else
        {
            // if the user looked at this receipt for the first time, then hit the back button, w/out
            // making any changes, we assume it's OK.
            if(receipt.hasBeenReviewd() == false)
            {
                receipt.setHasBeenReviewed(true);
                saveReceipt();
            }
            finish();
        }
    }

    /**
     * In the event this gets called, whatever valid information entered will be saved.
     * This means that the ReceiptReader will parse the receipt again if receipt.hasBeenReviewed()
     * is false. Calling this method, however, ensures that receipts, won't be in the database with
     * empty fields. (ex: Its better to have an incorrect store name than an empty string from a UI
     * standpoint)
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "On Destroy Called");
        // this is to save unreviewed info to the db if the user closes the app
        // the receipt will still be read again and resolved next time they view it.
        if(receipt != null && !receipt.hasBeenReviewd())
        {
            writeUpdate();
        }
    }

    private void putDateOnButton(Date d)
    {
        btnDate.setText(DateTools.toSimpleFormat(d));
    }

    private void saveReceipt()
    {
        // if this was the users first time looking at this receipt, mark it as read and
        // "teach" this receipt to the reader
        if(!receipt.hasBeenReviewd())
        {
            receipt.setHasBeenReviewed(true);
            receiptChanged = true;
            receiptReader.setCorrectStoreName(editTextName.getText().toString());
            receiptReader.resolve();
        }

        if(receiptChanged)
        {
            // these must be valid before saving.
            if(validAmount && validName && validDate)
            {
                // update the receipt
                Toast toast = Toast.makeText(getApplicationContext(),R.string.saved,Toast. LENGTH_SHORT);
                writeUpdate();
                toast.show();
                finish();
            }
            // notify the user to fix their errors
            else
            {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int choice)
                    {
                        switch (choice)
                        {
                            case DialogInterface.BUTTON_POSITIVE:
                                if (!validAmount)
                                {
                                    editTextTotalAmount.setBackgroundColor(Color.RED);
                                }
                                if (!validName)
                                {
                                    editTextName.setBackgroundColor(Color.RED);
                                }
                                if(!validDate)
                                {
                                    // TODO..if we decide to validate dates.
                                }
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.check_fields).setPositiveButton(R.string.ok, dialogClickListener).show();
            }
        }
        // the user pressed 'ok' without making any changes
        else
        {
            finish();
        }
    }

    /**
     * Saves current input to the database
     */
    private void writeUpdate()
    {
        // receipt may be null if it was deleted.
        if(receipt != null)
        {
            receipt.setTotalAmount(totalAmount);
            receipt.setStoreName(storeName);
            receipt.setDate(date);
            receipt.setCategory(category);
            receiptDAO.updateReceipt(receipt);
        }
    }

    /**
     * Deletes this receipt from the database and finishes the activity.
     */
    private void deleteReceipt()
    {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int choice)
            {
                switch (choice)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        receiptDAO.deleteReceipt(receipt.getId());
                        receipt = null;
                        Toast toast = Toast.makeText(getApplicationContext(), R.string.receipt_deleted, Toast.LENGTH_SHORT);
                        toast.show();
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:

                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.are_you_sure_delete).setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.receipt_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_share)
        {
            Uri imageUri = FileProvider.getUriForFile(this, receiptDAO.FILES_AUTHORITY, photoFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.setType("image/jpeg");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getResources().getText(R.string.send_to)));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data)
    {
        super.onActivityResult(reqCode, resultCode, data);
    }
}
