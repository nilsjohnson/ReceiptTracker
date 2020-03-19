
package coffee.nils.dev.receipts;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;



/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CatFilterDialogFrag.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CatFilterDialogFrag#newInstance} factory method to
 * create an instance of this fragment.
 */

public class CatFilterDialogFrag extends DialogFragment
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_CATEGORY_LIST = "categoryList";

    public static final String TAG = "CatFilterDialogFrag";

    // TODO: Rename and change types of parameters
    private CharSequence[] categories;
    private ArrayList<String> selectedCategoryList = new ArrayList<>();

    private OnFragmentInteractionListener categoryChangeListener;

    public CatFilterDialogFrag()
    {
        // Required empty public constructor
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.

     * @param categoryList the list of options to show user
     * @return A new instance of fragment CatFilterDialogFrag.
     */

    // TODO: Rename and change types and number of parameters
    public static CatFilterDialogFrag newInstance(ArrayList<String> categoryList)
    {
        CatFilterDialogFrag fragment = new CatFilterDialogFrag();
        Bundle args = new Bundle();
        args.putSerializable(ARG_CATEGORY_LIST, categoryList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            ArrayList<String> categoryList = (ArrayList<String>) getArguments().getSerializable(ARG_CATEGORY_LIST);

            categories = new CharSequence[categoryList.size()];

            for(int i = 0; i < categoryList.size(); i++)
            {
                categories[i] = categoryList.get(i);
            }
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener)
        {
            categoryChangeListener = (OnFragmentInteractionListener) context;
        } else
        {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        categoryChangeListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */

    public interface OnFragmentInteractionListener
    {
        void onCategoryFilterChange(ArrayList<String> categoryList);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        class SetFilterListener implements DialogInterface.OnClickListener
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        categoryChangeListener.onCategoryFilterChange(selectedCategoryList);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        }

        SetFilterListener sfl = new SetFilterListener();

        selectedCategoryList = new ArrayList<>();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.filter_category_title)
                .setMultiChoiceItems(categories, null, new DialogInterface.OnMultiChoiceClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked)
                    {
                        if(isChecked)
                        {
                            selectedCategoryList.add(categories[which].toString());
                        }
                        else
                        {
                            String category = categories[which].toString();
                            selectedCategoryList.remove(category); // TODO make sure this works..
                        }
                    }
                })
                .setPositiveButton(R.string.apply_filter, sfl)
                .setNegativeButton(android.R.string.cancel, sfl);

        return builder.create();
    }

}

