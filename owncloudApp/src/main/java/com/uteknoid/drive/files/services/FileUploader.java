/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author masensio
 * @author LukeOwnCloud
 * @author David A. Velasco
 * @author Christian Schabesberger
 * @author David González Verdugo
 * <p>
 * Copyright (C) 2012 Bartek Przybylski
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

package com.uteknoid.drive.files.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.util.Pair;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.uteknoid.drive.R;
import com.uteknoid.drive.authentication.AccountUtils;
import com.uteknoid.drive.datamodel.FileDataStorageManager;
import com.uteknoid.drive.datamodel.OCFile;
import com.uteknoid.drive.datamodel.OCUpload;
import com.uteknoid.drive.datamodel.UploadsStorageManager;
import com.uteknoid.drive.datamodel.UploadsStorageManager.UploadStatus;
import com.uteknoid.drive.db.UploadResult;
import com.uteknoid.drive.domain.capabilities.model.OCCapability;
import com.uteknoid.drive.lib.common.OwnCloudAccount;
import com.uteknoid.drive.lib.common.OwnCloudClient;
import com.uteknoid.drive.lib.common.SingleSessionManager;
import com.uteknoid.drive.lib.common.network.OnDatatransferProgressListener;
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult;
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult.ResultCode;
import com.uteknoid.drive.lib.resources.files.chunks.ChunkedUploadRemoteFileOperation;
import com.uteknoid.drive.operations.ChunkedUploadFileOperation;
import com.uteknoid.drive.operations.RemoveChunksFolderOperation;
import com.uteknoid.drive.operations.UploadFileOperation;
import com.uteknoid.drive.ui.activity.FileActivity;
import com.uteknoid.drive.ui.activity.UploadListActivity;
import com.uteknoid.drive.ui.errorhandling.ErrorMessageAdapter;
import com.uteknoid.drive.utils.Extras;
import com.uteknoid.drive.utils.NotificationUtils;
import com.uteknoid.drive.utils.SecurityUtils;
import timber.log.Timber;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import static com.uteknoid.drive.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_PICTURE;
import static com.uteknoid.drive.operations.UploadFileOperation.CREATED_AS_CAMERA_UPLOAD_VIDEO;
import static com.uteknoid.drive.utils.NotificationConstantsKt.UPLOAD_NOTIFICATION_CHANNEL_ID;

/**
 * Service for uploading files. Invoke using context.startService(...).
 * <p>
 * Files to be uploaded are stored persistently using {@link UploadsStorageManager}.
 * <p>
 * On next invocation of {@link FileUploader} uploaded files which
 * previously failed will be uploaded again until either upload succeeded or a
 * fatal error occurred.
 * <p>
 * Every file passed to this service is uploaded. No filtering is performed.
 */
public class FileUploader extends Service
        implements OnDatatransferProgressListener, OnAccountsUpdateListener,
        UploadFileOperation.OnRenameListener {

    private static final String UPLOADS_ADDED_MESSAGE = "UPLOADS_ADDED";
    private static final String UPLOAD_START_MESSAGE = "UPLOAD_START";
    private static final String UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH";

    protected static final String KEY_FILE = "FILE";
    protected static final String KEY_LOCAL_FILE = "LOCAL_FILE";
    protected static final String KEY_REMOTE_FILE = "REMOTE_FILE";
    protected static final String KEY_MIME_TYPE = "MIME_TYPE";
    protected static final String KEY_IS_AVAILABLE_OFFLINE_FILE = "KEY_IS_AVAILABLE_OFFLINE_FILE";
    protected static final String KEY_REQUESTED_FROM_WIFI_BACK_EVENT = "KEY_REQUESTED_FROM_WIFI_BACK_EVENT";

    /**
     * Call this Service with only this Intent key if all pending uploads are to be retried.
     */
    protected static final String KEY_RETRY = "KEY_RETRY";
    /**
     * Call this Service with KEY_RETRY and KEY_RETRY_UPLOAD to retry
     * upload of file identified by KEY_RETRY_UPLOAD.
     */
    protected static final String KEY_RETRY_UPLOAD = "KEY_RETRY_UPLOAD";
    /**
     * {@link Account} to which file is to be uploaded.
     */
    protected static final String KEY_ACCOUNT = "ACCOUNT";

    /**
     * Set to true if remote file is to be overwritten. Default action is to upload with different name.
     */
    protected static final String KEY_FORCE_OVERWRITE = "KEY_FORCE_OVERWRITE";
    /**
     * Set to true if remote folder is to be created if it does not exist.
     */
    protected static final String KEY_CREATE_REMOTE_FOLDER = "CREATE_REMOTE_FOLDER";
    /**
     * Key to signal what is the origin of the upload request
     */
    protected static final String KEY_CREATED_BY = "CREATED_BY";
    /**
     * Set to true if upload is to performed only when phone is being charged.
     */
    protected static final String KEY_WHILE_CHARGING_ONLY = "KEY_WHILE_CHARGING_ONLY";

    protected static final String KEY_LOCAL_BEHAVIOUR = "BEHAVIOUR";

    public static final int LOCAL_BEHAVIOUR_COPY = 0;
    public static final int LOCAL_BEHAVIOUR_MOVE = 1;
    public static final int LOCAL_BEHAVIOUR_FORGET = 2;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private OwnCloudClient mUploadClient = null;
    private Account mCurrentAccount = null;
    private FileDataStorageManager mStorageManager;
    //since there can be only one instance of an Android service, there also just one db connection.
    private UploadsStorageManager mUploadsStorageManager = null;

    private IndexedForest<UploadFileOperation> mPendingUploads = new IndexedForest<>();

    private LocalBroadcastManager mLocalBroadcastManager;

    /**
     * {@link UploadFileOperation} object of ongoing upload. Can be null. Note: There can only be one concurrent upload!
     */
    private UploadFileOperation mCurrentUpload = null;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private int mLastPercent;

    public static String getUploadsAddedMessage() {
        return FileUploader.class.getName() + UPLOADS_ADDED_MESSAGE;
    }

    public static String getUploadStartMessage() {
        return FileUploader.class.getName() + UPLOAD_START_MESSAGE;
    }

    public static String getUploadFinishMessage() {
        return FileUploader.class.getName() + UPLOAD_FINISH_MESSAGE;
    }

    @Override
    public void onRenameUpload() {
        mUploadsStorageManager.updateDatabaseUploadStart(mCurrentUpload);
        sendBroadcastUploadStarted(mCurrentUpload);
    }

    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("Creating service");

        mNotificationBuilder = NotificationUtils.newNotificationBuilder(this, UPLOAD_NOTIFICATION_CHANNEL_ID);

        HandlerThread thread = new HandlerThread("FileUploaderThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileUploaderBinder();

        mUploadsStorageManager = new UploadsStorageManager(getContentResolver());

        int failedCounter = mUploadsStorageManager.failInProgressUploads(
                UploadResult.SERVICE_INTERRUPTED    // Add UploadResult.KILLED?
        );
        if (failedCounter > 0) {
            resurrection();
        }

        // add AccountsUpdatedListener
        AccountManager am = AccountManager.get(getApplicationContext());
        am.addOnAccountsUpdatedListener(this, null, false);

        // create manager for local broadcasts
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    /**
     * Service clean-up when restarted after being killed
     */
    private void resurrection() {
        // remove stucked notification
        getNotificationManager().cancel(R.string.uploader_upload_in_progress_ticker);
    }

    /**
     * Service clean up
     */
    @Override
    public void onDestroy() {
        Timber.v("Destroying service");
        mBinder = null;
        mServiceHandler = null;
        mServiceLooper.quit();
        mServiceLooper = null;
        mNotificationManager = null;

        // remove AccountsUpdatedListener
        AccountManager am = AccountManager.get(getApplicationContext());
        am.removeOnAccountsUpdatedListener(this);

        super.onDestroy();
    }

    /**
     * Entry point to add one or several files to the queue of uploads.
     * <p>
     * New uploads are added calling to startService(), resulting in a call to
     * this method. This ensures the service will keep on working although the
     * caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("Starting command with id %s", startId);

        int createdBy = intent.getIntExtra(KEY_CREATED_BY, UploadFileOperation.CREATED_BY_USER);
        boolean isCameraUploadFile =
                createdBy == CREATED_AS_CAMERA_UPLOAD_PICTURE || createdBy == CREATED_AS_CAMERA_UPLOAD_VIDEO;
        boolean isAvailableOfflineFile = intent.getBooleanExtra(KEY_IS_AVAILABLE_OFFLINE_FILE, false);
        boolean isRequestedFromWifiBackEvent = intent.getBooleanExtra(
                KEY_REQUESTED_FROM_WIFI_BACK_EVENT, false
        );

        if ((isCameraUploadFile || isAvailableOfflineFile || isRequestedFromWifiBackEvent) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Timber.d("Starting FileUploader service in foreground");

            if (isCameraUploadFile) {
                mNotificationBuilder.setContentTitle(getString(R.string.uploader_upload_camera_upload_files));
            } else if (isAvailableOfflineFile) {
                mNotificationBuilder.setContentTitle(getString(R.string.uploader_upload_available_offline_files));
            } else if (isRequestedFromWifiBackEvent) {
                mNotificationBuilder.setContentTitle(getString(R.string.uploader_upload_requested_from_wifi_files));
            }

            /*
             * After calling startForegroundService method from {@link TransferRequester} for camera uploads or
             * available offline, we have to call this within five seconds after the service is created to avoid
             * an error
             */
            startForeground(141, mNotificationBuilder.build());
        }

        boolean retry = intent.getBooleanExtra(KEY_RETRY, false);
        AbstractList<String> requestedUploads = new Vector<>();

        if (!intent.hasExtra(KEY_ACCOUNT)) {
            Timber.e("Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }

        Account account = intent.getParcelableExtra(KEY_ACCOUNT);
        Timber.d("Account to upload the file to: %s", account);
        if (account == null || !AccountUtils.exists(account.name, getApplicationContext())) {
            return Service.START_NOT_STICKY;
        }

        if (!retry) {
            if (!(intent.hasExtra(KEY_LOCAL_FILE) || intent.hasExtra(KEY_FILE))) {
                Timber.e("Not enough information provided in intent");
                return Service.START_NOT_STICKY;
            }

            String[] localPaths = null, remotePaths = null, mimeTypes = null;
            OCFile[] files = null;

            if (intent.hasExtra(KEY_FILE)) {
                Parcelable[] files_temp = intent.getParcelableArrayExtra(KEY_FILE);
                files = new OCFile[files_temp.length];
                System.arraycopy(files_temp, 0, files, 0, files_temp.length);

            } else {
                localPaths = intent.getStringArrayExtra(KEY_LOCAL_FILE);
                remotePaths = intent.getStringArrayExtra(KEY_REMOTE_FILE);
                mimeTypes = intent.getStringArrayExtra(KEY_MIME_TYPE);
            }

            boolean forceOverwrite = intent.getBooleanExtra(KEY_FORCE_OVERWRITE, false);
            int localAction = intent.getIntExtra(KEY_LOCAL_BEHAVIOUR, LOCAL_BEHAVIOUR_FORGET);

            boolean isCreateRemoteFolder = intent.getBooleanExtra(KEY_CREATE_REMOTE_FOLDER, false);

            if (intent.hasExtra(KEY_FILE) && files == null) {
                Timber.e("Incorrect array for OCFiles provided in upload intent");
                return Service.START_NOT_STICKY;

            } else if (!intent.hasExtra(KEY_FILE)) {
                if (localPaths == null) {
                    Timber.e("Incorrect array for local paths provided in upload intent");
                    return Service.START_NOT_STICKY;
                }
                if (remotePaths == null) {
                    Timber.e("Incorrect array for remote paths provided in upload intent");
                    return Service.START_NOT_STICKY;
                }
                if (localPaths.length != remotePaths.length) {
                    Timber.e("Different number of remote paths and local paths!");
                    return Service.START_NOT_STICKY;
                }

                files = new OCFile[localPaths.length];
                for (int i = 0; i < localPaths.length; i++) {
                    files[i] = UploadFileOperation.obtainNewOCFileToUpload(
                            remotePaths[i],
                            localPaths[i],
                            ((mimeTypes != null) ? mimeTypes[i] : null),
                            getApplicationContext()
                    );
                    if (files[i] == null) {
                        Timber.e("obtainNewOCFileToUpload() returned null for remotePaths[i]:" + remotePaths[i]
                                + " and localPaths[i]:" + localPaths[i]);
                        return Service.START_NOT_STICKY;
                    }
                }
            }
            // at this point variable "OCFile[] files" is loaded correctly.

            String uploadKey;
            UploadFileOperation newUploadFileOperation;
            try {
                FileDataStorageManager storageManager = new FileDataStorageManager(
                        getApplicationContext(),
                        account,
                        getContentResolver()
                );
                OCCapability capabilitiesForAccount = storageManager.getCapability(account.name);
                boolean isChunkingAllowed =
                        capabilitiesForAccount != null && capabilitiesForAccount.isChunkingAllowed();
                Timber.d("Chunking is allowed: %s", isChunkingAllowed);
                for (OCFile ocFile : files) {

                    OCUpload ocUpload = new OCUpload(ocFile, account);
                    ocUpload.setFileSize(ocFile.getFileLength());
                    ocUpload.setForceOverwrite(forceOverwrite);
                    ocUpload.setCreateRemoteFolder(isCreateRemoteFolder);
                    ocUpload.setCreatedBy(createdBy);
                    ocUpload.setLocalAction(localAction);
                    /*ocUpload.setUseWifiOnly(isUseWifiOnly);
                    ocUpload.setWhileChargingOnly(isWhileChargingOnly);*/
                    ocUpload.setUploadStatus(UploadStatus.UPLOAD_IN_PROGRESS);

                    if (new File(ocFile.getStoragePath()).length() >
                            ChunkedUploadRemoteFileOperation.CHUNK_SIZE && isChunkingAllowed) {
                        ocUpload.setTransferId(
                                SecurityUtils.stringToMD5Hash(ocFile.getRemotePath()) + System.currentTimeMillis());
                        newUploadFileOperation = new ChunkedUploadFileOperation(
                                account,
                                ocFile,
                                ocUpload,
                                forceOverwrite,
                                localAction,
                                this
                        );
                    } else {
                        newUploadFileOperation = new UploadFileOperation(
                                account,
                                ocFile,
                                ocUpload,
                                forceOverwrite,
                                localAction,
                                this
                        );
                    }

                    newUploadFileOperation.setCreatedBy(createdBy);
                    if (isCreateRemoteFolder) {
                        newUploadFileOperation.setRemoteFolderToBeCreated();
                    }
                    newUploadFileOperation.addDatatransferProgressListener(this);
                    newUploadFileOperation.addDatatransferProgressListener((FileUploaderBinder) mBinder);

                    newUploadFileOperation.addRenameUploadListener(this);

                    Pair<String, String> putResult = mPendingUploads.putIfAbsent(
                            account.name,
                            ocFile.getRemotePath(),
                            newUploadFileOperation
                    );
                    if (putResult != null) {
                        uploadKey = putResult.first;
                        requestedUploads.add(uploadKey);

                        // Save upload in database
                        long id = mUploadsStorageManager.storeUpload(ocUpload);
                        newUploadFileOperation.setOCUploadId(id);
                    }
                }

            } catch (IllegalArgumentException e) {
                Timber.e(e, "Not enough information provided in intent: %s", e.getMessage());
                return START_NOT_STICKY;

            } catch (IllegalStateException e) {
                Timber.e(e, "Bad information provided in intent: %s", e.getMessage());
                return START_NOT_STICKY;

            } catch (Exception e) {
                Timber.e(e, "Unexpected exception while processing upload intent");
                return START_NOT_STICKY;

            }
            // *** TODO REWRITE: block inserted to request A retry; too many code copied, no control exception ***/
        } else {
            if (!intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_RETRY_UPLOAD)) {
                Timber.e("Not enough information provided in intent: no KEY_RETRY_UPLOAD_KEY");
                return START_NOT_STICKY;
            }
            OCUpload upload = intent.getParcelableExtra(KEY_RETRY_UPLOAD);

            UploadFileOperation newUploadFileOperation;

            if (upload.getFileSize() > ChunkedUploadRemoteFileOperation.CHUNK_SIZE) {
                upload.setTransferId(
                        SecurityUtils.stringToMD5Hash(upload.getRemotePath()) + System.currentTimeMillis());
                newUploadFileOperation = new ChunkedUploadFileOperation(
                        account,
                        null,
                        upload,
                        upload.isForceOverwrite(),
                        upload.getLocalAction(),
                        this
                );
            } else {
                newUploadFileOperation = new UploadFileOperation(
                        account,
                        null,
                        upload,
                        upload.isForceOverwrite(),
                        upload.getLocalAction(),
                        this
                );
            }

            newUploadFileOperation.addDatatransferProgressListener(this);
            newUploadFileOperation.addDatatransferProgressListener((FileUploaderBinder) mBinder);

            newUploadFileOperation.addRenameUploadListener(this);

            Pair<String, String> putResult = mPendingUploads.putIfAbsent(
                    account.name,
                    upload.getRemotePath(),
                    newUploadFileOperation
            );
            if (putResult != null) {
                String uploadKey = putResult.first;
                requestedUploads.add(uploadKey);

                // Update upload in database
                upload.setUploadStatus(UploadStatus.UPLOAD_IN_PROGRESS);
                mUploadsStorageManager.updateUpload(upload);
            }
        }
        // *** TODO REWRITE END ***/

        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
            sendBroadcastUploadsAdded();
        }
        return Service.START_NOT_STICKY;
    }

    /**
     * Provides a binder object that clients can use to perform operations on
     * the queue of uploads, excepting the addition of new files.
     * <p>
     * Implemented to perform cancellation, pause and resume of existing
     * uploads.
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    /**
     * Called when ALL the bound clients were onbound.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        ((FileUploaderBinder) mBinder).clearListeners();
        return false;   // not accepting rebinding (default behaviour)
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // Review current upload, and cancel it if its account doesn't exist
        if (mCurrentUpload != null &&
                !AccountUtils.exists(mCurrentUpload.getAccount().name, getApplicationContext())) {
            mCurrentUpload.cancel();
        }
        // The rest of uploads are cancelled when they try to start
    }

    /**
     * Binder to let client components to perform operations on the queue of
     * uploads.
     * <p/>
     * It provides by itself the available operations.
     */
    public class FileUploaderBinder extends Binder implements OnDatatransferProgressListener {

        /**
         * Map of listeners that will be reported about progress of uploads from a
         * {@link FileUploaderBinder} instance
         */
        private Map<String, WeakReference<OnDatatransferProgressListener>> mBoundListeners =
                new HashMap<>();

        /**
         * Cancels a pending or current upload of a remote file.
         *
         * @param account ownCloud account where the remote file will be stored.
         * @param file    A file in the queue of pending uploads
         */
        public void cancel(Account account, OCFile file) {
            cancel(account.name, file.getRemotePath());
        }

        /**
         * Cancels a pending or current upload that was persisted.
         *
         * @param storedUpload Upload operation persisted
         */
        public void cancel(OCUpload storedUpload) {
            cancel(storedUpload.getAccountName(), storedUpload.getRemotePath());
        }

        /**
         * Cancels a pending or current upload of a remote file.
         *
         * @param accountName Local name of an ownCloud account where the remote file will be stored.
         * @param remotePath  Remote target of the upload
         */
        private void cancel(String accountName, String remotePath) {
            Pair<UploadFileOperation, String> removeResult =
                    mPendingUploads.remove(accountName, remotePath);
            UploadFileOperation upload = removeResult.first;
            if (upload == null &&
                    mCurrentUpload != null && mCurrentAccount != null &&
                    mCurrentUpload.getRemotePath().startsWith(remotePath) &&
                    accountName.equals(mCurrentAccount.name)) {

                upload = mCurrentUpload;
            }
            if (upload != null) {
                upload.cancel();
                // need to update now table in mUploadsStorageManager,
                // since the operation will not get to be run by FileUploader#uploadFile
                mUploadsStorageManager.removeUpload(
                        accountName,
                        remotePath
                );
            }
        }

        /**
         * Cancels all the uploads for an account
         *
         * @param account ownCloud account.
         */
        public void cancel(Account account) {
            Timber.d("Account= %s", account.name);

            if (mCurrentUpload != null) {
                Timber.d("Current Upload Account= %s", mCurrentUpload.getAccount().name);
                if (mCurrentUpload.getAccount().name.equals(account.name)) {
                    mCurrentUpload.cancel();
                }
            }
            // Cancel pending uploads
            cancelUploadsForAccount(account);
        }

        public void clearListeners() {
            mBoundListeners.clear();
        }

        /**
         * Returns True when the file described by 'file' is being uploaded to
         * the ownCloud account 'account' or waiting for it
         * <p>
         * If 'file' is a directory, returns 'true' if some of its descendant files
         * is uploading or waiting to upload.
         * <p>
         * Warning: If remote file exists and !forceOverwrite the original file
         * is being returned here. That is, it seems as if the original file is
         * being updated when actually a new file is being uploaded.
         *
         * @param account Owncloud account where the remote file will be stored.
         * @param file    A file that could be in the queue of pending uploads
         */
        public boolean isUploading(Account account, OCFile file) {
            if (account == null || file == null) {
                return false;
            }
            return (mPendingUploads.contains(account.name, file.getRemotePath()));
        }

        public boolean isUploadingNow(OCUpload upload) {
            return (
                    upload != null &&
                            mCurrentAccount != null &&
                            mCurrentUpload != null &&
                            upload.getAccountName().equals(mCurrentAccount.name) &&
                            upload.getRemotePath().equals(mCurrentUpload.getRemotePath())
            );
        }

        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param account  ownCloud account holding the file of interest.
         * @param file     {@link OCFile} of interest for listener.
         */
        public void addDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                Account account,
                OCFile file
        ) {
            if (account == null || file == null || listener == null) {
                return;
            }
            String targetKey = buildRemoteName(account.name, file.getRemotePath());
            mBoundListeners.put(targetKey, new WeakReference<>(listener));
        }

        /**
         * Adds a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param ocUpload {@link OCUpload} of interest for listener.
         */
        public void addDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                OCUpload ocUpload
        ) {
            if (ocUpload == null || listener == null) {
                return;
            }
            String targetKey = buildRemoteName(ocUpload.getAccountName(), ocUpload.getRemotePath());
            mBoundListeners.put(targetKey, new WeakReference<>(listener));
        }

        /**
         * Removes a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param account  ownCloud account holding the file of interest.
         * @param file     {@link OCFile} of interest for listener.
         */
        public void removeDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                Account account,
                OCFile file
        ) {
            if (account == null || file == null || listener == null) {
                return;
            }
            String targetKey = buildRemoteName(account.name, file.getRemotePath());
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }

        /**
         * Removes a listener interested in the progress of the upload for a concrete file.
         *
         * @param listener Object to notify about progress of transfer.
         * @param ocUpload Stored upload of interest
         */
        public void removeDatatransferProgressListener(
                OnDatatransferProgressListener listener,
                OCUpload ocUpload
        ) {
            if (ocUpload == null || listener == null) {
                return;
            }
            String targetKey = buildRemoteName(ocUpload.getAccountName(), ocUpload.getRemotePath());
            if (mBoundListeners.get(targetKey) == listener) {
                mBoundListeners.remove(targetKey);
            }
        }

        /**
         * Builds a key for the map of listeners.
         * <p/>
         * TODO use method in IndexedForest, or refactor both to a common place
         * add to local database) to better policy (add to local database, then upload)
         *
         * @param accountName Local name of the ownCloud account where the file to upload belongs.
         * @param remotePath  Remote path to upload the file to.
         * @return Key
         */
        private String buildRemoteName(String accountName, String remotePath) {
            return accountName + remotePath;
        }

        @Override
        public void onTransferProgress(long read, long transferred, long total, String absolutePath) {
            String key = buildRemoteName(mCurrentUpload.getAccount().name, mCurrentUpload.getFile().getRemotePath());
            WeakReference<OnDatatransferProgressListener> boundListenerRef = mBoundListeners.get(key);
            if (boundListenerRef != null && boundListenerRef.get() != null) {
                boundListenerRef.get().onTransferProgress(read, transferred, total, absolutePath);
            }
        }
    }

    /**
     * Upload worker. Performs the pending uploads in the order they were
     * requested.
     * <p>
     * Created with the Looper of a new thread, started in
     * {@link FileUploader#onCreate()}.
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will
        // warn about a possible memory leak
        FileUploader mService;

        public ServiceHandler(Looper looper, FileUploader service) {
            super(looper);
            if (service == null) {
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            }
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            AbstractList<String> requestedUploads = (AbstractList<String>) msg.obj;
            if (msg.obj != null) {
                Iterator<String> it = requestedUploads.iterator();
                while (it.hasNext()) {
                    mService.uploadFile(it.next());
                }
            }
            Timber.d("Stopping command after id %s", msg.arg1);
            mService.stopForeground(true);
            mService.stopSelf(msg.arg1);
        }
    }

    /**
     * Core upload method: sends the file(s) to upload
     *
     * @param uploadKey Key to access the upload to perform, contained in mPendingUploads
     */
    public void uploadFile(String uploadKey) {

        mCurrentUpload = mPendingUploads.get(uploadKey);

        if (mCurrentUpload != null) {

            /// Check account existence
            if (!AccountUtils.exists(mCurrentUpload.getAccount().name, this)) {
                Timber.w("Account " + mCurrentUpload.getAccount().name + " does not exist anymore -> cancelling all " +
                        "its uploads");
                cancelUploadsForAccount(mCurrentUpload.getAccount());
                return;
            }

            /// OK, let's upload
            mUploadsStorageManager.updateDatabaseUploadStart(mCurrentUpload);

            notifyUploadStart(mCurrentUpload);

            sendBroadcastUploadStarted(mCurrentUpload);

            RemoteOperationResult uploadResult = null;

            try {
                /// prepare client object to send the request to the ownCloud server
                if (mCurrentAccount == null ||
                        !mCurrentAccount.equals(mCurrentUpload.getAccount())) {
                    mCurrentAccount = mCurrentUpload.getAccount();
                    mStorageManager = new FileDataStorageManager(
                            getApplicationContext(),
                            mCurrentAccount,
                            getContentResolver()
                    );
                }   // else, reuse storage manager from previous operation

                // always get client from client manager to get fresh credentials in case of update
                OwnCloudAccount ocAccount = new OwnCloudAccount(
                        mCurrentAccount,
                        this
                );
                mUploadClient = SingleSessionManager.getDefaultSingleton().
                        getClientFor(ocAccount, this);

                /// perform the upload
                uploadResult = mCurrentUpload.execute(mUploadClient, mStorageManager);

            } catch (Exception e) {
                Timber.e(e, "Error uploading");
                uploadResult = new RemoteOperationResult(e);

            } finally {
                Pair<UploadFileOperation, String> removeResult;
                if (mCurrentUpload.wasRenamed()) {
                    removeResult = mPendingUploads.removePayload(
                            mCurrentAccount.name,
                            mCurrentUpload.getOldFile().getRemotePath()
                    );
                    /* TODO: grant that name is also updated for mCurrentUpload.getOCUploadId */

                } else {
                    removeResult = mPendingUploads.removePayload(
                            mCurrentAccount.name,
                            mCurrentUpload.getRemotePath()
                    );
                }

                if (uploadResult != null && !uploadResult.isSuccess()) {
                    TransferRequester requester = new TransferRequester();
                    int jobId = mPendingUploads.buildKey(
                            mCurrentAccount.name,
                            mCurrentUpload.getRemotePath()
                    ).hashCode();

                    if (uploadResult.getException() != null) {
                        // if failed due to lack of connectivity, schedule an automatic retry
                        if (requester.shouldScheduleRetry(this, uploadResult.getException())) {
                            requester.scheduleUpload(
                                    this,
                                    jobId,
                                    mCurrentAccount.name,
                                    mCurrentUpload.getRemotePath()
                            );
                            uploadResult = new RemoteOperationResult(
                                    ResultCode.NO_NETWORK_CONNECTION);
                        } else {
                            String stringToLog = String.format(
                                    "Exception in upload, network is OK, no retry scheduled for %1s in %2s",
                                    mCurrentUpload.getRemotePath(),
                                    mCurrentAccount.name
                            );
                            Timber.v("%s", stringToLog);
                        }
                    } else if (uploadResult.getCode() == ResultCode.DELAYED_FOR_WIFI) {
                        // if failed due to the upload is delayed for wifi, schedule automatic retry as well
                        requester.scheduleUpload(
                                this,
                                jobId,
                                mCurrentAccount.name,
                                mCurrentUpload.getRemotePath()
                        );
                    }
                } else {
                    String stringToLog = String.format(
                            "Success OR fail without exception for %1s in %2s",
                            mCurrentUpload.getRemotePath(),
                            mCurrentAccount.name
                    );
                    Timber.v("%s", stringToLog);
                }

                if (uploadResult != null) {
                    mUploadsStorageManager.updateDatabaseUploadResult(uploadResult, mCurrentUpload);
                    /// notify result
                    notifyUploadResult(mCurrentUpload, uploadResult);
                }

                sendBroadcastUploadFinished(mCurrentUpload, uploadResult, removeResult.second);
            }
        }
    }

    private void removeChunksFolder(long ocUploadId) {
        RemoveChunksFolderOperation remoteChunksFolderOperation = new RemoveChunksFolderOperation(
                String.valueOf(ocUploadId)
        );

        RemoteOperationResult result = remoteChunksFolderOperation.execute(mUploadClient);

        if (!result.isSuccess()) {
            Timber.e("Error deleting chunks folder after cancelling chunked upload");
        }
    }

    /**
     * Creates a status notification to show the upload progress
     *
     * @param upload Upload operation starting.
     */
    private void notifyUploadStart(UploadFileOperation upload) {
        Timber.d("Notifying upload start");

        // / create status notification with a progress bar
        mLastPercent = 0;
        mNotificationBuilder
                .setOngoing(true)
                .setTicker(getString(R.string.uploader_upload_in_progress_ticker))
                .setContentTitle(getString(R.string.uploader_upload_in_progress_ticker))
                .setProgress(100, 0, false)
                .setContentText(
                        String.format(getString(R.string.uploader_upload_in_progress_content), 0, upload.getFileName()))
                .setWhen(System.currentTimeMillis());

        /// includes a pending intent in the notification showing the details
        Intent showUploadListIntent = new Intent(this, UploadListActivity.class);
        showUploadListIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
        showUploadListIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
        showUploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                showUploadListIntent, NotificationUtils.INSTANCE.getPendingIntentFlags()));

        if (!upload.isCameraUploadsPicture() && !upload.isCameraUploadsVideo()) {
            getNotificationManager().notify(R.string.uploader_upload_in_progress_ticker,
                    mNotificationBuilder.build());
        }// else wait until the upload really start (onTransferProgress is called), so that if it's discarded
        // due to lack of Wifi, no notification is shown
    }

    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar,
                                   long totalToTransfer, String filePath) {
        int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
        if (percent != mLastPercent) {
            mNotificationBuilder.setProgress(100, percent, false);
            String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, fileName);
            mNotificationBuilder.setContentText(text);
            getNotificationManager().notify(R.string.uploader_upload_in_progress_ticker, mNotificationBuilder.build());
        }
        mLastPercent = percent;
    }

    /**
     * Updates the status notification with the result of an upload operation.
     *
     * @param uploadResult Result of the upload operation.
     * @param upload       Finished upload operation
     */
    private void notifyUploadResult(UploadFileOperation upload,
                                    RemoteOperationResult uploadResult) {
        Timber.d("NotifyUploadResult with resultCode: %s", uploadResult.getCode());
        // / cancelled operation or success -> silent removal of progress notification
        getNotificationManager().cancel(R.string.uploader_upload_in_progress_ticker);

        if (uploadResult.isCancelled() && upload instanceof ChunkedUploadFileOperation) {
            removeChunksFolder(upload.getOCUploadId());
        }

        if (!uploadResult.isCancelled() &&
                !uploadResult.getCode().equals(ResultCode.DELAYED_FOR_WIFI)) {

            // Show the result: success or fail notification
            int tickerId = (uploadResult.isSuccess()) ? R.string.uploader_upload_succeeded_ticker :
                    R.string.uploader_upload_failed_ticker;

            String content;

            // check credentials error
            boolean needsToUpdateCredentials = (ResultCode.UNAUTHORIZED.equals(uploadResult.getCode()));
            tickerId = (needsToUpdateCredentials) ?
                    R.string.uploader_upload_failed_credentials_error : tickerId;

            mNotificationBuilder
                    .setTicker(getString(tickerId))
                    .setContentTitle(getString(tickerId))
                    .setAutoCancel(true)
                    .setOngoing(false)
                    .setProgress(0, 0, false);

            content = ErrorMessageAdapter.Companion.getResultMessage(
                    uploadResult, upload, getResources()
            );

            if (needsToUpdateCredentials) {
                // let the user update credentials with one click
                PendingIntent pendingIntentToRefreshCredentials =
                        NotificationUtils.INSTANCE.composePendingIntentToRefreshCredentials(this, upload.getAccount());

                mNotificationBuilder.setContentIntent(pendingIntentToRefreshCredentials);

            } else {
                mNotificationBuilder.setContentText(content);
            }

            if (!uploadResult.isSuccess() && !needsToUpdateCredentials) {
                //in case of failure, do not show details file view (because there is no file!)
                Intent showUploadListIntent = new Intent(this, UploadListActivity.class);
                showUploadListIntent.putExtra(FileActivity.EXTRA_FILE, upload.getFile());
                showUploadListIntent.putExtra(FileActivity.EXTRA_ACCOUNT, upload.getAccount());
                showUploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mNotificationBuilder.setContentIntent(PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                        showUploadListIntent, NotificationUtils.INSTANCE.getPendingIntentFlags()));
            }

            mNotificationBuilder.setContentText(content);

            getNotificationManager().notify(tickerId, mNotificationBuilder.build());

            if (uploadResult.isSuccess()) {
                mPendingUploads.remove(upload.getAccount().name, upload.getFile().getRemotePath());
                // remove success notification, with a delay of 2 seconds
                NotificationUtils.cancelWithDelay(
                        mNotificationManager,
                        R.string.uploader_upload_succeeded_ticker,
                        2000);

            }
        }
    }

    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     * <p>
     * TODO - no more broadcasts, replace with a callback to subscribed listeners
     */
    private void sendBroadcastUploadsAdded() {
        Intent start = new Intent(getUploadsAddedMessage());
        // nothing else needed right now
        mLocalBroadcastManager.sendBroadcast(start);
    }

    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     * <p>
     * TODO - no more broadcasts, replace with a callback to subscribed listeners
     *
     * @param upload Finished upload operation
     */
    private void sendBroadcastUploadStarted(
            UploadFileOperation upload) {

        Intent start = new Intent(getUploadStartMessage());
        start.putExtra(Extras.EXTRA_REMOTE_PATH, upload.getRemotePath()); // real remote
        start.putExtra(Extras.EXTRA_OLD_FILE_PATH, upload.getOriginalStoragePath());
        start.putExtra(Extras.EXTRA_ACCOUNT_NAME, upload.getAccount().name);

        mLocalBroadcastManager.sendBroadcast(start);
    }

    /**
     * Sends a broadcast in order to the interested activities can update their
     * view
     * <p>
     * TODO - no more broadcasts, replace with a callback to subscribed listeners
     *
     * @param upload                 Finished upload operation
     * @param uploadResult           Result of the upload operation
     * @param unlinkedFromRemotePath Path in the uploads tree where the upload was unlinked from
     */
    private void sendBroadcastUploadFinished(
            UploadFileOperation upload,
            RemoteOperationResult uploadResult,
            String unlinkedFromRemotePath) {

        Intent end = new Intent(getUploadFinishMessage());
        end.putExtra(Extras.EXTRA_REMOTE_PATH, upload.getRemotePath()); // real remote
        // path, after
        // possible
        // automatic
        // renaming
        if (upload.wasRenamed()) {
            end.putExtra(Extras.EXTRA_OLD_REMOTE_PATH, upload.getOldFile().getRemotePath());
        }
        end.putExtra(Extras.EXTRA_OLD_FILE_PATH, upload.getOriginalStoragePath());
        end.putExtra(Extras.EXTRA_ACCOUNT_NAME, upload.getAccount().name);
        end.putExtra(Extras.EXTRA_UPLOAD_RESULT, uploadResult.isSuccess());
        if (unlinkedFromRemotePath != null) {
            end.putExtra(Extras.EXTRA_LINKED_TO_PATH, unlinkedFromRemotePath);
        }

        mLocalBroadcastManager.sendBroadcast(end);
    }

    /**
     * Remove and 'forgets' pending uploads of an account.
     *
     * @param account Account which uploads will be cancelled
     */
    private void cancelUploadsForAccount(Account account) {
        mPendingUploads.remove(account.name);
        mUploadsStorageManager.removeUploads(account.name);
    }

    private NotificationManager getNotificationManager() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return mNotificationManager;
    }
}
