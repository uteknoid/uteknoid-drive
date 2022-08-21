/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.uteknoid.drive.usecases

import androidx.core.net.toUri
import androidx.work.WorkManager
import com.uteknoid.drive.MainApp
import com.uteknoid.drive.datamodel.UploadsStorageManager
import com.uteknoid.drive.domain.BaseUseCase
import com.uteknoid.drive.domain.camerauploads.model.FolderBackUpConfiguration.Behavior.COPY
import com.uteknoid.drive.domain.camerauploads.model.FolderBackUpConfiguration.Behavior.MOVE
import com.uteknoid.drive.files.services.FileUploader.LOCAL_BEHAVIOUR_MOVE

class RetryUploadFromContentUriUseCase(
    private val workManager: WorkManager
) : BaseUseCase<Unit, RetryUploadFromContentUriUseCase.Params>() {

    override fun run(params: Params) {

        val uploadsStorageManager = UploadsStorageManager(MainApp.appContext.contentResolver)
        val failedUploads = uploadsStorageManager.failedUploads
        val filteredUploads = failedUploads.filter { it.uploadId == params.uploadIdInStorageManager }
        val uploadToRetry = filteredUploads.firstOrNull()

        uploadToRetry ?: return

        UploadFileFromContentUriUseCase(workManager).execute(
            UploadFileFromContentUriUseCase.Params(
                accountName = uploadToRetry.accountName,
                contentUri = uploadToRetry.localPath.toUri(),
                lastModifiedInSeconds = (uploadToRetry.uploadEndTimestamp / 1000).toString(),
                behavior = if (uploadToRetry.localAction == LOCAL_BEHAVIOUR_MOVE) MOVE.name else COPY.name,
                uploadPath = uploadToRetry.remotePath,
                uploadIdInStorageManager = uploadToRetry.uploadId,
                wifiOnly = false,
                chargingOnly = false
            )
        )
        uploadsStorageManager.updateUpload(uploadToRetry.apply { uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS })
    }

    data class Params(
        val uploadIdInStorageManager: Long,
    )
}
