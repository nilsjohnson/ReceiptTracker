package coffee.nils.dev.receipts;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * Simple Fragment for when there are no receipts yet.
 */
public class EmptyListFragment extends Fragment
{
    public EmptyListFragment()
    {
        // Required empty public constructor
    }

    public static EmptyListFragment newInstance(String param1, String param2)
    {
        EmptyListFragment fragment = new EmptyListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_empty_list, container, false);
    }
}
