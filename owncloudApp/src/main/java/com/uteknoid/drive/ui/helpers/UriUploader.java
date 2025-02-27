/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2016 ownCloud GmbH.
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
package com.uteknoid.drive.ui.helpers;

import android.accounts.Account;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Parcelable;

import androidx.fragment.app.FragmentManager;
import com.uteknoid.drive.R;
import com.uteknoid.drive.files.services.FileUploader;
import com.uteknoid.drive.files.services.TransferRequester;
import com.uteknoid.drive.operations.UploadFileOperation;
import com.uteknoid.drive.ui.activity.FileActivity;
import com.uteknoid.drive.ui.asynctasks.CopyAndUploadContentUrisTask;
import com.uteknoid.drive.ui.fragment.TaskRetainerFragment;
import com.uteknoid.drive.utils.UriUtils;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.List;

/**
 * This class examines URIs pointing to files to upload and then requests {@link FileUploader} to upload them.
 *
 * URIs with scheme file:// do not require any previous processing, their path is sent to {@link FileUploader}
 * to find the source file.
 *
 * URIs with scheme content:// are handling assuming that file is in private storage owned by a different app,
 * and that persistency permission is not granted. Due to this, contents of the file are temporary copied by
 * the OC app, and then passed {@link FileUploader}.
 */
public class UriUploader {

    private FileActivity mActivity;
    private ArrayList<Uri> mUrisToUpload;
    private CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener mCopyTmpTaskListener;

    private int mBehaviour;

    private String mUploadPath;
    private Account mAccount;
    private boolean mShowWaitingDialog;

    private UriUploaderResultCode mCode = UriUploaderResultCode.OK;

    public enum UriUploaderResultCode {
        OK,
        COPY_THEN_UPLOAD,
        ERROR_UNKNOWN,
        ERROR_NO_FILE_TO_UPLOAD,
        ERROR_READ_PERMISSION_NOT_GRANTED
    }

    public UriUploader(
            FileActivity activity,
            ArrayList<Uri> uris,
            String uploadPath,
            Account account,
            int behaviour,
            boolean showWaitingDialog,
            CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener copyTmpTaskListener
    ) {
        mActivity = activity;
        mUrisToUpload = uris;
        mUploadPath = uploadPath;
        mAccount = account;
        mBehaviour = behaviour;
        mShowWaitingDialog = showWaitingDialog;
        mCopyTmpTaskListener = copyTmpTaskListener;
    }

    public UriUploaderResultCode uploadUris() {

        try {

            List<Uri> contentUris = new ArrayList<>();
            List<String> contentRemotePaths = new ArrayList<>();

            int schemeFileCounter = 0;

            for (Parcelable sourceStream : mUrisToUpload) {
                Uri sourceUri = (Uri) sourceStream;
                if (sourceUri != null) {
                    String displayName = UriUtils.getDisplayNameForUri(sourceUri, mActivity);
                    String remotePath = mUploadPath + displayName;

                    if (ContentResolver.SCHEME_CONTENT.equals(sourceUri.getScheme())) {
                        contentUris.add(sourceUri);
                        contentRemotePaths.add(remotePath);

                    } else if (ContentResolver.SCHEME_FILE.equals(sourceUri.getScheme())) {
                        /// file: uris should point to a local file, should be safe let FileUploader handle them
                        requestUpload(sourceUri.getPath(), remotePath);
                        schemeFileCounter++;
                    }
                }
            }

            if (!contentUris.isEmpty()) {
                /// content: uris will be copied to temporary files before calling {@link FileUploader}
                copyThenUpload(contentUris.toArray(new Uri[0]), contentRemotePaths.toArray(new String[0]));

                // Listen to CopyAndUploadContentUrisTask before killing the app or a SecurityException may appear.
                // At least when receiving files to upload.
                mCode = UriUploaderResultCode.COPY_THEN_UPLOAD;

            } else if (schemeFileCounter == 0) {
                mCode = UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD;
            }

        } catch (SecurityException e) {
            mCode = UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED;
            Timber.e(e, "Permissions fail");

        } catch (Exception e) {
            mCode = UriUploaderResultCode.ERROR_UNKNOWN;
            Timber.e(e, "Unexpected error");

        }
        return mCode;
    }

    /**
     * Requests the upload of a file in the local file system to {@link FileUploader} service.
     *
     * The original file will be left in its original location, and will not be duplicated.
     * As a side effect, the user will see the file as not uploaded when accesses to the OC app.
     * This is considered as acceptable, since when a file is shared from another app to OC,
     * the usual workflow will go back to the original app.
     *
     * @param localPath     Absolute path in the local file system to the file to upload.
     * @param remotePath    Absolute path in the current OC account to set to the uploaded file.
     */
    private void requestUpload(String localPath, String remotePath) {
        TransferRequester requester = new TransferRequester();
        requester.uploadNewFile(
                mActivity,
                mAccount,
                localPath,
                remotePath,
                mBehaviour,
                null,       // MIME type will be detected from file name
                false,      // do not create parent folder if not existent
                UploadFileOperation.CREATED_BY_USER
        );
    }

    /**
     *
     * @param sourceUris        Array of content:// URIs to the files to upload
     * @param remotePaths       Array of absolute paths to set to the uploaded files
     */
    private void copyThenUpload(Uri[] sourceUris, String[] remotePaths) {
        if (mShowWaitingDialog) {
            mActivity.showLoadingDialog(R.string.wait_for_tmp_copy_from_private_storage);
        }

        CopyAndUploadContentUrisTask copyTask = new CopyAndUploadContentUrisTask
                (mCopyTmpTaskListener, mActivity);

        FragmentManager fm = mActivity.getSupportFragmentManager();

        // Init Fragment without UI to retain AsyncTask across configuration changes
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);

        if (taskRetainerFragment != null) {
            taskRetainerFragment.setTask(copyTask);
        }

        copyTask.execute(
                CopyAndUploadContentUrisTask.makeParamsToExecute(
                        mAccount,
                        sourceUris,
                        remotePaths,
                        mBehaviour,
                        mActivity.getContentResolver()
                )
        );
    }
}
