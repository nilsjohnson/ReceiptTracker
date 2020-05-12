package coffee.nils.dev.receipts;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crystal.crystalrangeseekbar.interfaces.OnRangeSeekbarChangeListener;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;

import java.util.Date;

import coffee.nils.dev.receipts.data.ReceiptDAO;
import coffee.nils.dev.receipts.util.DateTools;

public class FilterDateDialogFrag extends DialogFragment
{
    public static String TAG = "FilterDateDialogFrag";
    private static final String ARG_MIN_DATE = "start_date";
    private static final String ARGE_MAX_DATE = "end_date";

    private FilterDateDialogFrag.OnRangeChangeListener rangeChangeListner;

    private TextView tvStartDate;
    private TextView tvEndDate;
    private CrystalRangeSeekbar rangeSeekBar;

    private Date selectedMinDate;
    private Date selectedMaxDate;

    private Date minDate;
    private Date maxDate;

    private static final long TIME_FACTOR = 1;

    ReceiptDAO receiptDAO = ReceiptDAO.getInstance(this.getActivity());

    public static FilterDateDialogFrag newInstance(Date minDate, Date maxDate)
    {
        Bundle args = new Bundle();
        args.putSerializable(ARG_MIN_DATE, minDate);
        args.putSerializable(ARGE_MAX_DATE, maxDate);

        FilterDateDialogFrag fdf = new FilterDateDialogFrag();
        fdf.setArguments(args);
        return fdf;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        View filterView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_filter, null);

        minDate = DateTools.setToNoon(receiptDAO.getLowestDate());
        maxDate = DateTools.setToNoon(receiptDAO.getHighestDate());

        tvEndDate = (TextView) filterView.findViewById(R.id.textView_endDate);
        tvEndDate.setText(DateTools.toSimpleFormat(maxDate));

        tvStartDate = (TextView) filterView.findViewById(R.id.textView_startDate);
        tvStartDate.setText(DateTools.toSimpleFormat(minDate));

        rangeSeekBar = (CrystalRangeSeekbar) filterView.findViewById(R.id.CrystalRangeSeekBar);

        rangeSeekBar.setMinValue(minDate.getTime() / TIME_FACTOR);
        rangeSeekBar.setMaxValue(maxDate.getTime() / TIME_FACTOR);

        // if there is already a date filter, we set the thumbs to those points
        if(receiptDAO.getFilter().startDate != null && receiptDAO.getFilter().endDate != null)
        {
            rangeSeekBar.setMinStartValue(receiptDAO.getFilter().startDate.getTime() / TIME_FACTOR);
            rangeSeekBar.setMaxStartValue(receiptDAO.getFilter().endDate.getTime() / TIME_FACTOR);
            rangeSeekBar.apply();
        }

        rangeSeekBar.setOnRangeSeekbarChangeListener(new OnRangeSeekbarChangeListener()
        {
            @Override
            public void valueChanged(Number minValue, Number maxValue)
            {
                selectedMinDate = new Date(minValue.longValue()*TIME_FACTOR);
                selectedMaxDate = new Date(maxValue.longValue()*TIME_FACTOR);
                tvStartDate.setText(DateTools.toSimpleFormat(selectedMinDate));
                tvEndDate.setText(DateTools.toSimpleFormat(selectedMaxDate));
                rangeChangeListner.onRangeChange(selectedMinDate, selectedMaxDate);
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
                        if(which == DialogInterface.BUTTON_POSITIVE)
                        {
                            if(DateTools.isSameDay(selectedMinDate, minDate) && DateTools.isSameDay(selectedMaxDate, maxDate))
                            {
                                Toast.makeText(getContext(), getResources().getString(R.string.showing_all_dates), Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(getContext(), getResources().getString(R.string.date_filter_applied), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .create();
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        if(context instanceof FilterDateDialogFrag.OnRangeChangeListener)
        {
            rangeChangeListner = (FilterDateDialogFrag.OnRangeChangeListener) context;
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
