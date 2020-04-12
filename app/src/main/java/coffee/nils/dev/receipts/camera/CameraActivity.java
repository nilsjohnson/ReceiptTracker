package coffee.nils.dev.receipts.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;

import java.io.Serializable;
import java.util.UUID;

import coffee.nils.dev.receipts.MainActivity;
import coffee.nils.dev.receipts.R;

public class CameraActivity extends AppCompatActivity
{
    private static final String KEY_UUID = "receipt_ID";
    private UUID newReceiptID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Bundle bundle = getIntent().getExtras();
        newReceiptID = (UUID)bundle.getSerializable(MainActivity.EXTRA_NEW_RECEIPT_ID);

        if (savedInstanceState != null)
        {
            newReceiptID = (UUID) savedInstanceState.getSerializable(KEY_UUID);
        }
        getSupportFragmentManager()
                .beginTransaction().
                replace(R.id.container, CameraFragment.newInstance(newReceiptID, new ReviewImage()))
                .commit();
    }

    public class ReviewImage implements Serializable
    {
        public void startReview(Bitmap image, String imagePath)
        {
            ImageReviewFragment frag = ImageReviewFragment.newInstance(image, imagePath, new RedoImage(), new AcceptImage());

            getSupportFragmentManager()
                    .beginTransaction().
                    replace(R.id.container, frag)
                    .commit();
        }
    }

    public class RedoImage implements Serializable
    {
        public void redoImage()
        {
            // TODO dry this up
            getSupportFragmentManager()
                    .beginTransaction().
                    replace(R.id.container, CameraFragment.newInstance(newReceiptID, new ReviewImage()))
                    .commit();
        }
    }

    public class AcceptImage implements Serializable
    {
        public void acceptImage()
        {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle instanceState)
    {
        super.onSaveInstanceState(instanceState);
        instanceState.putSerializable(KEY_UUID, newReceiptID);
    }

}