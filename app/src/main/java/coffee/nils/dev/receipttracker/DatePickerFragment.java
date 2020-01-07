package coffee.nils.dev.receipttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;



public class DatePickerFragment extends DialogFragment
{
    private DatePicker.OnDateChangedListener datePickerListener;

    private static final  String ARG_DATE = "date";
    private DatePicker datePicker;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Date date = (Date) getArguments().getSerializable(ARG_DATE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_date_picker, null);

        datePicker = (DatePicker) v.findViewById(R.id.date_picker);
        datePicker.init(year, month, day, datePickerListener);


        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle(R.string.date_picker_title)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // using listener, nothing needs to be here
                               /* int year = datePicker.getYear();
                                int month = datePicker.getMonth();
                                int day = datePicker.getDayOfMonth();
                                Date date = new GregorianCalendar(year, month, day).getTime();*/
                            }
                        })
                .create();
    }


    public static DatePickerFragment newInstance(Date date,  DatePicker.OnDateChangedListener listener)
    {
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);

        DatePickerFragment fragment = new DatePickerFragment();
        fragment.setArguments(args);
        fragment.setDatePickerListener(listener);

        return fragment;
    }


    public DatePicker.OnDateChangedListener getDatePickerListener() {
        return this.datePickerListener;
    }

    public void setDatePickerListener(DatePicker.OnDateChangedListener listener) {
        this.datePickerListener = listener;
    }

}



