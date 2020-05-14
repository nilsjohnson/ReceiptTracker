package coffee.nils.dev.receipts.camera;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import coffee.nils.dev.receipts.R;

/**
 * Fragment to display the image to the user so they can accept or reject it.
 */
public class ImageReviewFragment extends Fragment
{
    private ImageView imageView;
    private Button btnAccept;
    private Button btnRedo;

    private static final String ARG_IMAGE_PATH = "Image_Path";
    private Bitmap image;

    public static final String ARG_REDO_HANDLER = "Redo_Image_Handler";
    private CameraActivity.RedoImage redoImage;

    public static final String ARG_ACCEPT_HANDLER = "Accept_Image_Handler";
    private CameraActivity.AcceptImage acceptImage;

    private String imagePath;

    public ImageReviewFragment()
    {
        // Required empty public constructor
    }


    /**
     * To make this load faster, the initial time we run it we pass the bitmap into it.
     * Lifecycle methods may have to retrieve it from disc for things like screen rotations.
     *
     * @param image
     * @param imagePath
     * @param redoImage
     * @param acceptImage
     * @return
     */
    public static ImageReviewFragment newInstance(Bitmap image, String imagePath, CameraActivity.RedoImage redoImage, CameraActivity.AcceptImage acceptImage)
    {
        ImageReviewFragment fragment = new ImageReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_PATH, imagePath);
        args.putSerializable(ARG_ACCEPT_HANDLER, acceptImage);
        args.putSerializable(ARG_REDO_HANDLER, redoImage);
        fragment.setArguments(args);
        fragment.setImage(image);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            imagePath = (String) getArguments().getString(ARG_IMAGE_PATH);
            redoImage = (CameraActivity.RedoImage) getArguments().getSerializable(ARG_REDO_HANDLER);
            acceptImage = (CameraActivity.AcceptImage) getArguments().getSerializable(ARG_ACCEPT_HANDLER);
        }
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState)
    {
        imageView = (ImageView) view.findViewById(R.id.image_review);
        imageView.setImageBitmap(image);

        btnAccept = view.findViewById(R.id.btn_accept);
        btnAccept.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                acceptImage.acceptImage();
            }
        });

        btnRedo = view.findViewById(R.id.btn_redo);
        btnRedo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                redoImage.redoImage();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image_review, container, false);
    }

    public void setImage(Bitmap image)
    {
        this.image = image;
    }

}
