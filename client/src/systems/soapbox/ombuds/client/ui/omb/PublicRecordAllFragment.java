package systems.soapbox.ombuds.client.ui.omb;

import android.app.Fragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import systems.soapbox.ombuds.client.omb.WebRelayCoordinator;
import systems.soapbox.ombuds.client.omb.memory.PublicRecordDbHelper;
import systems.soapbox.ombuds.client_test.R;

/**
 * Created by askuck on 12/22/15.
 */
public class PublicRecordAllFragment extends Fragment {

    RecyclerView recyclerView;
    PublicRecordAllAdapter adapter;

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

        Cursor c = PublicRecordDbHelper.getInstance(getActivity()).getNewBulletinCursor();
        adapter = new PublicRecordAllAdapter(getActivity(), c);

        recyclerView = (RecyclerView) view.findViewById(R.id.pubrec_all_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            private final int PADDING = 2 * getActivity().getResources().getDimensionPixelOffset(R.dimen.card_padding_vertical);

            @Override
            public void getItemOffsets(final Rect outRect, final View view, final RecyclerView parent, final RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);

                final int position = parent.getChildAdapterPosition(view);
                if (position == 0)
                    outRect.top += PADDING;
                else if (position == parent.getAdapter().getItemCount() - 1)
                    outRect.bottom += PADDING;
            }
        });

        return view;
    }
}
