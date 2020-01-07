package coffee.nils.dev.receipttracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;

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
    private EditText editTextDate;

    private DAO dao;

    Receipt receipt;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        dao = DAO.get(this.getApplicationContext());

        setContentView(R.layout.activity_receipt);
        imageView = (ImageView)findViewById(R.id.imageView_recepit);
        UUID receiptId = (UUID) getIntent().getSerializableExtra(MainActivity.EXTRA_NEW_RECEIPT_ID);

        receipt = dao.getReceiptById(receiptId);
        photoFile = dao.getPhotoFile(receipt);

        Bitmap bitmap = ImageUtil.getScaledBitmap(photoFile.getPath(), this);
        imageView.setImageBitmap(bitmap);

        editTextName = (EditText)findViewById(R.id.editText_name);
        editTextName.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                receipt.setStoreName(s.toString());
                dao.updateReceipt(receipt);
            }

            @Override
            public void afterTextChanged(Editable s)
            {

            }
        });

        editTextDate = (EditText)findViewById(R.id.editText_date);
        editTextDate.setOnClickListener(new ChooseDateHandler());

    }

    private void updatePhotoView()
    {
        if(photoFile == null || !photoFile.exists())
        {

            imageView.setImageDrawable(null);
        }
        else
        {
            Bitmap bitmap = ImageUtil.getScaledBitmap(photoFile.getPath(), this);
            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(year, monthOfYear, dayOfMonth);
        Date d = cal.getTime();
        editTextDate.setText(editTextDate.toString());
    }

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
}
