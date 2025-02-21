/**
 * ownCloud Android client application
 * <p>
 * Copyright (C) 2017 ownCloud GmbH.
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

package com.uteknoid.drive.ui.activity;

import android.accounts.Account;
import android.os.Bundle;
import android.view.View.OnClickListener;

import com.uteknoid.drive.authentication.AccountUtils;
import com.uteknoid.drive.datamodel.OCFile;
import com.uteknoid.drive.ui.fragment.FileFragment;
import com.uteknoid.drive.ui.fragment.OCFileListFragment;

public class UploadPathActivity extends FolderPickerActivity implements FileFragment.ContainerActivity,
        OnClickListener, OnEnforceableRefreshListener {

    public static final String KEY_CAMERA_UPLOAD_PATH = "CAMERA_UPLOAD_PATH";
    public static final String KEY_CAMERA_UPLOAD_ACCOUNT = "CAMERA_UPLOAD_ACCOUNT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String cameraUploadPath = getIntent().getStringExtra(KEY_CAMERA_UPLOAD_PATH);

        // The caller activity (Preferences) is not a FileActivity, so it has no OCFile, only a path.
        OCFile folder = new OCFile(cameraUploadPath);

        setFile(folder);

        // Account may differ from current one. We need to show the picker for this one, not current.
        String accountName = getIntent().getStringExtra(KEY_CAMERA_UPLOAD_ACCOUNT);
        setAccount(AccountUtils.getOwnCloudAccountByName(this, accountName));
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was
     * just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {

            updateFileFromDB();

            OCFile folder = getFile();
            if (folder == null || !folder.isFolder()) {
                // fall back to root folder
                setFile(getStorageManager().getFileByPath(OCFile.ROOT_PATH));
                folder = getFile();
            }

            onBrowsedDownTo(folder);

            if (!stateWasRecovered) {
                OCFileListFragment listOfFolders = getListOfFilesFragment();
                listOfFolders.listDirectory(folder);

                startSyncFolderOperation(folder, false);
            }

            updateNavigationElementsInActionBar();
        }
    }
}
