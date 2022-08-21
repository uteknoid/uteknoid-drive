/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.uteknoid.drive.dependecyinjection

import android.accounts.AccountManager
import com.uteknoid.drive.MainApp.Companion.accountType
import com.uteknoid.drive.MainApp.Companion.dataFolder
import com.uteknoid.drive.data.OwncloudDatabase
import com.uteknoid.drive.data.authentication.datasources.LocalAuthenticationDataSource
import com.uteknoid.drive.data.authentication.datasources.implementation.OCLocalAuthenticationDataSource
import com.uteknoid.drive.data.folderbackup.datasources.FolderBackupLocalDataSource
import com.uteknoid.drive.data.folderbackup.datasources.implementation.FolderBackupLocalDataSourceImpl
import com.uteknoid.drive.data.capabilities.datasources.LocalCapabilitiesDataSource
import com.uteknoid.drive.data.capabilities.datasources.implementation.OCLocalCapabilitiesDataSource
import com.uteknoid.drive.data.preferences.datasources.SharedPreferencesProvider
import com.uteknoid.drive.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.uteknoid.drive.data.sharing.shares.datasources.LocalShareDataSource
import com.uteknoid.drive.data.sharing.shares.datasources.implementation.OCLocalShareDataSource
import com.uteknoid.drive.data.storage.LocalStorageProvider
import com.uteknoid.drive.data.storage.ScopedStorageProvider
import com.uteknoid.drive.data.user.datasources.LocalUserDataSource
import com.uteknoid.drive.data.user.datasources.implementation.OCLocalUserDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val localDataSourceModule = module {
    single { AccountManager.get(androidContext()) }

    single { OwncloudDatabase.getDatabase(androidContext()).capabilityDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).shareDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).userDao() }
    single { OwncloudDatabase.getDatabase(androidContext()).folderBackUpDao() }

    single<SharedPreferencesProvider> { SharedPreferencesProviderImpl(get()) }
    single<LocalStorageProvider> { ScopedStorageProvider(dataFolder, androidContext()) }

    factory<LocalAuthenticationDataSource> { OCLocalAuthenticationDataSource(androidContext(), get(), get(), accountType) }
    factory<LocalCapabilitiesDataSource> { OCLocalCapabilitiesDataSource(get()) }
    factory<LocalShareDataSource> { OCLocalShareDataSource(get()) }
    factory<LocalUserDataSource> { OCLocalUserDataSource(get()) }
    factory<FolderBackupLocalDataSource> { FolderBackupLocalDataSourceImpl(get()) }
}
