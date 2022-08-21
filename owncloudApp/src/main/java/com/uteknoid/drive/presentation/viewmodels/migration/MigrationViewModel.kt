/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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

package com.uteknoid.drive.presentation.viewmodels.migration

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uteknoid.drive.data.preferences.datasources.SharedPreferencesProvider
import com.uteknoid.drive.data.storage.LegacyStorageProvider
import com.uteknoid.drive.data.storage.LocalStorageProvider
import com.uteknoid.drive.datamodel.FileDataStorageManager
import com.uteknoid.drive.datamodel.OCUpload
import com.uteknoid.drive.datamodel.UploadsStorageManager
import com.uteknoid.drive.domain.utils.Event
import com.uteknoid.drive.presentation.ui.migration.StorageMigrationActivity.Companion.PREFERENCE_ALREADY_MIGRATED_TO_SCOPED_STORAGE
import com.uteknoid.drive.providers.AccountProvider
import com.uteknoid.drive.providers.ContextProvider
import com.uteknoid.drive.providers.CoroutinesDispatcherProvider
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * View Model to keep a reference to the capability repository and an up-to-date capability
 */
class MigrationViewModel(
    rootFolder: String,
    private val localStorageProvider: LocalStorageProvider,
    private val preferencesProvider: SharedPreferencesProvider,
    private val uploadsStorageManager: UploadsStorageManager,
    private val contextProvider: ContextProvider,
    private val accountProvider: AccountProvider,
    private val coroutineDispatcherProvider: CoroutinesDispatcherProvider,
) : ViewModel() {

    private val _migrationState = MediatorLiveData<Event<MigrationState>>()
    val migrationState: LiveData<Event<MigrationState>> = _migrationState

    private val legacyStorageDirectoryPath = LegacyStorageProvider(rootFolder).getRootFolderPath()

    init {
        _migrationState.postValue(Event(MigrationState.MigrationIntroState))
    }

    private fun getLegacyStorageSizeInBytes(): Long {
        val legacyStorageDirectory = File(legacyStorageDirectoryPath)
        return localStorageProvider.sizeOfDirectory(legacyStorageDirectory)
    }

    fun moveLegacyStorageToScopedStorage() {
        viewModelScope.launch(coroutineDispatcherProvider.io) {
            localStorageProvider.moveLegacyToScopedStorage()
            updatePendingUploadsPath()
            updateAlreadyDownloadedFilesPath()
            moveToNextState()
        }
    }

    private fun saveAlreadyMigratedPreference() {
        preferencesProvider.putBoolean(key = PREFERENCE_ALREADY_MIGRATED_TO_SCOPED_STORAGE, value = true)
    }

    private fun updatePendingUploadsPath() {
        uploadsStorageManager.clearSuccessfulUploads()
        val storedUploads: Array<OCUpload> = uploadsStorageManager.allStoredUploads
        val uploadsWithUpdatedPath =
            storedUploads.map {
                it.apply { localPath = localPath.replace(legacyStorageDirectoryPath, localStorageProvider.getRootFolderPath()) }
            }
        uploadsWithUpdatedPath.forEach { uploadsStorageManager.updateUpload(it) }
        clearUnrelatedTemporalFiles(uploadsWithUpdatedPath)
    }

    private fun clearUnrelatedTemporalFiles(pendingUploads: List<OCUpload>) {
        val listOfAccounts = accountProvider.getLoggedAccounts()

        listOfAccounts.forEach { account ->
            val temporalFolderForAccount = File(localStorageProvider.getTemporalPath(account.name))

            cleanTemporalRecursively(temporalFolderForAccount) { temporalFile ->
                if (!pendingUploads.map { it.localPath }.contains(temporalFile.absolutePath)) {
                    Timber.d("Found a temporary file that is not needed: $temporalFile, so let's delete it")
                    temporalFile.delete()
                }
            }
        }
    }

    private fun cleanTemporalRecursively(
        temporalFolder: File,
        deleteFileInCaseItIsNotNeeded: (file: File) -> Unit
    ) {
        temporalFolder.listFiles()?.forEach { temporalFile ->
            if (temporalFile.isDirectory) {
                cleanTemporalRecursively(temporalFile, deleteFileInCaseItIsNotNeeded)
            } else {
                deleteFileInCaseItIsNotNeeded(temporalFile)
            }

        }
    }

    private fun updateAlreadyDownloadedFilesPath() {
        val listOfAccounts = accountProvider.getLoggedAccounts()

        if (listOfAccounts.isEmpty()) return

        listOfAccounts.forEach { account ->
            val fileStorageManager = FileDataStorageManager(contextProvider.getContext(), account, contextProvider.getContext().contentResolver)

            fileStorageManager.migrateLegacyToScopedPath(legacyStorageDirectoryPath, localStorageProvider.getRootFolderPath())
        }
    }

    fun moveToNextState() {

        val nextState: MigrationState = when (_migrationState.value?.peekContent()) {
            is MigrationState.MigrationIntroState -> MigrationState.MigrationChoiceState(
                legacyStorageSpaceInBytes = getLegacyStorageSizeInBytes()
            )
            is MigrationState.MigrationChoiceState -> MigrationState.MigrationProgressState
            is MigrationState.MigrationProgressState -> MigrationState.MigrationCompletedState
            is MigrationState.MigrationCompletedState -> MigrationState.MigrationCompletedState
            null -> MigrationState.MigrationIntroState
        }

        if (nextState is MigrationState.MigrationCompletedState) {
            saveAlreadyMigratedPreference()
        }

        _migrationState.postValue(Event(nextState))
    }

    fun isThereEnoughSpaceInDevice(): Boolean {
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return availableBytes > getLegacyStorageSizeInBytes()
    }
}
