/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author masensio
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
import android.webkit.MimeTypeMap;

import com.uteknoid.drive.datamodel.OCFile;
import com.uteknoid.drive.lib.common.OwnCloudClient;
import com.uteknoid.drive.lib.common.network.OnDatatransferProgressListener;
import com.uteknoid.drive.lib.common.operations.OperationCancelledException;
import com.uteknoid.drive.lib.common.operations.RemoteOperation;
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult;
import com.uteknoid.drive.lib.resources.files.DownloadRemoteFileOperation;
import com.uteknoid.drive.utils.FileStorageUtils;
import timber.log.Timber;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Remote mDownloadOperation performing the download of a file to an ownCloud server
 */
public class DownloadFileOperation extends RemoteOperation {

    private Account mAccount;
    private OCFile mFile;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<>();
    private long mModificationTimestamp = 0;
    private String mEtag = "";
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);

    private DownloadRemoteFileOperation mDownloadOperation;

    public DownloadFileOperation(Account account, OCFile file) {
        if (account == null) {
            throw new IllegalArgumentException("Illegal null account in DownloadFileOperation " +
                    "creation");
        }
        if (file == null) {
            throw new IllegalArgumentException("Illegal null file in DownloadFileOperation " +
                    "creation");
        }

        mAccount = account;
        mFile = file;

    }

    public Account getAccount() {
        return mAccount;
    }

    public OCFile getFile() {
        return mFile;
    }

    public String getSavePath() {
        String path = mFile.getStoragePath();  // re-downloads should be done over the original file
        if (path != null && path.length() > 0) {
            return path;
        }
        return FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
    }

    public String getTmpPath() {
        return FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
    }

    public String getTmpFolder() {
        return FileStorageUtils.getTemporalPath(mAccount.name);
    }

    public String getRemotePath() {
        return mFile.getRemotePath();
    }

    public String getMimeType() {
        String mimeType = mFile.getMimetype();
        if (mimeType == null || mimeType.length() <= 0) {
            try {
                mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                                mFile.getRemotePath().substring(
                                        mFile.getRemotePath().lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Timber.e("Trying to find out MIME type of a file without extension: %s", mFile.getRemotePath());
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    public long getSize() {
        return mFile.getFileLength();
    }

    public long getModificationTimestamp() {
        return (mModificationTimestamp > 0) ? mModificationTimestamp :
                mFile.getModificationTimestamp();
    }

    public String getEtag() {
        return mEtag;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        File newFile;
        boolean moved;

        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());

        String tmpFolder = getTmpFolder();

        /// perform the download
        synchronized (mCancellationRequested) {
            if (mCancellationRequested.get()) {
                return new RemoteOperationResult(new OperationCancelledException());
            }
        }

        mDownloadOperation = new DownloadRemoteFileOperation(mFile.getRemotePath(), tmpFolder);
        Iterator<OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
        while (listener.hasNext()) {
            mDownloadOperation.addDatatransferProgressListener(listener.next());
        }
        result = mDownloadOperation.execute(client);

        if (result.isSuccess()) {
            mModificationTimestamp = mDownloadOperation.getModificationTimestamp();
            mEtag = mDownloadOperation.getEtag();
            if (FileStorageUtils.getUsableSpace() < tmpFile.length()) {
                Timber.w("Not enough space to copy %s", tmpFile.getAbsolutePath());
            }
            newFile = new File(getSavePath());
            Timber.d("Save path: %s", newFile.getAbsolutePath());
            File parent = newFile.getParentFile();
            boolean created = parent.mkdirs();
            Timber.d("Creation of parent folder " + parent.getAbsolutePath() + " succeeded: " + created);
            Timber.d("Parent folder " + parent.getAbsolutePath() + " exists: " + parent.exists());
            Timber.d("Parent folder " + parent.getAbsolutePath() + " is directory: " + parent.isDirectory());
            moved = tmpFile.renameTo(newFile);
            Timber.d("New file " + newFile.getAbsolutePath() + " exists: " + newFile.exists());
            Timber.d("New file " + newFile.getAbsolutePath() + " is directory: " + newFile.isDirectory());
            if (!moved) {
                result = new RemoteOperationResult<>(
                        RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED);
            }
        }
        Timber.i("Download of " + mFile.getRemotePath() + " to " + getSavePath() + ": " + result.getLogMessage());

        return result;
    }

    public void cancel() {
        mCancellationRequested.set(true);   // atomic set; there is no need of synchronizing it
        if (mDownloadOperation != null) {
            mDownloadOperation.cancel();
        }
    }

    public void addDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
    }

    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
    }
}
