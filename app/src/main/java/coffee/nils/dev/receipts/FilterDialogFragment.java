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

import coffee.nils.dev.receipts.data.DAO;
import coffee.nils.dev.receipts.data.Filter;
import coffee.nils.dev.receipts.util.DateTools;


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
        Date startDate = (Date) getArguments().getSerializable(ARG_MIN_DATE);
        Date endDate = (Date) getArguments().getSerializable(ARGE_MAX_DATE);
        View filterView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_filter, null);


        tvStartDate = (TextView) filterView.findViewById(R.id.textView_startDate);
        tvStartDate.setText(DateTools.toSimpleFormat(startDate));

        tvEndDate = (TextView) filterView.findViewById(R.id.textView_endDate);
        tvEndDate.setText(DateTools.toSimpleFormat(endDate));

        rangeSeekBar = (CrystalRangeSeekbar) filterView.findViewById(R.id.CrystalRangeSeekBar);
        rangeSeekBar.setMinValue(startDate.getTime());
        rangeSeekBar.setMaxValue(endDate.getTime());

        rangeSeekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener()
        {
            @Override
            public void valueChanged(Number minValue, Number maxValue)
            {
                selectedMinDate = new Date(minValue.longValue());
                selectedMaxDate = new Date(maxValue.longValue());
                tvStartDate.setText(DateTools.toSimpleFormat(selectedMinDate));
                tvEndDate.setText(DateTools.toSimpleFormat(selectedMaxDate));
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.filter_title)
                .setView(filterView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch(which)
                        {
                            case DialogInterface.BUTTON_POSITIVE:
                                rangeChangeListner.onRangeChange(selectedMinDate, selectedMaxDate);
                                break;
                                // TODO no negative button
                            case DialogInterface.BUTTON_NEGATIVE:
                                DAO.get(getContext()).removeFilter();
                        }
                    }
                })
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
    }

}
