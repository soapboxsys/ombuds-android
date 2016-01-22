/*
 * Copyright 2011-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package systems.soapbox.ombuds.client.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Wallet;

import javax.annotation.Nullable;

import systems.soapbox.ombuds.client.Configuration;
import systems.soapbox.ombuds.client.WalletApplication;
import systems.soapbox.ombuds.client.btc.service.BlockchainState;
import systems.soapbox.ombuds.client.btc.service.BlockchainStateLoader;
import systems.soapbox.ombuds.client.R;

/**
 * @author Andreas Schildbach
 */
public final class WalletBalanceFragment extends Fragment
{
    private WalletApplication application;
    private AbstractWalletActivity activity;
    private Configuration config;
    private Wallet wallet;
    private LoaderManager loaderManager;

    private View viewBalance;
    private CurrencyTextView viewBalanceBtc;
    private View viewBalanceTooMuch;
    private TextView viewProgress;

    private boolean installedFromGooglePlay;

    @Nullable
    private Coin balance = null;
    @Nullable
    private BlockchainState blockchainState = null;

    private static final int ID_BALANCE_LOADER = 0;
    private static final int ID_BLOCKCHAIN_STATE_LOADER = 1;

    private static final long BLOCKCHAIN_UPTODATE_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS;
    private static final Coin SOME_BALANCE_THRESHOLD = Coin.COIN.divide(20);
    private static final Coin TOO_MUCH_BALANCE_THRESHOLD = Coin.COIN.multiply(4);

    @Override
    public void onAttach(final Activity activity)
    {
        super.onAttach(activity);

        this.activity = (AbstractWalletActivity) activity;
        this.application = (WalletApplication) activity.getApplication();
        this.config = application.getConfiguration();
        this.wallet = application.getWallet();
        this.loaderManager = getLoaderManager();

        installedFromGooglePlay = "com.android.vending".equals(application.getPackageManager().getInstallerPackageName(application.getPackageName()));
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.wallet_balance_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        viewBalance = view.findViewById(R.id.wallet_balance);
        viewBalance.setEnabled(false);

        viewBalanceBtc = (CurrencyTextView) view.findViewById(R.id.wallet_balance_btc);
        viewBalanceBtc.setPrefixScaleX(0.9f);

        viewBalanceTooMuch = view.findViewById(R.id.wallet_balance_too_much);

        viewProgress = (TextView) view.findViewById(R.id.wallet_balance_progress);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        loaderManager.initLoader(ID_BALANCE_LOADER, null, balanceLoaderCallbacks);
        loaderManager.initLoader(ID_BLOCKCHAIN_STATE_LOADER, null, blockchainStateLoaderCallbacks);

        updateView();
    }

    @Override
    public void onPause()
    {
        loaderManager.destroyLoader(ID_BLOCKCHAIN_STATE_LOADER);
        loaderManager.destroyLoader(ID_BALANCE_LOADER);

        super.onPause();
    }

    private void updateView()
    {
        if (!isAdded())
            return;

        final boolean showProgress;

        if (blockchainState != null && blockchainState.bestChainDate != null)
        {
            final long blockchainLag = System.currentTimeMillis() - blockchainState.bestChainDate.getTime();
            final boolean blockchainUptodate = blockchainLag < BLOCKCHAIN_UPTODATE_THRESHOLD_MS;
            final boolean noImpediments = blockchainState.impediments.isEmpty();

            showProgress = !(blockchainUptodate || !blockchainState.replaying);

            final String downloading = getString(noImpediments ? R.string.blockchain_state_progress_downloading
                    : R.string.blockchain_state_progress_stalled);

            if (blockchainLag < 2 * DateUtils.DAY_IN_MILLIS)
            {
                final long hours = blockchainLag / DateUtils.HOUR_IN_MILLIS;
                viewProgress.setText(getString(R.string.blockchain_state_progress_hours, downloading, hours));
            }
            else if (blockchainLag < 2 * DateUtils.WEEK_IN_MILLIS)
            {
                final long days = blockchainLag / DateUtils.DAY_IN_MILLIS;
                viewProgress.setText(getString(R.string.blockchain_state_progress_days, downloading, days));
            }
            else if (blockchainLag < 90 * DateUtils.DAY_IN_MILLIS)
            {
                final long weeks = blockchainLag / DateUtils.WEEK_IN_MILLIS;
                viewProgress.setText(getString(R.string.blockchain_state_progress_weeks, downloading, weeks));
            }
            else
            {
                final long months = blockchainLag / (30 * DateUtils.DAY_IN_MILLIS);
                viewProgress.setText(getString(R.string.blockchain_state_progress_months, downloading, months));
            }
        }
        else
        {
            showProgress = false;
        }

        if (!showProgress)
        {
            viewBalance.setVisibility(View.VISIBLE);

            if (balance != null)
            {
                viewBalanceBtc.setVisibility(View.VISIBLE);
                viewBalanceBtc.setFormat(config.getFormat());
                viewBalanceBtc.setAmount(balance);

                final boolean tooMuch = balance.isGreaterThan(TOO_MUCH_BALANCE_THRESHOLD);

                viewBalanceTooMuch.setVisibility(tooMuch ? View.VISIBLE : View.GONE);
            }
            else
            {
                viewBalanceBtc.setVisibility(View.INVISIBLE);
            }

            viewProgress.setVisibility(View.GONE);
        }
        else
        {
            viewProgress.setVisibility(View.VISIBLE);
            viewBalance.setVisibility(View.INVISIBLE);
        }
    }

    private final LoaderCallbacks<BlockchainState> blockchainStateLoaderCallbacks = new LoaderManager.LoaderCallbacks<BlockchainState>()
    {
        @Override
        public Loader<BlockchainState> onCreateLoader(final int id, final Bundle args)
        {
            return new BlockchainStateLoader(activity);
        }

        @Override
        public void onLoadFinished(final Loader<BlockchainState> loader, final BlockchainState blockchainState)
        {
            WalletBalanceFragment.this.blockchainState = blockchainState;

            updateView();
        }

        @Override
        public void onLoaderReset(final Loader<BlockchainState> loader)
        {
        }
    };

    private final LoaderCallbacks<Coin> balanceLoaderCallbacks = new LoaderManager.LoaderCallbacks<Coin>()
    {
        @Override
        public Loader<Coin> onCreateLoader(final int id, final Bundle args)
        {
            return new WalletBalanceLoader(activity, wallet);
        }

        @Override
        public void onLoadFinished(final Loader<Coin> loader, final Coin balance)
        {
            WalletBalanceFragment.this.balance = balance;

            updateView();
        }

        @Override
        public void onLoaderReset(final Loader<Coin> loader)
        {
        }
    };
}
