package coffee.nils.dev.receipttracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import coffee.nils.dev.receipttracker.data.DAO;
import coffee.nils.dev.receipttracker.data.Receipt;
import coffee.nils.dev.receipttracker.util.ImageUtil;

public class ReceiptActivity extends AppCompatActivity  implements DatePicker.OnDateChangedListener
{
    private static final String DIALOG_DATE = "DialogDate";

    private ImageView imageView;
    private File photoFile;

    private EditText editTextName;
    private EditText editTextTotalAmount;
    private Button btnDate;

    private DAO dao;

    Receipt receipt;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // for handling when user changes the name of where their receipt is from
        class NameChangeListener implements TextWatcher
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                receipt.setStoreName(s.toString());
                dao.updateReceipt(receipt);
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
                try
                {
                    double value = Double.parseDouble(s.toString());
                    receipt.setTotalAmount(value);
                }
                catch(NumberFormatException e)
                {
                    editTextTotalAmount.setBackgroundColor(Color.RED);
                }

            }

            @Override
            public void afterTextChanged(Editable s)
            { }
        }

        // for when the user clicks the button to change's the receipts date
        class ChooseDateHandler implements View.OnClickListener
        {
            @Override
            public void onClick(View v)
            {
                FragmentManager manager = getSupportFragmentManager();
                DatePickerFragment dialog = DatePickerFragment.newInstance(new Date(), ReceiptActivity.this);
                dialog.show(manager, DIALOG_DATE);
            }
        }

        // set the view and get the DAO
        setContentView(R.layout.activity_receipt);
        dao = DAO.get(this.getApplicationContext());

        // get the reciept
        UUID receiptId = (UUID) getIntent().getSerializableExtra(MainActivity.EXTRA_NEW_RECEIPT_ID);
        receipt = dao.getReceiptById(receiptId);

        // set the image
        imageView = (ImageView)findViewById(R.id.imageView_receipt);
        photoFile = dao.getPhotoFile(receipt);
        Bitmap bitmap = ImageUtil.getScaledBitmap(photoFile.getPath(), this);
        imageView.setImageBitmap(bitmap);

        // set the name related things
        editTextName = (EditText)findViewById(R.id.editText_name);
        editTextName.addTextChangedListener(new NameChangeListener());

        // set the date related things
        btnDate = (Button)findViewById(R.id.button_date);
        btnDate.setOnClickListener(new ChooseDateHandler());

        // set the receipt's total-amount related things
        editTextTotalAmount = (EditText) findViewById(R.id.editText_amount);
        editTextTotalAmount.addTextChangedListener(new UpdateTotalAmountHandler());

    }

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(year, monthOfYear, dayOfMonth);
        Date d = cal.getTime();
        btnDate.setText(d.toString());
        receipt.setDate(d);
    }


}
