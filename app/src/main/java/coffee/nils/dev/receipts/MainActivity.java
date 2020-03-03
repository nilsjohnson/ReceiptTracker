package coffee.nils.dev.receipts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Filter;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;

import coffee.nils.dev.receipts.data.DAO;
import coffee.nils.dev.receipts.data.Receipt;

public class MainActivity extends AppCompatActivity implements FilterFragment.OnFragmentInteractionListener
{
    private static String TAG = "MainActivity";
    Fragment curFrag;
    Toolbar toolbar;
    EditText searchText;

    public final static String EXTRA_NEW_RECEIPT_ID = "coffee.nils.dev.purchasetracker.MainActivity.newReceiptId";

    private FloatingActionButton fabAddReceipt;
    private DAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_main);
        dao = DAO.get(getApplicationContext());

        toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        searchText = (EditText) findViewById(R.id.editText_search);
        searchText.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction().replace(R.id.filter_container, FilterFragment.newInstance("", "")).commit();
            }
        });

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

                launchReceiptActivity(receipt, getApplicationContext());
                startActivity(captureImage);
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        Log.d(TAG, "on start called");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "on resume called");
        FragmentManager fm = getSupportFragmentManager();


        if(dao.getReceiptList().size() > 0)
        {
            curFrag = fm.findFragmentById(R.id.frag_receipt_list);
            if(curFrag == null)
            {
                curFrag = new ReceiptListFragment();
            }
        }
        else
        {
            curFrag = fm.findFragmentById(R.id.frag_empty_list);
            if(curFrag == null)
            {
                curFrag = new EmptyListFragment();
            }
        }

        fm.beginTransaction().replace(R.id.frag_container, curFrag).commit();
    }
    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(TAG, "on pause called");
    }
    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(TAG, "on stop called");
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "on destroy called");
    }

    public static void launchReceiptActivity(Receipt receipt, Context context )
    {
        Intent intent = new Intent(context, ReceiptActivity.class);
        intent.putExtra(EXTRA_NEW_RECEIPT_ID, receipt.getId());
        context.startActivity(intent);
    }

    @Override
    public void onFragmentInteraction(Uri uri)
    {
        
    }
}
