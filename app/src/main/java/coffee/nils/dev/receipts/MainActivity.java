package coffee.nils.dev.receipts;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

import coffee.nils.dev.receipts.data.DAO;
import coffee.nils.dev.receipts.data.DAO;
import coffee.nils.dev.receipts.data.Receipt;

public class MainActivity extends AppCompatActivity
{
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java4");
    }

    public native String stringFromJNI();

    DAO dao;
    public final static String EXTRA_NEW_RECEIPT_ID = "coffee.nils.dev.purchasetracker.newReceiptId";
    private FloatingActionButton fabAddReceipt;

    // for the scrolling items
    private RecyclerView recyclerView;
    private ReceiptAdapter adaper;



    private class ReceiptHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private TextView textViewBusinessName;
        private TextView textViewDate;
        private TextView textViewAmount;

        private Receipt receipt;

        public ReceiptHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.list_item, parent, false));
            textViewBusinessName = (TextView) itemView.findViewById(R.id.textView_name);
            textViewDate = (TextView) itemView.findViewById(R.id.textView_date);
            textViewAmount = (TextView) itemView.findViewById(R.id.textView_amount);

            itemView.setOnClickListener(this);
        }

        public void bind(Receipt receipt)
        {
            this.receipt = receipt;
            if(receipt.getStoreName() != null)
            {
                textViewBusinessName.setText(receipt.getStoreName());
            }
            //TODO use Receipt method
            textViewAmount.setText(Double.toString(receipt.getTotalAmount()));
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
            textViewDate.setText(sdf.format(receipt.getDate()));

            //textViewName = (TextView) itemView.findViewById((R.id.textView_date));
        }

        @Override
        public void onClick(View view)
        {
            launchReceiptActivity(receipt);
        }
    }

    private class ReceiptAdapter extends RecyclerView.Adapter<ReceiptHolder>
    {
        private List<Receipt> receiptList;

        public ReceiptAdapter(List<Receipt> purchaseList)
        {
            this.receiptList = purchaseList;
        }

        @NonNull
        @Override
        public ReceiptHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
            return new ReceiptHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ReceiptHolder holder, int position)
        {
            Receipt purchase = receiptList.get(position);
            holder.bind(purchase);
        }

        @Override
        public int getItemCount()
        {
            return receiptList.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = DAO.get(getApplicationContext());

        recyclerView = (RecyclerView)findViewById(R.id.recyclerView_receipts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        fabAddReceipt = (FloatingActionButton) findViewById((R.id.fab_add_receipt));
        fabAddReceipt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final Intent captureImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                Receipt receipt = dao.createReceipt();
                File photoFile = dao.getPhotoFile(receipt);

                Uri uri = FileProvider.getUriForFile(getApplicationContext(), "coffee.nils.dev.receipts.fileprovider", photoFile);
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, uri);

                List<ResolveInfo> cameraActivities = getApplicationContext().getPackageManager().queryIntentActivities(captureImage, PackageManager.MATCH_DEFAULT_ONLY);

                for (ResolveInfo activity: cameraActivities)
                {
                    getApplicationContext().grantUriPermission(activity.activityInfo.packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                launchReceiptActivity(receipt);
                startActivity(captureImage);

            }
        });

        updateUI();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        updateUI();
    }

    private void updateUI()
    {
        List<Receipt> receiptList = dao.getReceiptList();
        adaper = new ReceiptAdapter((receiptList));
        recyclerView.setAdapter(adaper);
    }

    private void launchReceiptActivity(Receipt receipt)
    {
        Intent intent = new Intent(getApplicationContext(), ReceiptActivity.class);
        intent.putExtra(EXTRA_NEW_RECEIPT_ID, receipt.getId());
        startActivity(intent);
    }
}
