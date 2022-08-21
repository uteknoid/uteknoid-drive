/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.uteknoid.drive.providers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.uteknoid.drive.workers.CameraUploadsWorker
import com.uteknoid.drive.workers.OldLogsCollectorWorker

class WorkManagerProvider(
    val context: Context
) {
    fun enqueueCameraUploadsWorker() {
        val cameraUploadsWorker = PeriodicWorkRequestBuilder<CameraUploadsWorker>(
            repeatInterval = CameraUploadsWorker.repeatInterval,
            repeatIntervalTimeUnit = CameraUploadsWorker.repeatIntervalTimeUnit
        ).addTag(CameraUploadsWorker.CAMERA_UPLOADS_WORKER)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(CameraUploadsWorker.CAMERA_UPLOADS_WORKER, ExistingPeriodicWorkPolicy.KEEP, cameraUploadsWorker)
    }

    fun enqueueOldLogsCollectorWorker() {
        val constraintsRequired = Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()

        val oldLogsCollectorWorker = PeriodicWorkRequestBuilder<OldLogsCollectorWorker>(
            repeatInterval = OldLogsCollectorWorker.repeatInterval,
            repeatIntervalTimeUnit = OldLogsCollectorWorker.repeatIntervalTimeUnit
        )
            .addTag(OldLogsCollectorWorker.OLD_LOGS_COLLECTOR_WORKER)
            .setConstraints(constraintsRequired)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(OldLogsCollectorWorker.OLD_LOGS_COLLECTOR_WORKER, ExistingPeriodicWorkPolicy.REPLACE, oldLogsCollectorWorker)
    }
}
