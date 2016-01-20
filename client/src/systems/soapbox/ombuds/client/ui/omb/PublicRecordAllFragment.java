package systems.soapbox.ombuds.client.ui.omb;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import systems.soapbox.ombuds.client.omb.WebRelayCoordinator;
import systems.soapbox.ombuds.client_test.R;

/**
 * Created by askuck on 12/22/15.
 */
public class PublicRecordAllFragment extends Fragment {

    public static PublicRecordAllFragment newInstance() {
        return new PublicRecordAllFragment();
    }

    public PublicRecordAllFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pubrec_all, container, false);

        Button refreshButton = (Button) view.findViewById(R.id.button_refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), WebRelayCoordinator.class);
                intent.setAction("REFRESH");
                getActivity().startService(intent);
            }
        });

        return view;
    }
}
