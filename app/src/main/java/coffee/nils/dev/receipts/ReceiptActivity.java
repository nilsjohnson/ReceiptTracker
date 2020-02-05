package coffee.nils.dev.receipts;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import coffee.nils.dev.receipts.data.DAO;
import coffee.nils.dev.receipts.data.Receipt;
import coffee.nils.dev.receipts.data.ReceiptDBSchema;
import coffee.nils.dev.receipts.util.ImageUtil;
import coffee.nils.dev.receipts.util.receiptReader.ReceiptReader;


public class ReceiptActivity extends AppCompatActivity
{
    private static final String TAG = "ReceiptActivity";
    private ReceiptReader receiptReader;

    static
    {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    public native long autoCrop(long addr);

    private static final String DIALOG_DATE = "DialogDate";

    private ImageView imageView;
    private File photoFile;

    private EditText editTextName;
    private EditText editTextTotalAmount;
    private Button btnDate;
    private Button btnOk;
    private Button btnDelete;

    private DAO dao;

    Receipt receipt;
    boolean receiptChanged = false;
    boolean validName = true;
    boolean validAmount = true;
    boolean validDate = true;

    // to hold the values from the user input fields
    // we dont directly update the receipt, until the user
    // presses save or the back button. This prevents the DB and the cache from
    // being out of sync.
    String storeName;
    double totalAmount;
    Date date;

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
                DatePickerFragment dialog = DatePickerFragment.newInstance(new Date(), this);
                dialog.show(manager, DIALOG_DATE);
            }

            // TODO validate date
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth)
            {
                Calendar cal = Calendar.getInstance();
                cal.set(year, monthOfYear, dayOfMonth);
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

        // set the view and get the DAO
        dao = DAO.get(this.getApplicationContext());
        setContentView(R.layout.activity_receipt);

        // get the reciept
        UUID receiptId = (UUID) getIntent().getSerializableExtra(MainActivity.EXTRA_NEW_RECEIPT_ID);
        receipt = dao.getReceiptById(receiptId);

        photoFile = dao.getPhotoFile(receipt);
        Bitmap bitmap = ImageUtil.getScaledBitmap(photoFile.getPath(), this);

        // set the image
        // if this is the first time showing reciept, autocrop and autofill it
        if(!receipt.hasBeenReviewd())
        {
            Toast toast= Toast.makeText(getApplicationContext(),"Autocropping and saving Image :)", Toast.LENGTH_SHORT);
            toast.show();

            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);
            long addr = autoCrop(autoCrop(mat.getNativeObjAddr()));
            Mat cropped = new Mat(addr);
            Bitmap forCropped = ImageUtil.getEmptyBitmap(cropped);
            Utils.matToBitmap(cropped, forCropped);
            bitmap = forCropped;

            // try to guess values;
            receiptReader = new ReceiptReader(bitmap, this);
            receipt.setStoreName(receiptReader.getStoreName());
            receipt.setTotalAmount(receiptReader.getTotalAmount());
            receipt.setDate(receiptReader.getPurchaseDate());

            toast = Toast.makeText(getApplicationContext(),"Please Review This Information", Toast.LENGTH_SHORT);
            toast.show();

            try
            {
                dao.saveImage(bitmap, receipt.getFileName());
            }
            catch (IOException e)
            {
                Log.e(TAG, "Problem saving resized bitmap as a jpg.\n" + e.getMessage());
            }
        }
        else
        {
            Toast toast= Toast.makeText(getApplicationContext(),"Already AutoCropped!",Toast.LENGTH_SHORT);
            toast.show();
        }

        // initialize these so they get saved correctly if not changed
        storeName = receipt.getStoreName();
        date = receipt.getDate();
        totalAmount = receipt.getTotalAmount();

        imageView = (ImageView) findViewById(R.id.imageView_receipt);
        photoFile = dao.getPhotoFile(receipt);
        imageView.setImageBitmap(bitmap);

        // set the name related things
        editTextName = (EditText) findViewById(R.id.editText_name);
        editTextName.setText(receipt.getStoreName());
        editTextName.addTextChangedListener(new NameChangeListener());

        // set the date related things
        btnDate = (Button) findViewById(R.id.button_date);
        btnDate.setOnClickListener(new ChooseDateHandler());
        putDateOnButton(date);

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
                        case DialogInterface.BUTTON_POSITIVE:
                            saveReceipt();
                            break;

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

    private void putDateOnButton(Date d)
    {
        btnDate.setText(Receipt.sdf.format(d));
    }

    private void saveReceipt()
    {
        // if this was the users first time looking at this receipt, mark it as read and
        // "teach" this receipt to the reader
        if(receipt.hasBeenReviewd() == false)
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
                receipt.setTotalAmount(totalAmount);
                receipt.setStoreName(storeName);
                receipt.setDate(date);
                // save it
                dao.updateReceipt(receipt);
                Toast toast = Toast.makeText(getApplicationContext(),R.string.saved,Toast. LENGTH_SHORT);
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
                                    // TODO
                                }
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.check_fields).setPositiveButton(R.string.ok, dialogClickListener).show();
            }
        }
    }

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
                        dao.deleteReceipt(receipt.getId());
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
}
