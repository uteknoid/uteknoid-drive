/**
 * ownCloud Android client application
 *
 * @author masensio
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
package com.uteknoid.drive.operations;

import android.accounts.AccountManager;

import com.uteknoid.drive.MainApp;
import com.uteknoid.drive.lib.common.OwnCloudClient;
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult;
import com.uteknoid.drive.lib.resources.status.GetRemoteCapabilitiesOperation;
import com.uteknoid.drive.lib.resources.status.GetRemoteStatusOperation;
import com.uteknoid.drive.lib.resources.status.RemoteCapability;
import com.uteknoid.drive.lib.resources.status.OwnCloudVersion;
import com.uteknoid.drive.lib.resources.status.RemoteServerInfo;
import com.uteknoid.drive.operations.common.SyncOperation;
import timber.log.Timber;

/**
 * Get and save capabilities from the server
 */
public class SyncCapabilitiesOperation extends SyncOperation<RemoteCapability> {

    @Override
    protected RemoteOperationResult<RemoteCapability> run(OwnCloudClient client) {
        RemoteCapability capabilities = null;
        OwnCloudVersion serverVersion = null;

        /// Get current value for capabilities from server
        GetRemoteCapabilitiesOperation getCapabilities = new GetRemoteCapabilitiesOperation();
        RemoteOperationResult<RemoteCapability> result = getCapabilities.execute(client);
        if (result.isSuccess()) {
            // Read data from the result
            if (result.getData() != null) {
                capabilities = result.getData();
                serverVersion = new OwnCloudVersion(capabilities.getVersionString());
            }

        } else {
            Timber.w("Remote capabilities not available");

            // server version is important; this fallback will try to get it from status.php
            // if capabilities API is not available.
            GetRemoteStatusOperation getStatus = new GetRemoteStatusOperation();
            RemoteOperationResult<RemoteServerInfo> statusResult = getStatus.execute(client);
            if (statusResult.isSuccess()) {
                serverVersion = statusResult.getData().getOwnCloudVersion();
            }
        }

        /// save data - capabilities in database
        if (capabilities != null) {
            getStorageManager().saveCapabilities(capabilities);
        }

        /// save data - OC version
        // need to save separately version in AccountManager, due to bad dependency in
        // library: com.owncloud.android.lib.common.accounts.AccountUtils#getCredentialsForAccount(...)
        // and com.owncloud.android.lib.common.accounts.AccountUtils#getServerVersionForAccount(...)
        if (serverVersion != null) {
            AccountManager accountMngr = AccountManager.get(MainApp.Companion.getAppContext());
            accountMngr.setUserData(
                    getStorageManager().getAccount(),
                    com.uteknoid.drive.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION,
                    serverVersion.getVersion()
            );
        }

        return result;
    }

}
