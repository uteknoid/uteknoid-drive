/**
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author masensio
 * @author David A. Velasco
 * @author Christian Schabesberger
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

package com.uteknoid.drive.datamodel;

import android.accounts.Account;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.uteknoid.drive.authentication.AccountUtils;
import com.uteknoid.drive.datamodel.UploadsStorageManager.UploadStatus;
import com.uteknoid.drive.db.UploadResult;
import com.uteknoid.drive.files.services.FileUploader;
import com.uteknoid.drive.operations.UploadFileOperation;
import com.uteknoid.drive.utils.MimetypeIconUtil;
import timber.log.Timber;

import java.io.File;

/**
 * Stores all information in order to start upload operations. PersistentUploadObject can
 * be stored persistently by {@link UploadsStorageManager}.
 */
public class OCUpload implements Parcelable {

    private long mId;

    /**
     * Absolute path in the local file system to the file to be uploaded
     */
    private String mLocalPath;

    /**
     * Absolute path in the remote account to set to the uploaded file (not for its parent folder!)
     */
    private String mRemotePath;

    /**
     * Name of Owncloud account to upload file to.
     */
    private String mAccountName;

    /**
     * File size
     */
    private long mFileSize;

    /**
     * Local action for upload. (0 - COPY, 1 - MOVE, 2 - FORGET)
     */
    private int mLocalAction;

    /**
     * Overwrite destination file?
     */
    private boolean mForceOverwrite;
    /**
     * Create destination folder?
     */
    private boolean mCreatesRemoteFolder;
    /**
     * Status of upload (later, in_progress, ...).
     */
    private UploadStatus mUploadStatus;
    /**
     * Result from last upload operation. Can be null.
     */
    private UploadResult mLastResult;

    /**
     * Defines the origin of the upload; see constants CREATED_ in {@link UploadFileOperation}
     */
    private int mCreatedBy;

    /*
     * When the upload ended
     */
    private long mUploadEndTimeStamp;

    /*
     * Used to identify remote chunks folders
     */
    private String mTransferId;

    /**
     * Main constructor
     *
     * @param localPath         Absolute path in the local file system to the file to be uploaded.
     * @param remotePath        Absolute path in the remote account to set to the uploaded file.
     * @param accountName       Name of an ownCloud account to update the file to.
     */
    public OCUpload(String localPath, String remotePath, String accountName) {
        if (localPath == null) {
            throw new IllegalArgumentException("Local path must be an absolute path in the local file system");
        }
        if (remotePath == null || !remotePath.startsWith(File.separator)) {
            throw new IllegalArgumentException("Remote path must be an absolute path in the local file system");
        }
        if (accountName == null || accountName.length() < 1) {
            throw new IllegalArgumentException("Invalid account name");
        }
        resetData();
        mLocalPath = localPath;
        mRemotePath = remotePath;
        mAccountName = accountName;
    }

    /**
     * Convenience constructor to reupload already existing {@link OCFile}s
     *
     * @param  ocFile           {@link OCFile} instance to update in the remote server.
     * @param  account          ownCloud {@link Account} where ocFile is contained.
     */
    public OCUpload(OCFile ocFile, Account account) {
        this(ocFile.getStoragePath(), ocFile.getRemotePath(), account.name);
    }

    /**
     * Reset all the fields to default values.
     */
    private void resetData() {
        mRemotePath = "";
        mLocalPath = "";
        mAccountName = "";
        mFileSize = -1;
        mId = -1;
        mLocalAction = FileUploader.LOCAL_BEHAVIOUR_COPY;
        mForceOverwrite = false;
        mCreatesRemoteFolder = false;
        mUploadStatus = UploadStatus.UPLOAD_IN_PROGRESS;
        mLastResult = UploadResult.UNKNOWN;
        mCreatedBy = UploadFileOperation.CREATED_BY_USER;
        mTransferId = "";
    }

    // Getters & Setters
    public void setUploadId(long id) {
        mId = id;
    }

    public long getUploadId() {
        return mId;
    }

    /**
     * @return the uploadStatus
     */
    public UploadStatus getUploadStatus() {
        return mUploadStatus;
    }

    /**
     * Sets uploadStatus AND SETS lastResult = null;
     * @param uploadStatus the uploadStatus to set
     */
    public void setUploadStatus(UploadStatus uploadStatus) {
        this.mUploadStatus = uploadStatus;
        setLastResult(UploadResult.UNKNOWN);
    }

    /**
     * @return the lastResult
     */
    public UploadResult getLastResult() {
        return mLastResult;
    }

    /**
     * @param lastResult the lastResult to set
     */
    public void setLastResult(UploadResult lastResult) {
        this.mLastResult = ((lastResult != null) ? lastResult : UploadResult.UNKNOWN);
    }

    /**
     * @return the localPath
     */
    public String getLocalPath() {
        return mLocalPath;
    }

    public void setLocalPath(String localPath) {
        mLocalPath = localPath;
    }

    /**
     * @return the remotePath
     */
    public String getRemotePath() {
        return mRemotePath;
    }

    /**
     * @param remotePath
     */
    public void setRemotePath(String remotePath) {
        mRemotePath = remotePath;
    }

    /**
     * @return File size
     */
    public long getFileSize() {
        return mFileSize;
    }

    public void setFileSize(long fileSize) {
        mFileSize = fileSize;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return MimetypeIconUtil.getBestMimeTypeByFilename(mLocalPath);
    }

    /**
     * @return the localAction
     */
    public int getLocalAction() {
        return mLocalAction;
    }

    /**
     * @param localAction the localAction to set
     */
    public void setLocalAction(int localAction) {
        this.mLocalAction = localAction;
    }

    /**
     * @return the forceOverwrite
     */
    public boolean isForceOverwrite() {
        return mForceOverwrite;
    }

    /**
     * @param forceOverwrite the forceOverwrite to set
     */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.mForceOverwrite = forceOverwrite;
    }

    /**
     * @return true if remote folder needs to be created, false otherwise
     */
    public boolean createsRemoteFolder() {
        return mCreatesRemoteFolder;
    }

    /**
     * @param mCreatesRemoteFolder folder needs to be created or not
     */
    public void setCreateRemoteFolder(boolean mCreatesRemoteFolder) {
        this.mCreatesRemoteFolder = mCreatesRemoteFolder;
    }

    /**
     * @return the accountName
     */
    public String getAccountName() {
        return mAccountName;
    }

    /**
     * Returns owncloud account as {@link Account} object.  
     */
    public Account getAccount(Context context) {
        return AccountUtils.getOwnCloudAccountByName(context, getAccountName());
    }

    public void setCreatedBy(int createdBy) {
        mCreatedBy = createdBy;
    }

    public int getCreatedBy() {
        return mCreatedBy;
    }

    public void setUploadEndTimestamp(long uploadEndTimestamp) {
        mUploadEndTimeStamp = uploadEndTimestamp;
    }

    public long getUploadEndTimestamp() {
        return mUploadEndTimeStamp;
    }

    public void setTransferId(String transferId) {
        mTransferId = transferId;
    }

    public String getTransferId() {
        return mTransferId;
    }

    /**
     * For debugging purposes only.
     */
    public String toFormattedString() {
        try {
            String localPath = getLocalPath() != null ? getLocalPath() : "";
            return localPath + " status:" + getUploadStatus() + " result:" +
                    (getLastResult() == null ? "null" : getLastResult().getValue());
        } catch (NullPointerException e) {
            Timber.d("Exception %s", e.toString());
            return (e.toString());
        }
    }

    /****
     *
     */
    public static final Parcelable.Creator<OCUpload> CREATOR = new Parcelable.Creator<OCUpload>() {

        @Override
        public OCUpload createFromParcel(Parcel source) {
            return new OCUpload(source);
        }

        @Override
        public OCUpload[] newArray(int size) {
            return new OCUpload[size];
        }
    };

    /**
     * Reconstruct from parcel
     *
     * @param source The source parcel
     */
    protected OCUpload(Parcel source) {
        readFromParcel(source);
    }

    public void readFromParcel(Parcel source) {
        mId = source.readLong();
        mLocalPath = source.readString();
        mRemotePath = source.readString();
        mAccountName = source.readString();
        mFileSize = source.readLong();
        mLocalAction = source.readInt();
        mForceOverwrite = (source.readInt() == 1);
        mCreatesRemoteFolder = (source.readInt() == 1);
        try {
            mUploadStatus = UploadStatus.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mUploadStatus = UploadStatus.UPLOAD_IN_PROGRESS;
        }
        mUploadEndTimeStamp = source.readLong();
        try {
            mLastResult = UploadResult.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mLastResult = UploadResult.UNKNOWN;
        }
        mCreatedBy = source.readInt();
        mTransferId = source.readString();
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mLocalPath);
        dest.writeString(mRemotePath);
        dest.writeString(mAccountName);
        dest.writeLong(mFileSize);
        dest.writeInt(mLocalAction);
        dest.writeInt(mForceOverwrite ? 1 : 0);
        dest.writeInt(mCreatesRemoteFolder ? 1 : 0);
        dest.writeString(mUploadStatus.name());
        dest.writeLong(mUploadEndTimeStamp);
        dest.writeString(((mLastResult == null) ? "" : mLastResult.name()));
        dest.writeInt(mCreatedBy);
        dest.writeString(mTransferId);
    }

    enum CanUploadFileNowStatus {NOW, LATER, FILE_GONE, ERROR}

}
