package systems.soapbox.ombuds.client.ui.omb;

import android.app.Fragment;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;

import de.greenrobot.event.EventBus;
import systems.soapbox.ombuds.client.Constants;
import systems.soapbox.ombuds.client.WalletApplication;
import systems.soapbox.ombuds.client.omb.WebRelayCoordinator;
import systems.soapbox.ombuds.client.omb.event.NewBulletinsEvent;
import systems.soapbox.ombuds.client.omb.event.SendEndoEvent;
import systems.soapbox.ombuds.client.omb.memory.ProfileDbHelper;
import systems.soapbox.ombuds.client.omb.memory.PublicRecordDbHelper;
import systems.soapbox.ombuds.client.ui.AbstractWalletActivity;
import systems.soapbox.ombuds.client.ui.DialogBuilder;
import systems.soapbox.ombuds.client.ui.send.SendCoinsOfflineTask;
import systems.soapbox.ombuds.client.R;
import systems.soapbox.ombuds.lib.OmbudsBuilder;
import systems.soapbox.ombuds.lib.encode.BasicEncoder1;
import systems.soapbox.ombuds.lib.encode.MaxSizeException;
import systems.soapbox.ombuds.lib.field.BulletinId;
import systems.soapbox.ombuds.lib.field.Timestamp;
import systems.soapbox.ombuds.lib.record.Endorsement;

/**
 * Created by askuck on 12/22/15.
 */
public class PublicRecordAllFragment extends Fragment {

    PublicRecordDbHelper pubRecDbHelper;
    RecyclerView recyclerView;
    PublicRecordAllAdapter adapter;
    SwipeRefreshLayout swipeRefreshLayout;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    Wallet wallet;

    public static PublicRecordAllFragment newInstance() {
        return new PublicRecordAllFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundThread = new HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        wallet = ((WalletApplication)getActivity().getApplication()).getWallet();
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
        adapter = new PublicRecordAllAdapter((AbstractWalletActivity) getActivity(), pubRecDbHelper.getNewBulletinsCursor());

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

    public void onEvent(SendEndoEvent endoEvent) {
        handleEndorse(endoEvent.getTxid());
    }



    private void handleEndorse(String txid)
    {
        BulletinId bulletinId = new BulletinId(Sha256Hash.wrap(txid));
        Timestamp timestamp = new Timestamp( System.currentTimeMillis() / 1000 );
        final Endorsement endo = new Endorsement(bulletinId, timestamp);

        BasicEncoder1 encoder = new BasicEncoder1();
        OmbudsBuilder ombBuilder = new OmbudsBuilder(Constants.NETWORK_PARAMETERS, encoder);

        Wallet.SendRequest sendRequest = null;
        try {
            sendRequest = ombBuilder.toSendRequest(endo);
        } catch (MaxSizeException e) {
            e.printStackTrace();
            // TODO alert the user!
            return;
        }

        new SendCoinsOfflineTask(wallet, backgroundHandler)
        {
            @Override
            protected void onSuccess(final Transaction transaction)
            {
                ProfileDbHelper localRecordDb = ProfileDbHelper.getInstance(getActivity());
                localRecordDb.add(transaction, endo);

                ((WalletApplication) getActivity().getApplication()).broadcastTransaction(transaction);
            }

            @Override
            protected void onInsufficientMoney(final Coin missing)
            {

                final Coin estimated = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                final Coin available = wallet.getBalance(Wallet.BalanceType.AVAILABLE);
                final Coin pending = estimated.subtract(available);

                Toast.makeText(getActivity(), "not enough coin! oh my! ", Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onInvalidKey()
            {
            }

            @Override
            protected void onEmptyWalletFailed()
            {

                final DialogBuilder dialog = DialogBuilder.warn(getActivity(), R.string.send_coins_fragment_empty_wallet_failed_title);
                dialog.setMessage(R.string.send_coins_fragment_hint_empty_wallet_failed);
                dialog.setNeutralButton(R.string.button_dismiss, null);
                dialog.show();
            }

            @Override
            protected void onFailure(Exception exception)
            {
                final DialogBuilder dialog = DialogBuilder.warn(getActivity(), R.string.send_coins_error_msg);
                dialog.setMessage(exception.toString());
                dialog.setNeutralButton(R.string.button_dismiss, null);
                dialog.show();
            }
        }.sendCoinsOffline(sendRequest); // send asynchronously
    }

}
