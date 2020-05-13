package coffee.nils.dev.receipts;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import coffee.nils.dev.receipts.data.ReceiptDAO;
import coffee.nils.dev.receipts.data.Receipt;
import coffee.nils.dev.receipts.util.DateTools;


/**
 * For displaying the user's receipts
 */
public class ReceiptListFragment extends Fragment
{
    private ReceiptDAO dao;

    // for the scrolling items
    private RecyclerView recyclerView;
    private ReceiptAdapter adaper;

    public ReceiptListFragment()
    {
        // Required empty public constructor
    }

    public static ReceiptListFragment newInstance()
    {
        return new ReceiptListFragment();
    }

    private class ReceiptHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        private TextView textViewBusinessName;
        private TextView textViewDate;

        private Receipt receipt;

        public ReceiptHolder(LayoutInflater inflater, ViewGroup parent)
        {
            super(inflater.inflate(R.layout.list_item, parent, false));
            textViewBusinessName = (TextView) itemView.findViewById(R.id.textView_name);
            textViewDate = (TextView) itemView.findViewById(R.id.textView_date);

            itemView.setOnClickListener(this);
        }

        public void bind(Receipt receipt)
        {
            this.receipt = receipt;
            if(receipt.getStoreName() != null)
            {
                textViewBusinessName.setText(receipt.getStoreName());
            }

            textViewDate.setText(DateTools.toSimpleFormat(receipt.getDate()));
        }

        @Override
        public void onClick(View view)
        {
            MainActivity.launchReceiptActivity(receipt, getContext());
        }
    }

    private class ReceiptAdapter extends RecyclerView.Adapter<ReceiptHolder>
    {
        private List<Receipt> receiptList;

        public ReceiptAdapter(List<Receipt> receiptList)
        {
            this.receiptList = receiptList;
        }

        @NonNull
        @Override
        public ReceiptHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            return new ReceiptHolder(layoutInflater, parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ReceiptHolder holder, int position)
        {
            Receipt purchase = receiptList.get(position);
            holder.bind(purchase);
        }

        @Override
        public int getItemCount()
        {
            return receiptList.size();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        dao = ReceiptDAO.getInstance(getActivity());
        View view = inflater.inflate(R.layout.fragment_receipt_list, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView_receipts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        updateUI();
        return view;
    }

    private void updateUI()
    {
        List<Receipt> receiptList = dao.getReceiptList();
        adaper = new ReceiptAdapter(receiptList);
        recyclerView.setAdapter(adaper);
    }
}
