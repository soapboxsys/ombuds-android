package systems.soapbox.ombuds.client.ui.omb;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.utils.MonetaryFormat;
import org.bitcoinj.wallet.DefaultCoinSelector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import systems.soapbox.ombuds.client.AddressBookProvider;
import systems.soapbox.ombuds.client.Constants;
import systems.soapbox.ombuds.client.omb.memory.NotABulletinException;
import systems.soapbox.ombuds.client.omb.memory.NotAnEndorsementException;
import systems.soapbox.ombuds.client.omb.memory.ProfileDbHelper;
import systems.soapbox.ombuds.client.ui.CurrencyTextView;
import systems.soapbox.ombuds.client.util.CircularProgressView;
import systems.soapbox.ombuds.client.util.Formats;
import systems.soapbox.ombuds.client.util.WalletUtils;
import systems.soapbox.ombuds.client_test.R;
import systems.soapbox.ombuds.lib.field.Message;
import systems.soapbox.ombuds.lib.record.Bulletin;
import systems.soapbox.ombuds.lib.record.Endorsement;

/**
 * Created by askuck on 1/16/16.
 */
public class ProfileTransactionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    public enum Warning
    {
        BACKUP, STORAGE_ENCRYPTION
    }

    private final Context context;
    private final LayoutInflater inflater;

    private final boolean useCards;
    private final Wallet wallet;
    private final int maxConnectedPeers;
    private final ProfileDbHelper profileDb;
    @Nullable
    private final OnClickListener onClickListener;

    private final List<Transaction> transactions = new ArrayList<Transaction>();
    private MonetaryFormat format;
    private Warning warning = null;

    private long selectedItemId = RecyclerView.NO_ID;

    private final int colorBackground, colorBackgroundSelected;
    private final int colorSignificant, colorLessSignificant, colorInsignificant;
    private final int colorValuePositve, colorValueNegative, colorValueOmbuds;
    private final int colorError;
    private final String textCoinBase;
    private final String textInternal;

    private static final String CONFIDENCE_SYMBOL_DEAD = "\u271D"; // latin cross
    private static final String CONFIDENCE_SYMBOL_UNKNOWN = "?";

    private static final int VIEW_TYPE_TRANSACTION = 0;
    private static final int VIEW_TYPE_WARNING = 1;

    private Map<Sha256Hash, TransactionCacheEntry> transactionCache = new HashMap<Sha256Hash, TransactionCacheEntry>();

    private static class TransactionCacheEntry
    {
        private final Coin value;
        private final boolean sent;
        private final boolean showFee;
        @Nullable
        private final Address address;
        @Nullable
        private final String addressLabel;

        private TransactionCacheEntry(final Coin value, final boolean sent, final boolean showFee, final @Nullable Address address,
                                      final @Nullable String addressLabel)
        {
            this.value = value;
            this.sent = sent;
            this.showFee = showFee;
            this.address = address;
            this.addressLabel = addressLabel;
        }
    }

    public ProfileTransactionsAdapter(final Context context, final Wallet wallet, final boolean useCards, final int maxConnectedPeers,
                                      final @Nullable OnClickListener onClickListener)
    {
        this.context = context;
        inflater = LayoutInflater.from(context);

        this.useCards = useCards;
        this.wallet = wallet;
        this.maxConnectedPeers = maxConnectedPeers;
        this.onClickListener = onClickListener;
        this.profileDb = ProfileDbHelper.getInstance(context);

        final Resources res = context.getResources();
        colorBackground = res.getColor(R.color.bg_bright);
        colorBackgroundSelected = res.getColor(R.color.bg_panel);
        colorSignificant = res.getColor(R.color.fg_significant);
        colorLessSignificant = res.getColor(R.color.fg_less_significant);
        colorInsignificant = res.getColor(R.color.fg_insignificant);
        colorValuePositve = res.getColor(R.color.green_dark);
        colorValueNegative = res.getColor(R.color.red_dark);
        colorValueOmbuds = res.getColor(R.color.theme_accent);
        colorError = res.getColor(R.color.red_error);
        textCoinBase = context.getString(R.string.wallet_transactions_fragment_coinbase);
        textInternal = context.getString(R.string.symbol_internal) + " " + context.getString(R.string.wallet_transactions_fragment_internal);

        setHasStableIds(true);
    }

    public void setFormat(final MonetaryFormat format)
    {
        this.format = format.noCode();

        notifyDataSetChanged();
    }

    public void setWarning(final Warning warning)
    {
        this.warning = warning;

        notifyDataSetChanged();
    }

    public void clear()
    {
        transactions.clear();

        notifyDataSetChanged();
    }

    public void replace(final Transaction tx)
    {
        transactions.clear();
        transactions.add(tx);

        notifyDataSetChanged();
    }

    public void replace(final Collection<Transaction> transactions)
    {
        this.transactions.clear();
        this.transactions.addAll(transactions);

        notifyDataSetChanged();
    }

    public void setSelectedItemId(final long itemId)
    {
        selectedItemId = itemId;

        notifyDataSetChanged();
    }

    public void clearCacheAndNotifyDataSetChanged()
    {
        transactionCache.clear();

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount()
    {
        int count = transactions.size();

        if (warning != null)
            count++;

        return count;
    }

    @Override
    public long getItemId(int position)
    {
        if (position == RecyclerView.NO_POSITION)
            return RecyclerView.NO_ID;

        if (warning != null)
        {
            if (position == 0)
                return 0;
            else
                position--;
        }

        return WalletUtils.longHash(transactions.get(position).getHash());
    }

    @Override
    public int getItemViewType(final int position)
    {
        if (position == 0 && warning != null)
            return VIEW_TYPE_WARNING;
        else
            return VIEW_TYPE_TRANSACTION;
    }

    public RecyclerView.ViewHolder createTransactionViewHolder(final ViewGroup parent)
    {
        return createViewHolder(parent, VIEW_TYPE_TRANSACTION);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType)
    {
        if (viewType == VIEW_TYPE_TRANSACTION)
        {
            if (useCards)
            {
                final CardView cardView = (CardView) inflater.inflate(R.layout.profile_tx_row_card, parent, false);
                cardView.setPreventCornerOverlap(false);
                cardView.setUseCompatPadding(true);
                return new TransactionViewHolder(cardView);
            }
            else
            {
                return new TransactionViewHolder(inflater.inflate(R.layout.profile_tx_row, parent, false));
            }
        }
        else if (viewType == VIEW_TYPE_WARNING)
        {
            return new WarningViewHolder(inflater.inflate(R.layout.transaction_row_warning, parent, false));
        }
        else
        {
            throw new IllegalStateException("unknown type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position)
    {
        if (holder instanceof TransactionViewHolder)
        {
            final TransactionViewHolder transactionHolder = (TransactionViewHolder) holder;

            final long itemId = getItemId(position);
            transactionHolder.itemView.setActivated(itemId == selectedItemId);

            final Transaction tx = transactions.get(position - (warning != null ? 1 : 0));
            transactionHolder.bind(tx);

            transactionHolder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(final View v)
                {
//                    setSelectedItemId(getItemId(transactionHolder.getAdapterPosition()));
                }
            });

            if (onClickListener != null)
            {
//                transactionHolder.menuView.setOnClickListener(new View.OnClickListener()
//                {
//                    @Override
//                    public void onClick(final View v)
//                    {
//                        onClickListener.onTransactionMenuClick(v, tx);
//                    }
//                });
            }
        }
        else if (holder instanceof WarningViewHolder)
        {
            final WarningViewHolder warningHolder = (WarningViewHolder) holder;

            if (warning == Warning.BACKUP)
            {
                if (transactions.size() == 1)
                {
                    warningHolder.messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    warningHolder.messageView.setText(Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_backup)));
                }
                else
                {
                    warningHolder.messageView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning_grey600_24dp, 0, 0, 0);
                    warningHolder.messageView.setText(Html.fromHtml(context.getString(R.string.wallet_disclaimer_fragment_remind_backup)));
                }
            }
            else if (warning == Warning.STORAGE_ENCRYPTION)
            {
                warningHolder.messageView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                warningHolder.messageView.setText(Html.fromHtml(context.getString(R.string.wallet_transactions_row_warning_storage_encryption)));
            }
        }
    }

    public interface OnClickListener
    {
        void onTransactionMenuClick(View view, Transaction tx);

        void onWarningClick();
    }

    private class TransactionViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView paymentTypeView;
        private final CircularProgressView confidenceCircularView;
        private final TextView confidenceTextualView;
        private final TextView addressView;
        private final View extendAddressView;
        private final TextView txInfoView;
        private final View extendTxInfoView;
        private final TextView bltnMsgView;
        private final View extendBltnMsgView;
        private final TextView timeView;
        private final CurrencyTextView valueView;

        private TransactionViewHolder(final View itemView)
        {
            super(itemView);

            paymentTypeView = (TextView) itemView.findViewById(R.id.profile_row_payment_type);
            confidenceCircularView = (CircularProgressView) itemView.findViewById(R.id.profile_row_confidence_circular);
            confidenceTextualView = (TextView) itemView.findViewById(R.id.transaction_row_confidence_textual);
            timeView = (TextView) itemView.findViewById(R.id.profile_row_time);
            addressView = (TextView) itemView.findViewById(R.id.profile_row_address);
            extendAddressView = itemView.findViewById(R.id.profile_row_extend_address);
            txInfoView = (TextView) itemView.findViewById(R.id.profile_row_tx_info);
            extendTxInfoView = itemView.findViewById(R.id.profile_row_extend_tx_info);
            bltnMsgView = (TextView) itemView.findViewById(R.id.profile_row_bulletin_message);
            extendBltnMsgView = itemView.findViewById(R.id.profile_row_extend_bulletin_message);
            valueView = (CurrencyTextView) itemView.findViewById(R.id.profile_row_total_value);

        }

        private void bind(final Transaction tx)
        {
            ((CardView) itemView).setCardBackgroundColor(colorBackground);

            final TransactionConfidence confidence = tx.getConfidence();
            final TransactionConfidence.ConfidenceType confidenceType = confidence.getConfidenceType();
            final boolean isOwn = confidence.getSource().equals(TransactionConfidence.Source.SELF);
            final boolean isCoinBase = tx.isCoinBase();
            final Transaction.Purpose purpose = tx.getPurpose();
            final Coin fee = tx.getFee();
            final String[] memo = Formats.sanitizeMemo(tx.getMemo());

            Bulletin bltn = null;
            try {
                bltn = profileDb.getBulletin(tx.getHash());
            } catch (NotABulletinException e) {
                // swallow
            }
            Endorsement endo = null;
            try {
                endo = profileDb.getEndorsement(tx.getHash());
            } catch (NotAnEndorsementException e) {
                // swallow
            }
            final boolean isRecord = bltn != null || endo != null;

            TransactionCacheEntry txCache = transactionCache.get(tx.getHash());
            if (txCache == null)
            {
                final Coin value = tx.getValue(wallet);
                final boolean sent = value.signum() < 0;
                final boolean showFee = sent && fee != null && !fee.isZero();
                final Address address;
                if (sent)
                    address = WalletUtils.getToAddressOfSent(tx, wallet);
                else
                    address = WalletUtils.getWalletAddressOfReceived(tx, wallet);
                final String addressLabel = address != null ? AddressBookProvider.resolveLabel(context, address.toString()) : null;

                txCache = new TransactionCacheEntry(value, sent, showFee, address, addressLabel);
                transactionCache.put(tx.getHash(), txCache);
            }

            // color of confidence depending on transaction state
            final int textColor, lessSignificantColor, valueColor;
            if (confidenceType == TransactionConfidence.ConfidenceType.DEAD)
            {
                textColor = colorError;
                lessSignificantColor = colorError;
                valueColor = colorError;
            }
            else if (DefaultCoinSelector.isSelectable(tx))
            {
                textColor = colorSignificant;
                lessSignificantColor = colorLessSignificant;
                if(txCache.sent) {
                    if(isRecord) {
                        valueColor = colorValueOmbuds;
                    } else {
                        valueColor = colorValueNegative;
                    }
                } else {
                    valueColor = colorValuePositve;
                }
            }
            else
            {
                textColor = colorInsignificant;
                lessSignificantColor = colorInsignificant;
                valueColor = colorInsignificant;
            }

            // payment type
            paymentTypeView.setVisibility(View.VISIBLE);
            if(txCache.sent) {
                if(bltn != null) {
                    String topicString = Utils.listToHashtagString( Message.topicExtractor(bltn.getMessage()) );
                    if(topicString.isEmpty())
                        paymentTypeView.setText(R.string.profile_row_no_topics);
                    else
                        paymentTypeView.setText(topicString);
                }
                else if(endo != null) {
                    paymentTypeView.setText(R.string.profile_row_endorsement);
                }
                else {
                    paymentTypeView.setText(R.string.profile_row_payment_sent);
                }
            } else {
                paymentTypeView.setText(R.string.profile_row_payment_received);
            }

            // confidence
            final CircularProgressView confidenceCircularView = this.confidenceCircularView;
            if (confidenceType == TransactionConfidence.ConfidenceType.PENDING)
            {
                confidenceCircularView.setVisibility(View.VISIBLE);
                confidenceTextualView.setVisibility(View.GONE);

                confidenceCircularView.setProgress(1);
                confidenceCircularView.setMaxProgress(1);
                confidenceCircularView.setSize(confidence.numBroadcastPeers());
                confidenceCircularView.setMaxSize(maxConnectedPeers / 2); // magic value
                confidenceCircularView.setColors(colorInsignificant, Color.TRANSPARENT);
            }
            else if (confidenceType == TransactionConfidence.ConfidenceType.BUILDING)
            {
                confidenceCircularView.setVisibility(View.VISIBLE);
                confidenceTextualView.setVisibility(View.GONE);

                confidenceCircularView.setProgress(confidence.getDepthInBlocks());
                confidenceCircularView.setMaxProgress(isCoinBase ? Constants.NETWORK_PARAMETERS.getSpendableCoinbaseDepth()
                        : Constants.MAX_NUM_CONFIRMATIONS);
                confidenceCircularView.setSize(1);
                confidenceCircularView.setMaxSize(1);
                confidenceCircularView.setColors(valueColor, Color.TRANSPARENT);
            }
            else if (confidenceType == TransactionConfidence.ConfidenceType.DEAD)
            {
                confidenceCircularView.setVisibility(View.GONE);
                confidenceTextualView.setVisibility(View.VISIBLE);

                confidenceTextualView.setText(CONFIDENCE_SYMBOL_DEAD);
                confidenceTextualView.setTextColor(colorError);
            }
            else {
                confidenceCircularView.setVisibility(View.GONE);
                confidenceTextualView.setVisibility(View.VISIBLE);

                confidenceTextualView.setText(CONFIDENCE_SYMBOL_UNKNOWN);
                confidenceTextualView.setTextColor(colorInsignificant);
            }

            // address
            if (isCoinBase)
            {
                addressView.setTextColor(textColor);
                addressView.setTypeface(Typeface.DEFAULT_BOLD);
                addressView.setText(textCoinBase);
            }
            else if (purpose == Transaction.Purpose.KEY_ROTATION)
            {
                addressView.setTextColor(textColor);
                addressView.setTypeface(Typeface.DEFAULT_BOLD);
                addressView.setText(textInternal);
            }
            else if (purpose == Transaction.Purpose.RAISE_FEE)
            {
                addressView.setText(null);
            }
            else if (txCache.addressLabel != null)
            {
                addressView.setTextColor(textColor);
                addressView.setTypeface(Typeface.DEFAULT_BOLD);
                addressView.setText(txCache.addressLabel);
            }
            else if (memo != null && memo.length >= 2)
            {
                addressView.setTextColor(textColor);
                addressView.setTypeface(Typeface.DEFAULT_BOLD);
                addressView.setText(memo[1]);
            }
            else if (txCache.address != null)
            {
                addressView.setTextColor(lessSignificantColor);
                addressView.setTypeface(Typeface.DEFAULT);
                addressView.setText(WalletUtils.formatAddress(txCache.address, Constants.ADDRESS_FORMAT_GROUP_SIZE,
                        Constants.ADDRESS_FORMAT_LINE_SIZE));
            }
            else
            {
                addressView.setTextColor(lessSignificantColor);
                addressView.setTypeface(Typeface.DEFAULT);
                addressView.setText("?");
            }
            extendAddressView.setVisibility(!isRecord && purpose != Transaction.Purpose.RAISE_FEE ? View.VISIBLE : View.GONE);

            // record message
            if(bltn != null) {
                bltnMsgView.setText(bltn.getMessage().getMsg());
                extendBltnMsgView.setVisibility(View.VISIBLE);
            } else if(endo != null) {
                bltnMsgView.setText(endo.getBulletinId().getHash().toString());
                extendBltnMsgView.setVisibility(View.VISIBLE);
            } else {
                bltnMsgView.setText("");
                extendBltnMsgView.setVisibility(View.GONE);
            }

            // time
            final Date time = tx.getUpdateTime();
            timeView.setVisibility(View.VISIBLE);
            timeView.setText(DateUtils.getRelativeTimeSpanString(context, time.getTime()));
            timeView.setTextColor(textColor);

            // value
            valueView.setAlwaysSigned(true);
            valueView.setFormat(format);
            final Coin value;
            if (purpose == Transaction.Purpose.RAISE_FEE)
            {
                valueView.setTextColor(colorInsignificant);
                value = fee.negate();
            }
            else
            {
                valueView.setTextColor(valueColor);
                // TODO add fee for sent tx's
                value = txCache.value;
            }
            valueView.setAmount(value);
            valueView.setVisibility(!value.isZero() ? View.VISIBLE : View.GONE);

            // tx info
            extendTxInfoView.setVisibility(View.GONE);
            txInfoView.setSingleLine(false);

            if (purpose == Transaction.Purpose.KEY_ROTATION)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(Html.fromHtml(context.getString(R.string.transaction_row_message_purpose_key_rotation)));
                txInfoView.setTextColor(colorSignificant);
            }
            else if (purpose == Transaction.Purpose.RAISE_FEE)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(R.string.transaction_row_message_purpose_raise_fee);
                txInfoView.setTextColor(colorInsignificant);
            }
            else if (isOwn && confidenceType == TransactionConfidence.ConfidenceType.PENDING && confidence.numBroadcastPeers() == 0)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(R.string.transaction_row_message_own_unbroadcasted);
                txInfoView.setTextColor(colorInsignificant);
            }
            else if (!isOwn && confidenceType == TransactionConfidence.ConfidenceType.PENDING && confidence.numBroadcastPeers() == 0)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(R.string.transaction_row_message_received_direct);
                txInfoView.setTextColor(colorInsignificant);
            }
            else if (!txCache.sent && txCache.value.compareTo(Transaction.MIN_NONDUST_OUTPUT) < 0)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(R.string.transaction_row_message_received_dust);
                txInfoView.setTextColor(colorInsignificant);
            }
            else if (!txCache.sent && confidenceType == TransactionConfidence.ConfidenceType.PENDING)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(R.string.transaction_row_message_received_unconfirmed_unlocked);
                txInfoView.setTextColor(colorInsignificant);
            }
            else if (!txCache.sent && confidenceType == TransactionConfidence.ConfidenceType.DEAD)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(R.string.transaction_row_message_received_dead);
                txInfoView.setTextColor(colorError);
            }
            else if (!txCache.sent && WalletUtils.isPayToManyTransaction(tx))
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(R.string.transaction_row_message_received_pay_to_many);
                txInfoView.setTextColor(colorInsignificant);
            }
            else if (memo != null)
            {
                extendTxInfoView.setVisibility(View.VISIBLE);
                txInfoView.setText(memo[0]);
                txInfoView.setTextColor(colorInsignificant);
                txInfoView.setSingleLine(!itemView.isActivated());
            }
        }
    }

    private class WarningViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView messageView;

        private WarningViewHolder(final View itemView)
        {
            super(itemView);

            messageView = (TextView) itemView.findViewById(R.id.transaction_row_warning_message);

            if (onClickListener != null)
            {
                itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(final View v)
                    {
                        onClickListener.onWarningClick();
                    }
                });
            }
        }
    }
}
