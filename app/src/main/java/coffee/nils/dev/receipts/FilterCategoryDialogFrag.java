
package coffee.nils.dev.receipts;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import coffee.nils.dev.receipts.data.Receipt;
import coffee.nils.dev.receipts.data.ReceiptDAO;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FilterCategoryDialogFrag.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FilterCategoryDialogFrag#newInstance} factory method to
 * create an instance of this fragment.
 */

public class FilterCategoryDialogFrag extends DialogFragment
{
    public static final String TAG = "FilterCategoryDialogFrag";

    // to hold the names of categories and whether they are checked or not
    private CharSequence[] categories;
    private boolean[] checked;

    // for getting categories and current filters
    private ReceiptDAO receiptDAO = ReceiptDAO.get(this.getContext());

    // to categories the user checks
    private ArrayList<String> selectedCategoryList = new ArrayList<>();

    // to pass info back to main activity
    private OnFragmentInteractionListener categoryChangeListener;

    public FilterCategoryDialogFrag()
    {
        // Required empty public constructor
    }

    public static FilterCategoryDialogFrag newInstance()
    {
        FilterCategoryDialogFrag fragment = new FilterCategoryDialogFrag();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // puts the category list into CharSequence[]
        // and if it is currently checked into the checked[]
        ArrayList<String> categoryList = receiptDAO.getCategoryList();
        categories = new CharSequence[categoryList.size()];
        checked = new boolean[categoryList.size()];

        for (int i = 0; i < categoryList.size(); i++)
        {
            // adds an option for each categoy
            categories[i] = categoryList.get(i);

            // if there is already a category filter
            if(receiptDAO.getFilter().chosenCategoryList != null)
            {
                // check the box of that category if it's chosen
                if(receiptDAO.getFilter().chosenCategoryList.contains(categoryList.get(i)))
                {
                    checked[i] = true;
                }
                else
                {
                    checked[i] = false;
                }
            }
            // if there is no category filter, we check them all
            else
            {
                checked[i] = true;
                selectedCategoryList.add(categoryList.get(i));
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


    public interface OnFragmentInteractionListener
    {
        void onCategoriesSelected(ArrayList<String> categoryList);
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
                        if(!allChecked())
                        {
                            for(int i = 0; i < categories.length; i++)
                            {
                                if(checked[i])
                                {
                                    selectedCategoryList.add(categories[i].toString());
                                }
                            }
                        }
                        else
                        {
                            selectedCategoryList = null;
                        }
                        categoryChangeListener.onCategoriesSelected(selectedCategoryList);
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
                .setMultiChoiceItems(categories, checked , new DialogInterface.OnMultiChoiceClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked)
                    {
                        if(isChecked)
                        {
                            checked[which] = true;
                        }
                        else
                        {
                            checked[which] = false;
                        }
                    }
                })
                .setPositiveButton(R.string.apply_filter, sfl)
                .setNegativeButton(android.R.string.cancel, sfl);

        return builder.create();
    }

    // all checked is the same as no filter, hence this method
    private boolean allChecked()
    {
        for(int i = 0; i < checked.length; i++)
        {
            if(!checked[i])
            {
                return false;
            }
        }
        return true;
    }

}

