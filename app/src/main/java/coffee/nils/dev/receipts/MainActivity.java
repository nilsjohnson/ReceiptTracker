package coffee.nils.dev.receipts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import coffee.nils.dev.receipts.data.ReceiptDAO;
import coffee.nils.dev.receipts.data.Receipt;

public class MainActivity extends AppCompatActivity implements FilterDateDialogFrag.OnRangeChangeListener, FilterCategoryDialogFrag.OnFragmentInteractionListener
{
    private static String TAG = "MainActivity";
    Fragment curFrag;

    public final static String EXTRA_NEW_RECEIPT_ID = "coffee.nils.dev.purchasetracker.MainActivity.newReceiptId";

    private FloatingActionButton fabAddReceipt;
    private ReceiptDAO dao;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_main);
        dao = ReceiptDAO.get(getApplicationContext());


        toolbar = findViewById(R.id.toolbar_main_menu);
        setSupportActionBar(toolbar);

        fabAddReceipt = (FloatingActionButton) findViewById(R.id.fab_add_receipt);
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

        fm.beginTransaction().replace(R.id.receipt_list_container, curFrag).commit();
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_filter)
        {
            FragmentManager fm = getSupportFragmentManager();
            FilterDateDialogFrag frag = FilterDateDialogFrag.newInstance(dao.getLowestDate(), dao.getHighestDate());
            frag.show(fm, FilterDateDialogFrag.TAG);

            return true;
        }

        if(id == R.id.action_filter_category)
        {
            FragmentManager fm = getSupportFragmentManager();
            FilterCategoryDialogFrag frag = FilterCategoryDialogFrag.newInstance();
            frag.show(fm, FilterCategoryDialogFrag.TAG);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRangeChange(Date startDate, Date endDate)
    {
        dao.getFilter().startDate = startDate;
        dao.getFilter().endDate = endDate;

        updateUIAfterFilter();
    }


    @Override
    public void onCategoriesSelected(ArrayList<String> categoryList)
    {
        dao.getFilter().chosenCategoryList = categoryList;
        updateUIAfterFilter();
    }

    private void updateUIAfterFilter()
    {
        FragmentManager fm = getSupportFragmentManager();
        ReceiptListFragment frag = ReceiptListFragment.newInstance();
        fm.beginTransaction().replace(R.id.receipt_list_container, frag).commit();
    }
}
