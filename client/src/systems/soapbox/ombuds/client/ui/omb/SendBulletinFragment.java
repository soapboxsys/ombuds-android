package systems.soapbox.ombuds.client.ui.omb;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.apradanas.simplelinkabletext.Link;
import com.apradanas.simplelinkabletext.LinkableEditText;

import org.bitcoinj.core.Wallet;
import org.spongycastle.crypto.params.KeyParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import systems.soapbox.ombuds.client.Configuration;
import systems.soapbox.ombuds.client.WalletApplication;
import systems.soapbox.ombuds.client.ui.AbstractBindServiceActivity;
import systems.soapbox.ombuds.client.ui.send.DeriveKeyTask;
import systems.soapbox.ombuds.client_test.R;

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

//    private AbstractBindServiceActivity activity;
//    private WalletApplication application;
//    private Configuration config;
//    private Wallet wallet;
//    private ContentResolver contentResolver;
//    private LoaderManager loaderManager;
//    private FragmentManager fragmentManager;
    private LinkableEditText messageEdit;

    public SendBulletinFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

//        this.activity = (AbstractBindServiceActivity) context;
//        this.application = (WalletApplication) activity.getApplication();
//        this.config = application.getConfiguration();
//        this.wallet = application.getWallet();
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

//    private void handleGo()
//    {
//        privateKeyBadPasswordView.setVisibility(View.INVISIBLE);
//
//        if (wallet.isEncrypted())
//        {
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
//        }
//        else
//        {
//            signAndSendPayment(null);
//        }
//    }

}
