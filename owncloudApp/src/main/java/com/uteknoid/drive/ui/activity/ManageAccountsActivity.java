/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.uteknoid.drive.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SyncRequest;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.work.WorkManager;
import com.uteknoid.drive.MainApp;
import com.uteknoid.drive.R;
import com.uteknoid.drive.authentication.AccountUtils;
import com.uteknoid.drive.datamodel.FileDataStorageManager;
import com.uteknoid.drive.files.services.FileDownloader;
import com.uteknoid.drive.files.services.FileUploader;
import com.uteknoid.drive.presentation.ui.authentication.AuthenticatorConstants;
import com.uteknoid.drive.presentation.ui.authentication.LoginActivity;
import com.uteknoid.drive.services.OperationsService;
import com.uteknoid.drive.ui.adapter.AccountListAdapter;
import com.uteknoid.drive.ui.adapter.AccountListItem;
import com.uteknoid.drive.ui.dialog.RemoveAccountDialogFragment;
import com.uteknoid.drive.ui.dialog.RemoveAccountDialogViewModel;
import com.uteknoid.drive.ui.helpers.FileOperationsHelper;
import com.uteknoid.drive.usecases.CancelUploadFromAccountUseCase;
import com.uteknoid.drive.utils.PreferenceUtils;
import kotlin.Lazy;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.koin.java.KoinJavaComponent.inject;

/**
 * An Activity that allows the user to manage accounts.
 */
public class ManageAccountsActivity extends FileActivity
        implements
        AccountListAdapter.AccountListAdapterListener,
        AccountManagerCallback<Boolean>,
        ComponentsGetter {

    public static final String KEY_ACCOUNT_LIST_CHANGED = "ACCOUNT_LIST_CHANGED";
    public static final String KEY_CURRENT_ACCOUNT_CHANGED = "CURRENT_ACCOUNT_CHANGED";

    private ListView mListView;
    private final Handler mHandler = new Handler();
    private String mAccountBeingRemoved;
    private AccountListAdapter mAccountListAdapter;
    protected FileUploader.FileUploaderBinder mUploaderBinder = null;
    protected FileDownloader.FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadServiceConnection, mUploadServiceConnection = null;
    Set<String> mOriginalAccounts;
    String mOriginalCurrentAccount;
    private Drawable mTintedCheck;

    private RemoveAccountDialogViewModel mRemoveAccountDialogViewModel = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        @NotNull Lazy<RemoveAccountDialogViewModel> removeAccountDialogViewModelLazy = inject(RemoveAccountDialogViewModel.class);
        mRemoveAccountDialogViewModel = removeAccountDialogViewModelLazy.getValue();

        mTintedCheck = ContextCompat.getDrawable(this, R.drawable.ic_current_white);
        mTintedCheck = DrawableCompat.wrap(mTintedCheck);
        int tint = ContextCompat.getColor(this, R.color.actionbar_start_color);
        DrawableCompat.setTint(mTintedCheck, tint);

        setContentView(R.layout.accounts_layout);

        mListView = findViewById(R.id.account_list);
        mListView.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getApplicationContext())
        );

        setupStandardToolbar(getString(R.string.prefs_manage_accounts), true, true, true);

        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.Companion.getAccountType());
        mOriginalAccounts = toAccountNameSet(accountList);
        mOriginalCurrentAccount = AccountUtils.getCurrentOwnCloudAccount(this).name;

        setAccount(AccountUtils.getCurrentOwnCloudAccount(this));
        onAccountSet(false);

        initializeComponentGetters();

        // added click listener to switch account
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switchAccount(position);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAccountListAdapter = new AccountListAdapter(this, getAccountListItems(), mTintedCheck);
        mListView.setAdapter(mAccountListAdapter);
    }

    /**
     * converts an array of accounts into a set of account names.
     *
     * @param accountList the account array
     * @return set of account names
     */
    private Set<String> toAccountNameSet(Account[] accountList) {
        Set<String> actualAccounts = new HashSet<String>(accountList.length);
        for (Account account : accountList) {
            actualAccounts.add(account.name);
        }
        return actualAccounts;
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_ACCOUNT_LIST_CHANGED, hasAccountListChanged());
        resultIntent.putExtra(KEY_CURRENT_ACCOUNT_CHANGED, hasCurrentAccountChanged());
        setResult(RESULT_OK, resultIntent);

        finish();
        super.onBackPressed();
    }

    /**
     * checks the set of actual accounts against the set of original accounts when the activity has been started.
     *
     * @return <code>true</code> if account list has changed, <code>false</code> if not
     */
    private boolean hasAccountListChanged() {
        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.Companion.getAccountType());
        Set<String> actualAccounts = toAccountNameSet(accountList);
        return !mOriginalAccounts.equals(actualAccounts);
    }

    /**
     * checks actual current account against current accounts when the activity has been started.
     *
     * @return <code>true</code> if account list has changed, <code>false</code> if not
     */
    private boolean hasCurrentAccountChanged() {
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(this);
        return (currentAccount != null && !mOriginalCurrentAccount.equals(currentAccount.name));
    }

    /**
     * Initialize ComponentsGetters.
     */
    private void initializeComponentGetters() {
        mDownloadServiceConnection = newTransferenceServiceConnection();
        if (mDownloadServiceConnection != null) {
            bindService(new Intent(this, FileDownloader.class), mDownloadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
        mUploadServiceConnection = newTransferenceServiceConnection();
        if (mUploadServiceConnection != null) {
            bindService(new Intent(this, FileUploader.class), mUploadServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * creates the account list items list including the add-account action in case multiaccount_support is enabled.
     *
     * @return list of account list items
     */
    private ArrayList<AccountListItem> getAccountListItems() {
        Account[] accountList = AccountManager.get(this).getAccountsByType(MainApp.Companion.getAccountType());
        ArrayList<AccountListItem> adapterAccountList = new ArrayList<>(accountList.length);
        for (Account account : accountList) {
            adapterAccountList.add(new AccountListItem(account));
        }

        // Add Create Account item at the end of account list if multi-account is enabled
        if (getResources().getBoolean(R.bool.multiaccount_support)) {
            adapterAccountList.add(new AccountListItem());
        }

        return adapterAccountList;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    public void removeAccount(Account account) {
        mAccountBeingRemoved = account.name;
        RemoveAccountDialogFragment dialog = RemoveAccountDialogFragment.newInstance(
                account,
                mRemoveAccountDialogViewModel.hasCameraUploadsAttached(account.name)
        );
        dialog.show(getSupportFragmentManager(), RemoveAccountDialogFragment.FTAG_CONFIRMATION);
    }

    @Override
    public void changePasswordOfAccount(Account account) {
        Intent updateAccountCredentials = new Intent(ManageAccountsActivity.this, LoginActivity.class);
        updateAccountCredentials.putExtra(AuthenticatorConstants.EXTRA_ACCOUNT, account);
        updateAccountCredentials.putExtra(AuthenticatorConstants.EXTRA_ACTION,
                AuthenticatorConstants.ACTION_UPDATE_TOKEN);
        startActivity(updateAccountCredentials);
    }

    @Override
    public void refreshAccount(Account account) {
        Timber.d("Got to start sync");
        Timber.d("Requesting sync for " + account.name + " at " + MainApp.Companion.getAuthority() + " with new API");
        SyncRequest.Builder builder = new SyncRequest.Builder();
        builder.setSyncAdapter(account, MainApp.Companion.getAuthority());
        builder.setExpedited(true);
        builder.setManual(true);
        builder.syncOnce();

        // Fix bug in Android Lollipop when you click on refresh the whole account
        Bundle extras = new Bundle();
        builder.setExtras(extras);

        SyncRequest request = builder.build();
        ContentResolver.requestSync(request);

        this.showSnackMessage(getString(R.string.synchronizing_account));
    }

    @Override
    public void createAccount() {
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addAccount(MainApp.Companion.getAccountType(),
                null,
                null,
                null,
                this,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        if (future != null) {
                            try {
                                Bundle result = future.getResult();
                                String name = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                                AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), name);
                                mAccountListAdapter = new AccountListAdapter(
                                        ManageAccountsActivity.this,
                                        getAccountListItems(),
                                        mTintedCheck
                                );
                                mListView.setAdapter(mAccountListAdapter);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAccountListAdapter.notifyDataSetChanged();
                                    }
                                });
                            } catch (OperationCanceledException e) {
                                Timber.e(e, "Account creation canceled");
                            } catch (Exception e) {
                                Timber.e(e, "Account creation finished in exception");
                            }
                        }
                    }
                }, mHandler);
    }

    /**
     * Callback executed after the {@link AccountManager} removed an account
     *
     * @param future Result of the removal; future.getResult() is true if account was removed correctly.
     */
    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future != null && future.isDone()) {
            Account account = new Account(mAccountBeingRemoved, MainApp.Companion.getAccountType());
            if (!AccountUtils.exists(account.name, MainApp.Companion.getAppContext())) {
                // Cancel transfers of the removed account
                if (mUploaderBinder != null) {
                    mUploaderBinder.cancel(account);
                }
                if (mDownloaderBinder != null) {
                    mDownloaderBinder.cancel(account);
                }
                CancelUploadFromAccountUseCase cancelUploadFromAccountUseCase = new CancelUploadFromAccountUseCase(WorkManager.getInstance(getBaseContext()));
                cancelUploadFromAccountUseCase.execute(new CancelUploadFromAccountUseCase.Params(account.name));
            }

            mAccountListAdapter = new AccountListAdapter(this, getAccountListItems(), mTintedCheck);
            mListView.setAdapter(mAccountListAdapter);

            AccountManager am = AccountManager.get(this);
            if (am.getAccountsByType(MainApp.Companion.getAccountType()).length == 0) {
                // Show create account screen if there isn't any account
                am.addAccount(
                        MainApp.Companion.getAccountType(),
                        null, null, null,
                        this,
                        null, null
                );
            } else {    // at least one account left
                if (AccountUtils.getCurrentOwnCloudAccount(this) == null) {
                    // current account was removed - set another as current
                    String accountName = "";
                    Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.Companion.getAccountType());
                    if (accounts.length != 0) {
                        accountName = accounts[0].name;
                    }
                    AccountUtils.setCurrentOwnCloudAccount(this, accountName);
                }
            }
        }
    }

    /**
     * Switch current account to that contained in the received position of the list adapter.
     *
     * @param position A position of the account adapter containing an account.
     */
    private void switchAccount(int position) {
        Account clickedAccount = mAccountListAdapter.getItem(position).getAccount();
        if (getAccount().name.equals(clickedAccount.name)) {
            // current account selected, just go back
            finish();
        } else {
            // restart list of files with new account
            AccountUtils.setCurrentOwnCloudAccount(
                    ManageAccountsActivity.this,
                    clickedAccount.name
            );
            // Refresh dependencies to be used in selected account
            MainApp.Companion.initDependencyInjection();
            Intent i = new Intent(
                    ManageAccountsActivity.this,
                    FileDisplayActivity.class
            );
            i.putExtra(FileActivity.EXTRA_ACCOUNT, clickedAccount);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    @Override
    protected void onDestroy() {
        if (mDownloadServiceConnection != null) {
            unbindService(mDownloadServiceConnection);
            mDownloadServiceConnection = null;
        }
        if (mUploadServiceConnection != null) {
            unbindService(mUploadServiceConnection);
            mUploadServiceConnection = null;
        }

        super.onDestroy();
    }

    // Methods for ComponentsGetter
    @Override
    public FileDownloader.FileDownloaderBinder getFileDownloaderBinder() {
        return mDownloaderBinder;
    }

    @Override
    public FileUploader.FileUploaderBinder getFileUploaderBinder() {
        return mUploaderBinder;
    }

    @Override
    public OperationsService.OperationsServiceBinder getOperationsServiceBinder() {
        return null;
    }

    @Override
    public FileDataStorageManager getStorageManager() {
        return super.getStorageManager();
    }

    @Override
    public FileOperationsHelper getFileOperationsHelper() {
        return null;
    }

    protected ServiceConnection newTransferenceServiceConnection() {
        return new ManageAccountsServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class ManageAccountsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {

            if (component.equals(new ComponentName(ManageAccountsActivity.this, FileDownloader.class))) {
                mDownloaderBinder = (FileDownloader.FileDownloaderBinder) service;

            } else if (component.equals(new ComponentName(ManageAccountsActivity.this, FileUploader.class))) {
                Timber.d("Upload service connected");
                mUploaderBinder = (FileUploader.FileUploaderBinder) service;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(ManageAccountsActivity.this, FileDownloader.class))) {
                Timber.d("Download service suddenly disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(ManageAccountsActivity.this, FileUploader.class))) {
                Timber.d("Upload service suddenly disconnected");
                mUploaderBinder = null;
            }
        }
    }
}
