package systems.soapbox.ombuds.client.ui.omb;

import android.app.Fragment;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import systems.soapbox.ombuds.client.omb.WebRelayCoordinator;
import systems.soapbox.ombuds.client.omb.event.NewBulletinsEvent;
import systems.soapbox.ombuds.client.omb.memory.PublicRecordDbHelper;
import systems.soapbox.ombuds.client_test.R;

/**
 * Created by askuck on 12/22/15.
 */
public class PublicRecordAllFragment extends Fragment {

    PublicRecordDbHelper pubRecDbHelper;
    RecyclerView recyclerView;
    PublicRecordAllAdapter adapter;
    SwipeRefreshLayout swipeRefreshLayout;

    public static PublicRecordAllFragment newInstance() {
        return new PublicRecordAllFragment();
    }

    public PublicRecordAllFragment() {
        // Required empty public constructor
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pubrec_all, container, false);

        pubRecDbHelper = PublicRecordDbHelper.getInstance(getActivity());
        adapter = new PublicRecordAllAdapter(getActivity(), pubRecDbHelper.getNewBulletinsCursor());

        recyclerView = (RecyclerView) view.findViewById(R.id.pubrec_all_list);
        recyclerView.setHasFixedSize(true);
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

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.pubrec_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.theme_accent, R.color.theme_accent, R.color.theme_accent);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshList();
            };
        });

        refreshList();
        return view;
    }

    private void refreshList() {
        setRefreshAnim(true);
        WebRelayCoordinator.refreshNewBltns(getActivity());
    }

    private void setRefreshAnim(boolean display) {
        if(swipeRefreshLayout == null)
            return;

        swipeRefreshLayout.setRefreshing(display);
    }

    public void onEvent(NewBulletinsEvent newBltnsEvent) {
        if(newBltnsEvent.wasSuccess()) {
            adapter.swapCursor(pubRecDbHelper.getNewBulletinsCursor());
            adapter.notifyDataSetChanged();
        } else {

        }
        setRefreshAnim(false);
    }

}
