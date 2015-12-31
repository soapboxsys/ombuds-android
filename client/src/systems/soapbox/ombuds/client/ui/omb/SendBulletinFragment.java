package systems.soapbox.ombuds.client.ui.omb;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.apradanas.simplelinkabletext.Link;
import com.apradanas.simplelinkabletext.LinkableEditText;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.utils.MonetaryFormat;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import systems.soapbox.ombuds.client.Configuration;
import systems.soapbox.ombuds.client.Constants;
import systems.soapbox.ombuds.client.WalletApplication;
import systems.soapbox.ombuds.client.ui.AbstractBindServiceActivity;
import systems.soapbox.ombuds.client.ui.DialogBuilder;
import systems.soapbox.ombuds.client.ui.send.SendCoinsOfflineTask;
import systems.soapbox.ombuds.client_test.R;
import systems.soapbox.ombuds.lib.OmbudsBuilder;
import systems.soapbox.ombuds.lib.OmbudsTransaction;
import systems.soapbox.ombuds.lib.encode.BasicEncoder1;
import systems.soapbox.ombuds.lib.encode.MaxSizeException;
import systems.soapbox.ombuds.lib.encode.UnencodableRecordException;
import systems.soapbox.ombuds.lib.field.Location;
import systems.soapbox.ombuds.lib.field.Message;
import systems.soapbox.ombuds.lib.field.Timestamp;
import systems.soapbox.ombuds.lib.record.Bulletin;

public class SendBulletinFragment extends Fragment {

    /**
     *  TODO:
     *  + handle encrypted wallet
     *  + build topic detector into ombuds-lib
     *  + maybe remove simplelinkabletext dependency
     *  + display cursor on messageEdit on startup
     *  + handle orientation change
     *
     */

    private AbstractBindServiceActivity activity;
    private WalletApplication application;
    private Configuration config;
    private Wallet wallet;
//    private ContentResolver contentResolver;
//    private LoaderManager loaderManager;
//    private FragmentManager fragmentManager;
    private LinkableEditText messageEdit;

    private final Handler handler = new Handler();
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private Transaction sentTransaction = null;


    public SendBulletinFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundThread = new HandlerThread("backgroundThread", Process.THREAD_PRIORITY_BACKGROUND);
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        this.activity = (AbstractBindServiceActivity) context;
        this.application = (WalletApplication) activity.getApplication();
        this.config = application.getConfiguration();
        this.wallet = application.getWallet();
//        this.contentResolver = activity.getContentResolver();
//        this.loaderManager = getLoaderManager();
//        this.fragmentManager = getFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_send_bulletin, container);
        final Button submitButton = (Button) view.findViewById(R.id.button_send_bulletin);
        final TextView topicsView = (TextView) view.findViewById(R.id.text_topics);
        messageEdit = (LinkableEditText) view.findViewById(R.id.edit_message);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleGo();
            }
        });

        if(messageEdit.getText().length() == 0){     // should always resolve as true
            submitButton.setEnabled(false);
            topicsView.setText(getString(R.string.send_bulletin_no_topics));
        }


        Link linkHashtag = new Link(Pattern.compile("(#\\w+)"))
                .setUnderlined(false)
                .setTextStyle(Link.TextStyle.ITALIC);

        List<Link> links = new ArrayList<>();
        links.add(linkHashtag);

        messageEdit.addLinks(links);
        messageEdit.setTextChangedListener(new LinkableEditText.OnTextChangedListener() {
            @Override
            public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                submitButton.setEnabled(s.length() > 0);

                List<Link> foundLinks = messageEdit.getFoundLinks();

                int i = 0;
                int size = foundLinks.size();

                if (size > 0) {
                    String found = "";
                    while (i < size) {
                        found += foundLinks.get(i).getText();
                        found += i < size - 1 ? ", " : "";
                        i++;
                    }
                    topicsView.setText(found);
                } else {
                    topicsView.setText(getString(R.string.send_bulletin_no_topics));
                }
            }
        });

        requestFocus();
        return view;
    }

    private void requestFocus() {
        messageEdit.requestFocus();
    }

    private void handleGo()
    {
//        privateKeyBadPasswordView.setVisibility(View.INVISIBLE);

        if (wallet.isEncrypted())
        {
//            new DeriveKeyTask(backgroundHandler)
//            {
//                @Override
//                protected void onSuccess(KeyParameter encryptionKey)
//                {
//                    signAndSendPayment(encryptionKey);
//                }
//            }.deriveKey(wallet.getKeyCrypter(), privateKeyPasswordView.getText().toString().trim());
//
//            setState(State.DECRYPTING);
        }
        else
        {
            signAndSendPayment(null);
        }
    }

    private void signAndSendPayment(final KeyParameter encryptionKey)
    {
//        setState(State.SIGNING);

        Message mMessage = new Message(messageEdit.getText().toString());
        Timestamp mTimestamp = new Timestamp( System.currentTimeMillis() / 1000 );
        Location mLoc = new Location();
        Bulletin mBulletin = new Bulletin(mMessage, mTimestamp, mLoc);

        BasicEncoder1 mEncorder = new BasicEncoder1();
        OmbudsBuilder ombBuilder = new OmbudsBuilder(Constants.NETWORK_PARAMETERS, mEncorder);

        Wallet.SendRequest sendRequest = null;
        try {
            sendRequest = ombBuilder.toSendRequest(mBulletin);
        } catch (MaxSizeException e) {
            e.printStackTrace();
            // TODO alert the user!
            return;
        }

//        sendRequest.memo = paymentIntent.memo;
//        sendRequest.aesKey = encryptionKey;

        new SendCoinsOfflineTask(wallet, backgroundHandler)
        {
            @Override
            protected void onSuccess(final Transaction transaction)
            {
                sentTransaction = transaction;

//                setState(State.SENDING);
//                sentTransaction.getConfidence().addEventListener(sentTransactionConfidenceListener);

                application.broadcastTransaction(sentTransaction);
                activity.finish();
            }

            @Override
            protected void onInsufficientMoney(final Coin missing)
            {
//                setState(State.INPUT);

                final Coin estimated = wallet.getBalance(Wallet.BalanceType.ESTIMATED);
                final Coin available = wallet.getBalance(Wallet.BalanceType.AVAILABLE);
                final Coin pending = estimated.subtract(available);

                final MonetaryFormat btcFormat = config.getFormat();
                Toast.makeText(activity, "not enough coin! oh my! ", Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onInvalidKey()
            {
//                setState(State.INPUT);

//                privateKeyBadPasswordView.setVisibility(View.VISIBLE);
//                privateKeyPasswordView.requestFocus();
            }

            @Override
            protected void onEmptyWalletFailed()
            {
//                setState(State.INPUT);

                final DialogBuilder dialog = DialogBuilder.warn(activity, R.string.send_coins_fragment_empty_wallet_failed_title);
                dialog.setMessage(R.string.send_coins_fragment_hint_empty_wallet_failed);
                dialog.setNeutralButton(R.string.button_dismiss, null);
                dialog.show();
            }

            @Override
            protected void onFailure(Exception exception)
            {
//                setState(State.FAILED);

                final DialogBuilder dialog = DialogBuilder.warn(activity, R.string.send_coins_error_msg);
                dialog.setMessage(exception.toString());
                dialog.setNeutralButton(R.string.button_dismiss, null);
                dialog.show();
            }
        }.sendCoinsOffline(sendRequest); // send asynchronously
    }

}
