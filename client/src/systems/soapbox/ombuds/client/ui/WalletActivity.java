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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.base.Charsets;
import com.gordonwong.materialsheetfab.MaterialSheetFab;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.VersionedChecksummedBytes;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.BalanceType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import systems.soapbox.ombuds.client.Configuration;
import systems.soapbox.ombuds.client.Constants;
import systems.soapbox.ombuds.client.WalletApplication;
import systems.soapbox.ombuds.client.btc.data.PaymentIntent;
import systems.soapbox.ombuds.client.ui.InputParser.StringInputParser;
import systems.soapbox.ombuds.client.ui.omb.PublicRecordPagerAdapter;
import systems.soapbox.ombuds.client.ui.omb.SearchActivity;
import systems.soapbox.ombuds.client.ui.omb.SendBulletinActivity;
import systems.soapbox.ombuds.client.ui.omb.SendFab;
import systems.soapbox.ombuds.client.ui.preference.PreferenceActivity;
import systems.soapbox.ombuds.client.ui.send.SendCoinsActivity;
import systems.soapbox.ombuds.client.ui.send.SweepWalletActivity;
import systems.soapbox.ombuds.client.util.CrashReporter;
import systems.soapbox.ombuds.client.util.Crypto;
import systems.soapbox.ombuds.client.util.Io;
import systems.soapbox.ombuds.client.util.WalletUtils;
import systems.soapbox.ombuds.client_test.R;

/**
 * @author Andreas Schildbach
 */
public final class WalletActivity extends AbstractWalletActivity
{
    private static final int DIALOG_RESTORE_WALLET = 0;
    private static final int DIALOG_LOW_STORAGE_ALERT = 1;

    private WalletApplication application;
    private Configuration config;
    private Wallet wallet;

    private MaterialSheetFab materialSheetFab;

    private Handler handler = new Handler();

    private static final int REQUEST_CODE_SCAN = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        application = getWalletApplication();
        config = application.getConfiguration();
        wallet = application.getWallet();

        setContentView(R.layout.activity_ombuds);
        setupActionBar();
        setupFab();
        setupTabs();

        if (savedInstanceState == null)
            checkAlerts();

        config.touchLastUsed();

        MaybeMaintenanceFragment.add(getFragmentManager());
    }

    private void setupActionBar() {
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    private void setupFab() {
        SendFab fab = (SendFab) findViewById(R.id.fab);
        View sheetView = findViewById(R.id.fab_sheet);
        View overlay = findViewById(R.id.overlay);
        int sheetColor = ContextCompat.getColor(this, R.color.background_card);
        int fabColor = ContextCompat.getColor(this, R.color.theme_accent);

        // Create material sheet FAB
        materialSheetFab = new MaterialSheetFab<>(fab, sheetView, overlay, sheetColor, fabColor);

        // Set material sheet item click listeners
        final View sendCoinButton = findViewById(R.id.fab_sheet_item_send_coin);
        sendCoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                handleSendCoins();
                materialSheetFab.hideSheet();
            }
        });

        final View requestCoinButton = findViewById(R.id.fab_sheet_item_request_coin);
        requestCoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                handleRequestCoins();
                materialSheetFab.hideSheet();
            }
        });

        final View scanButton = findViewById(R.id.fab_sheet_item_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                handleScan();
                materialSheetFab.hideSheet();
            }
        });

        final View createBulletinButton = findViewById(R.id.fab_sheet_item_create_bulletin);
        createBulletinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                handleSendBulletin();
                materialSheetFab.hideSheet();
            }
        });
    }

    private void setupTabs() {
        // Setup view pager
        ViewPager viewpager = (ViewPager) findViewById(R.id.viewpager);
        viewpager.setAdapter(new PublicRecordPagerAdapter(this, getFragmentManager()));
        viewpager.setOffscreenPageLimit(PublicRecordPagerAdapter.NUM_ITEMS);
        updatePage(viewpager.getCurrentItem());

        // Setup tab layout
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewpager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_public_white_24dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_star_white_24dp);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_person_white_24dp);
        viewpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                updatePage(i);
//                tabLayout.getTabAt(0).setIcon(R.drawable.ic_add_grey_600_24dp);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    private void updatePage(int selectedPage) {
        updateFab(selectedPage);
    }

    private void updateFab(int selectedPage) {
        materialSheetFab.showFab();
//        materialSheetFab.showFab(0, -getResources().getDimensionPixelSize(R.dimen.snackbar_height));
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                // delayed start so that UI has enough time to initialize
                getWalletApplication().startBlockchainService(true);
            }
        }, 1000);

        checkLowStorageAlert();
    }

    @Override
    protected void onPause()
    {
        handler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent)
    {
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK)
        {
            final String input = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);

            new StringInputParser(input)
            {
                @Override
                protected void handlePaymentIntent(final PaymentIntent paymentIntent)
                {
                    SendCoinsActivity.start(WalletActivity.this, paymentIntent);
                }

                @Override
                protected void handlePrivateKey(final VersionedChecksummedBytes key)
                {
                    SweepWalletActivity.start(WalletActivity.this, key);
                }

                @Override
                protected void handleDirectTransaction(final Transaction tx) throws VerificationException
                {
                    application.processDirectTransaction(tx);
                }

                @Override
                protected void error(final int messageResId, final Object... messageArgs)
                {
                    dialog(WalletActivity.this, null, R.string.button_scan, messageResId, messageArgs);
                }
            }.parse();
        }
    }

    @Override
    public void onBackPressed() {
        if (materialSheetFab.isSheetVisible()) {
            materialSheetFab.hideSheet();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.wallet_options, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        final Resources res = getResources();
        final String externalStorageState = Environment.getExternalStorageState();

        menu.findItem(R.id.wallet_options_restore_wallet).setEnabled(
                Environment.MEDIA_MOUNTED.equals(externalStorageState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState));
        menu.findItem(R.id.wallet_options_backup_wallet).setEnabled(Environment.MEDIA_MOUNTED.equals(externalStorageState));
        menu.findItem(R.id.wallet_options_encrypt_keys).setTitle(
                wallet.isEncrypted() ? R.string.wallet_options_encrypt_keys_change : R.string.wallet_options_encrypt_keys_set);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_search:
                startActivity(new Intent(this, SearchActivity.class));
                return true;

            case R.id.wallet_options_address_book:
                AddressBookActivity.start(this);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;

            case R.id.wallet_options_sweep_wallet:
                SweepWalletActivity.start(this);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;

            case R.id.wallet_options_network_monitor:
                startActivity(new Intent(this, NetworkMonitorActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                return true;

            case R.id.wallet_options_restore_wallet:
                showDialog(DIALOG_RESTORE_WALLET);
                return true;

            case R.id.wallet_options_backup_wallet:
                handleBackupWallet();
                return true;

            case R.id.wallet_options_encrypt_keys:
                handleEncryptKeys();
                return true;

            case R.id.wallet_options_preferences:
                startActivity(new Intent(this, PreferenceActivity.class));
                return true;

            case R.id.wallet_options_safety:
                HelpDialogFragment.page(getFragmentManager(), R.string.help_safety);
                return true;

            case R.id.wallet_options_help:
                HelpDialogFragment.page(getFragmentManager(), R.string.help_wallet);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void handleRequestCoins()
    {
        startActivity(new Intent(this, RequestCoinsActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void handleSendCoins()
    {
        startActivity(new Intent(this, SendCoinsActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void handleScan()
    {
        startActivityForResult(new Intent(this, ScanActivity.class), REQUEST_CODE_SCAN);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void handleSendBulletin() {
        startActivity(new Intent(this, SendBulletinActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void handleBackupWallet()
    {
        BackupWalletDialogFragment.show(getFragmentManager());
    }

    public void handleEncryptKeys()
    {
        EncryptKeysDialogFragment.show(getFragmentManager());
    }

    @Override
    protected Dialog onCreateDialog(final int id, final Bundle args)
    {
        if (id == DIALOG_RESTORE_WALLET)
            return createRestoreWalletDialog();
        else if (id == DIALOG_LOW_STORAGE_ALERT)
            return createLowStorageAlertDialog();
        else
            throw new IllegalArgumentException();
    }

    @Override
    protected void onPrepareDialog(final int id, final Dialog dialog)
    {
        if (id == DIALOG_RESTORE_WALLET)
            prepareRestoreWalletDialog(dialog);
    }

    private Dialog createRestoreWalletDialog()
    {
        final View view = getLayoutInflater().inflate(R.layout.restore_wallet_dialog, null);
        final TextView messageView = (TextView) view.findViewById(R.id.restore_wallet_dialog_message);
        final Spinner fileView = (Spinner) view.findViewById(R.id.import_keys_from_storage_file);
        final EditText passwordView = (EditText) view.findViewById(R.id.import_keys_from_storage_password);

        final DialogBuilder dialog = new DialogBuilder(this);
        dialog.setTitle(R.string.import_keys_dialog_title);
        dialog.setView(view);
        dialog.setPositiveButton(R.string.import_keys_dialog_button_import, new OnClickListener()
        {
            @Override
            public void onClick(final DialogInterface dialog, final int which)
            {
                final File file = (File) fileView.getSelectedItem();
                final String password = passwordView.getText().toString().trim();
                passwordView.setText(null); // get rid of it asap

                if (WalletUtils.BACKUP_FILE_FILTER.accept(file))
                    restoreWalletFromProtobuf(file);
                else if (WalletUtils.KEYS_FILE_FILTER.accept(file))
                    restorePrivateKeysFromBase58(file);
                else if (Crypto.OPENSSL_FILE_FILTER.accept(file))
                    restoreWalletFromEncrypted(file, password);
            }
        });
        dialog.setNegativeButton(R.string.button_cancel, new OnClickListener()
        {
            @Override
            public void onClick(final DialogInterface dialog, final int which)
            {
                passwordView.setText(null); // get rid of it asap
            }
        });
        dialog.setOnCancelListener(new OnCancelListener()
        {
            @Override
            public void onCancel(final DialogInterface dialog)
            {
                passwordView.setText(null); // get rid of it asap
            }
        });

        final FileAdapter adapter = new FileAdapter(this)
        {
            @Override
            public View getDropDownView(final int position, View row, final ViewGroup parent)
            {
                final File file = getItem(position);
                final boolean isExternal = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.equals(file.getParentFile());
                final boolean isEncrypted = Crypto.OPENSSL_FILE_FILTER.accept(file);

                if (row == null)
                    row = inflater.inflate(R.layout.restore_wallet_file_row, null);

                final TextView filenameView = (TextView) row.findViewById(R.id.wallet_import_keys_file_row_filename);
                filenameView.setText(file.getName());

                final TextView securityView = (TextView) row.findViewById(R.id.wallet_import_keys_file_row_security);
                final String encryptedStr = context.getString(isEncrypted ? R.string.import_keys_dialog_file_security_encrypted
                        : R.string.import_keys_dialog_file_security_unencrypted);
                final String storageStr = context.getString(isExternal ? R.string.import_keys_dialog_file_security_external
                        : R.string.import_keys_dialog_file_security_internal);
                securityView.setText(encryptedStr + ", " + storageStr);

                final TextView createdView = (TextView) row.findViewById(R.id.wallet_import_keys_file_row_created);
                createdView
                        .setText(context.getString(isExternal ? R.string.import_keys_dialog_file_created_manual
                                : R.string.import_keys_dialog_file_created_automatic, DateUtils.getRelativeTimeSpanString(context,
                                file.lastModified(), true)));

                return row;
            }
        };

        final String path;
        final String backupPath = Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.getAbsolutePath();
        final String storagePath = Constants.Files.EXTERNAL_STORAGE_DIR.getAbsolutePath();
        if (backupPath.startsWith(storagePath))
            path = backupPath.substring(storagePath.length());
        else
            path = backupPath;
        messageView.setText(getString(R.string.import_keys_dialog_message, path));

        fileView.setAdapter(adapter);

        return dialog.create();
    }

    private void prepareRestoreWalletDialog(final Dialog dialog)
    {
        final AlertDialog alertDialog = (AlertDialog) dialog;

        final List<File> files = new LinkedList<File>();

        // external storage
        if (Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.exists() && Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.isDirectory())
            for (final File file : Constants.Files.EXTERNAL_WALLET_BACKUP_DIR.listFiles())
                if (Crypto.OPENSSL_FILE_FILTER.accept(file))
                    files.add(file);

        // internal storage
        for (final String filename : fileList())
            if (filename.startsWith(Constants.Files.WALLET_KEY_BACKUP_PROTOBUF + '.'))
                files.add(new File(getFilesDir(), filename));

        // sort
        Collections.sort(files, new Comparator<File>()
        {
            @Override
            public int compare(final File lhs, final File rhs)
            {
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });

        final View replaceWarningView = alertDialog.findViewById(R.id.restore_wallet_from_storage_dialog_replace_warning);
        final boolean hasCoins = wallet.getBalance(BalanceType.ESTIMATED).signum() > 0;
        replaceWarningView.setVisibility(hasCoins ? View.VISIBLE : View.GONE);

        final Spinner fileView = (Spinner) alertDialog.findViewById(R.id.import_keys_from_storage_file);
        final FileAdapter adapter = (FileAdapter) fileView.getAdapter();
        adapter.setFiles(files);
        fileView.setEnabled(!adapter.isEmpty());

        final EditText passwordView = (EditText) alertDialog.findViewById(R.id.import_keys_from_storage_password);
        passwordView.setText(null);

        final ImportDialogButtonEnablerListener dialogButtonEnabler = new ImportDialogButtonEnablerListener(passwordView, alertDialog)
        {
            @Override
            protected boolean hasFile()
            {
                return fileView.getSelectedItem() != null;
            }

            @Override
            protected boolean needsPassword()
            {
                final File selectedFile = (File) fileView.getSelectedItem();
                return selectedFile != null ? Crypto.OPENSSL_FILE_FILTER.accept(selectedFile) : false;
            }
        };
        passwordView.addTextChangedListener(dialogButtonEnabler);
        fileView.setOnItemSelectedListener(dialogButtonEnabler);

        final CheckBox showView = (CheckBox) alertDialog.findViewById(R.id.import_keys_from_storage_show);
        showView.setOnCheckedChangeListener(new ShowPasswordCheckListener(passwordView));
    }

    private void checkLowStorageAlert()
    {
        final Intent stickyIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW));
        if (stickyIntent != null)
            showDialog(DIALOG_LOW_STORAGE_ALERT);
    }

    private Dialog createLowStorageAlertDialog()
    {
        final DialogBuilder dialog = DialogBuilder.warn(this, R.string.wallet_low_storage_dialog_title);
        dialog.setMessage(R.string.wallet_low_storage_dialog_msg);
        dialog.setPositiveButton(R.string.wallet_low_storage_dialog_button_apps, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(final DialogInterface dialog, final int id)
            {
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
                finish();
            }
        });
        dialog.setNegativeButton(R.string.button_dismiss, null);
        return dialog.create();
    }

    private void checkAlerts()
    {
        final PackageInfo packageInfo = getWalletApplication().packageInfo();

        if (CrashReporter.hasSavedCrashTrace())
        {
            final StringBuilder stackTrace = new StringBuilder();

            try
            {
                CrashReporter.appendSavedCrashTrace(stackTrace);
            }
            catch (final IOException x)
            {
                log.info("problem appending crash info", x);
            }

            final ReportIssueDialogBuilder dialog = new ReportIssueDialogBuilder(this, R.string.report_issue_dialog_title_crash,
                    R.string.report_issue_dialog_message_crash)
            {
                @Override
                protected CharSequence subject()
                {
                    return Constants.REPORT_SUBJECT_CRASH + " " + packageInfo.versionName;
                }

                @Override
                protected CharSequence collectApplicationInfo() throws IOException
                {
                    final StringBuilder applicationInfo = new StringBuilder();
                    CrashReporter.appendApplicationInfo(applicationInfo, application);
                    return applicationInfo;
                }

                @Override
                protected CharSequence collectStackTrace() throws IOException
                {
                    if (stackTrace.length() > 0)
                        return stackTrace;
                    else
                        return null;
                }

                @Override
                protected CharSequence collectDeviceInfo() throws IOException
                {
                    final StringBuilder deviceInfo = new StringBuilder();
                    CrashReporter.appendDeviceInfo(deviceInfo, WalletActivity.this);
                    return deviceInfo;
                }

                @Override
                protected CharSequence collectWalletDump()
                {
                    return wallet.toString(false, true, true, null);
                }
            };

            dialog.show();
        }
    }

    private void restoreWalletFromEncrypted(final File file, final String password)
    {
        try
        {
            final BufferedReader cipherIn = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charsets.UTF_8));
            final StringBuilder cipherText = new StringBuilder();
            Io.copy(cipherIn, cipherText, Constants.BACKUP_MAX_CHARS);
            cipherIn.close();

            final byte[] plainText = Crypto.decryptBytes(cipherText.toString(), password.toCharArray());
            final InputStream is = new ByteArrayInputStream(plainText);

            restoreWallet(WalletUtils.restoreWalletFromProtobufOrBase58(is, Constants.NETWORK_PARAMETERS));

            log.info("successfully restored encrypted wallet: {}", file);
        }
        catch (final IOException x)
        {
            final DialogBuilder dialog = DialogBuilder.warn(this, R.string.import_export_keys_dialog_failure_title);
            dialog.setMessage(getString(R.string.import_keys_dialog_failure, x.getMessage()));
            dialog.setPositiveButton(R.string.button_dismiss, null);
            dialog.setNegativeButton(R.string.button_retry, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    showDialog(DIALOG_RESTORE_WALLET);
                }
            });
            dialog.show();

            log.info("problem restoring wallet", x);
        }
    }

    private void restoreWalletFromProtobuf(final File file)
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(file);
            restoreWallet(WalletUtils.restoreWalletFromProtobuf(is, Constants.NETWORK_PARAMETERS));

            log.info("successfully restored unencrypted wallet: {}", file);
        }
        catch (final IOException x)
        {
            final DialogBuilder dialog = DialogBuilder.warn(this, R.string.import_export_keys_dialog_failure_title);
            dialog.setMessage(getString(R.string.import_keys_dialog_failure, x.getMessage()));
            dialog.setPositiveButton(R.string.button_dismiss, null);
            dialog.setNegativeButton(R.string.button_retry, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    showDialog(DIALOG_RESTORE_WALLET);
                }
            });
            dialog.show();

            log.info("problem restoring wallet", x);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (final IOException x2)
                {
                    // swallow
                }
            }
        }
    }

    private void restorePrivateKeysFromBase58(final File file)
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(file);
            restoreWallet(WalletUtils.restorePrivateKeysFromBase58(is, Constants.NETWORK_PARAMETERS));

            log.info("successfully restored unencrypted private keys: {}", file);
        }
        catch (final IOException x)
        {
            final DialogBuilder dialog = DialogBuilder.warn(this, R.string.import_export_keys_dialog_failure_title);
            dialog.setMessage(getString(R.string.import_keys_dialog_failure, x.getMessage()));
            dialog.setPositiveButton(R.string.button_dismiss, null);
            dialog.setNegativeButton(R.string.button_retry, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(final DialogInterface dialog, final int id)
                {
                    showDialog(DIALOG_RESTORE_WALLET);
                }
            });
            dialog.show();

            log.info("problem restoring private keys", x);
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (final IOException x2)
                {
                    // swallow
                }
            }
        }
    }

    private void restoreWallet(final Wallet wallet) throws IOException
    {
        application.replaceWallet(wallet);

        config.disarmBackupReminder();

        final DialogBuilder dialog = new DialogBuilder(this);
        final StringBuilder message = new StringBuilder();
        message.append(getString(R.string.restore_wallet_dialog_success));
        message.append("\n\n");
        message.append(getString(R.string.restore_wallet_dialog_success_replay));
        dialog.setMessage(message);
        dialog.setNeutralButton(R.string.button_ok, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(final DialogInterface dialog, final int id)
            {
                getWalletApplication().resetBlockchain();
                finish();
            }
        });
        dialog.show();
    }
}
