/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author David González Verdugo
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.uteknoid.drive.operations;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.uteknoid.drive.authentication.AccountUtils;
import com.uteknoid.drive.datamodel.OCFile;
import com.uteknoid.drive.lib.common.OwnCloudClient;
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult;
import com.uteknoid.drive.lib.resources.files.RemoteFile;
import com.uteknoid.drive.lib.resources.status.OwnCloudVersion;
import com.uteknoid.drive.lib.resources.status.RemoteCapability;
import com.uteknoid.drive.operations.common.SyncOperation;
import com.uteknoid.drive.syncadapter.FileSyncAdapter;
import timber.log.Timber;

import java.util.ArrayList;

/**
 * Operation performing a REFRESH on a folder, conceived to be triggered by an action started
 * FROM THE USER INTERFACE.
 *
 * Fetches the LIST and properties of the files contained in the given folder (including the
 * properties of the folder itself), and updates the local database with them.
 *
 * Synchronizes the CONTENTS of any file or folder set locally as AVAILABLE OFFLINE.
 *
 * If the folder is ROOT, it also retrieves the VERSION of the server, and the USER PROFILE info.
 *
 * Does NOT travel subfolders to refresh their contents also, UNLESS they are
 * set as AVAILABLE OFFLINE FOLDERS.
 */
public class RefreshFolderOperation extends SyncOperation<ArrayList<RemoteFile>> {

    public static final String EVENT_SINGLE_FOLDER_CONTENTS_SYNCED =
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_CONTENTS_SYNCED";
    public static final String EVENT_SINGLE_FOLDER_SHARES_SYNCED =
            RefreshFolderOperation.class.getName() + ".EVENT_SINGLE_FOLDER_SHARES_SYNCED";

    /**
     * Locally cached information about folder to synchronize
     */
    private OCFile mLocalFolder;

    /**
     * Account where the file to synchronize belongs
     */
    private Account mAccount;

    /**
     * Android context; necessary to send requests to the download service
     */
    private Context mContext;

    /**
     * 'True' means that the list of files in the remote folder should
     * be fetched and merged locally even though the 'eTag' did not change.
     */
    private boolean mIgnoreETag;    // TODO - use it prefetching ETag of folder; two PROPFINDS, but better
    // TODO -   performance with (big) unchanged folders

    private LocalBroadcastManager mLocalBroadcastManager;

    private boolean syncVersionAndProfileEnabled = true;

    /**
     * Creates a new instance of {@link RefreshFolderOperation}.
     *
     * @param folder           Folder to synchronize.
     * @param ignoreETag       'True' means that the content of the remote folder should
     *                         be fetched and updated even though the 'eTag' did not
     *                         change.
     * @param account          ownCloud account where the folder is located.
     * @param context          Application context.
     */
    public RefreshFolderOperation(OCFile folder,
                                  boolean ignoreETag,
                                  Account account,
                                  Context context) {
        mLocalFolder = folder;
        mAccount = account;
        mContext = context;
        mIgnoreETag = ignoreETag;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
    }

    /**
     * Performs the synchronization.
     *
     * {@inheritDoc}
     */
    @Override
    protected RemoteOperationResult<ArrayList<RemoteFile>> run(OwnCloudClient client) {
        RemoteOperationResult<ArrayList<RemoteFile>> result;
        OwnCloudVersion serverVersion = null;

        // get 'fresh data' from the database
        OCFile tempFile = getStorageManager().getFileByPath(mLocalFolder.getRemotePath());

        if (tempFile != null) {
            mLocalFolder = tempFile;
        } else {
            Timber.w("File with path: " + mLocalFolder.getRemotePath() + " and account " + mAccount.name
                    + " could not be retrieved from database " + "for account : "
                    + getStorageManager().getAccount().name + " Let's try to synchronize it anyway");
        }

        // only in root folder: sync server version and user profile
        if (OCFile.ROOT_PATH.equals(mLocalFolder.getRemotePath()) && syncVersionAndProfileEnabled) {
            serverVersion = syncCapabilitiesAndGetServerVersion();
            syncUserProfile();
        }

        // sync list of files, and contents of available offline files & folders
        SynchronizeFolderOperation syncOp = new SynchronizeFolderOperation(
                mContext,
                mLocalFolder.getRemotePath(),
                mAccount,
                System.currentTimeMillis(),
                false,
                false,
                false
        );
        result = syncOp.execute(client, getStorageManager());

        sendLocalBroadcast(EVENT_SINGLE_FOLDER_CONTENTS_SYNCED, mLocalFolder.getRemotePath(), serverVersion, result);

        sendLocalBroadcast(EVENT_SINGLE_FOLDER_SHARES_SYNCED, mLocalFolder.getRemotePath(), serverVersion, result);

        return result;
    }

    private void syncUserProfile() {
        SyncProfileOperation syncProfileOperation = new SyncProfileOperation(getStorageManager().getAccount());
        syncProfileOperation.syncUserProfile();
    }

    /**
     * Normally user profile and owncloud version get synchronized if you sync the root directory.
     * With this you can override this behaviour and disable it, which is useful for the DocumentsProvider
     *
     * @param syncVersionAndProfileEnabled disables/enables sync Version/Profile when syncing root DIR
     */
    public void syncVersionAndProfileEnabled(boolean syncVersionAndProfileEnabled) {
        this.syncVersionAndProfileEnabled = syncVersionAndProfileEnabled;
    }

    private OwnCloudVersion syncCapabilitiesAndGetServerVersion() {
        OwnCloudVersion serverVersion;
        SyncCapabilitiesOperation getCapabilities = new SyncCapabilitiesOperation();
        RemoteOperationResult<RemoteCapability> result = getCapabilities.execute(getStorageManager(), mContext);
        if (result.isSuccess()) {
            RemoteCapability capability = result.getData();
            serverVersion = new OwnCloudVersion(capability.getVersionString());
        } else {
            // get whatever was stored before for the version
            serverVersion = AccountUtils.getServerVersion(mAccount);
        }
        return serverVersion;
    }

    /**
     * Sends a message to any application component interested in the progress
     * of the synchronization.
     *
     * @param event         Action type to broadcast
     * @param dirRemotePath Remote path of a folder that was just synchronized
     *                      (with or without success)
     */
    private void sendLocalBroadcast(String event, String dirRemotePath, OwnCloudVersion serverVersion,
                                    RemoteOperationResult result) {
        Timber.d("Send broadcast " + event);
        Intent intent = new Intent(event);
        intent.putExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME, mAccount.name);
        if (dirRemotePath != null) {
            intent.putExtra(FileSyncAdapter.EXTRA_FOLDER_PATH, dirRemotePath);
        }
        if (serverVersion != null) {
            intent.putExtra(FileSyncAdapter.EXTRA_SERVER_VERSION, serverVersion);
        }
        intent.putExtra(FileSyncAdapter.EXTRA_RESULT, result);
        mLocalBroadcastManager.sendBroadcast(intent);
    }
}
