package coffee.nils.dev.receipts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.util.Date;

import coffee.nils.dev.receipts.data.ReceiptDAO;
import coffee.nils.dev.receipts.util.DateTools;

import static coffee.nils.dev.receipts.util.DateTools.setToEndOfDay;
import static coffee.nils.dev.receipts.util.DateTools.setToStartOfDay;


public class FilterDialogFragment extends DialogFragment
{
    public static String TAG = "FilterDialogFragment";
    private static final String ARG_MIN_DATE = "start_date";
    private static final String ARGE_MAX_DATE = "end_date";

    private FilterDialogFragment.OnRangeChangeListener rangeChangeListner;

    private TextView tvStartDate;
    private TextView tvEndDate;
    private CrystalRangeSeekbar rangeSeekBar;

    Date selectedMinDate;
    Date selectedMaxDate;

    ReceiptDAO dao = ReceiptDAO.get(this.getActivity());


    public static FilterDialogFragment newInstance(Date minDate, Date maxDate)
    {
        Bundle args = new Bundle();
        args.putSerializable(ARG_MIN_DATE, minDate);
        args.putSerializable(ARGE_MAX_DATE, maxDate);

        FilterDialogFragment fdf = new FilterDialogFragment();
        fdf.setArguments(args);
        return fdf;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Date startDate = dao.getLowestDate();//(Date) getArguments().getSerializable(ARG_MIN_DATE);
        Date endDate = dao.getHighestDate();//(Date) getArguments().getSerializable(ARGE_MAX_DATE);
        View filterView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_filter, null);


        tvStartDate = (TextView) filterView.findViewById(R.id.textView_startDate);
        tvStartDate.setText(DateTools.toSimpleFormat(startDate));

        tvEndDate = (TextView) filterView.findViewById(R.id.textView_endDate);
        tvEndDate.setText(DateTools.toSimpleFormat(endDate));

        rangeSeekBar = (CrystalRangeSeekbar) filterView.findViewById(R.id.CrystalRangeSeekBar);



        Date startDateMidnight = setToStartOfDay(startDate);
        Date endDateMidnight = setToEndOfDay(endDate);


        rangeSeekBar.setMinValue(startDateMidnight.getTime());
        rangeSeekBar.setMaxValue(endDateMidnight.getTime());


        if(dao.getFilter() != null)
        {
            rangeSeekBar.setMinStartValue(dao.getFilter().startDate.getTime());
            rangeSeekBar.setMaxStartValue(dao.getFilter().endDate.getTime());
            rangeSeekBar.apply();
        }

        rangeSeekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener()
        {
            @Override
            public void valueChanged(Number minValue, Number maxValue)
            {
                Log.d(TAG, "Min: " + minValue + "Max: " + maxValue);
                selectedMinDate = setToStartOfDay(new Date(minValue.longValue()));
                selectedMaxDate = setToStartOfDay(new Date(maxValue.longValue()));
                tvStartDate.setText(DateTools.toSimpleFormat(selectedMinDate));
                tvEndDate.setText(DateTools.toSimpleFormat(selectedMaxDate));
            }
        });


        class FilterButtonSelect implements DialogInterface.OnClickListener
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch(which)
                {
                    case DialogInterface.BUTTON_POSITIVE:
                        rangeChangeListner.onRangeChange(selectedMinDate, selectedMaxDate);
                        Log.d(TAG, "Range Selected");
                        break;
                    // TODO no negative button
                    case DialogInterface.BUTTON_NEGATIVE:
                        ReceiptDAO.get(getContext()).removeFilter();
                        rangeChangeListner.removeFilter();
                        Log.d(TAG, "Filter Removed.");
                }
            }
        }

        FilterButtonSelect fbs = new FilterButtonSelect();

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.filter_title)
                .setView(filterView)
                .setPositiveButton(R.string.apply_filter, fbs)
                .setNegativeButton(R.string.remove_filter, fbs)
                //.setNeutralButton(android.R.string.cancel, null)

                .create();
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if(context instanceof FilterDialogFragment.OnRangeChangeListener)
        {
            rangeChangeListner = (FilterDialogFragment.OnRangeChangeListener) context;
        }
        else
        {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        rangeChangeListner = null;
    }

    /**
     * This must be implemented in the calling activity.
     */
    public interface OnRangeChangeListener
    {
        void onRangeChange(Date startDate, Date endDate);
        void removeFilter();

    }


}
