/**
 * ownCloud Android client application
 *
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

import com.uteknoid.drive.lib.common.OwnCloudClient;
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult;
import com.uteknoid.drive.lib.resources.files.chunks.CreateRemoteChunkFolderOperation;
import timber.log.Timber;

public class CreateChunksFolderOperation extends CreateFolderOperation {

    /**
     * Constructor
     *
     * @param remotePath         Path in which create the chunks folder in server
     */
    public CreateChunksFolderOperation(String remotePath) {
        super(remotePath, false);
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        CreateRemoteChunkFolderOperation createRemoteChunkFolderOperation = new CreateRemoteChunkFolderOperation(
                mRemotePath,
                mCreateFullPath
        );

        RemoteOperationResult result = createRemoteChunkFolderOperation.execute(client);

        if (result.isSuccess()) {
            Timber.w("Remote chunks folder " + mRemotePath + " was created");
        } else {
            Timber.e("%s hasn't been created", mRemotePath);
        }

        return result;
    }
}