package systems.soapbox.ombuds.client.ui.omb;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import systems.soapbox.ombuds.client_test.R;

/**
 * Created by askuck on 12/22/15.
 */
public class ExploreProfileFragment extends Fragment {
    public static ExploreProfileFragment newInstance() {
        return new ExploreProfileFragment();
    }

    public ExploreProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ombuds_profile, container, false);
    }
}