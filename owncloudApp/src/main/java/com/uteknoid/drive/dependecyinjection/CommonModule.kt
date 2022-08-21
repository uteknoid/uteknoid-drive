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

package com.uteknoid.drive.dependecyinjection

import com.uteknoid.drive.datamodel.UploadsStorageManager
import com.uteknoid.drive.presentation.manager.AvatarManager
import com.uteknoid.drive.providers.AccountProvider
import com.uteknoid.drive.providers.ContextProvider
import com.uteknoid.drive.providers.CoroutinesDispatcherProvider
import com.uteknoid.drive.providers.LogsProvider
import com.uteknoid.drive.providers.MdmProvider
import com.uteknoid.drive.providers.OCContextProvider
import com.uteknoid.drive.providers.WorkManagerProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val commonModule = module {

    single { AvatarManager() }
    single { CoroutinesDispatcherProvider() }
    factory<ContextProvider> { OCContextProvider(androidContext()) }
    single { LogsProvider(get()) }
    single { MdmProvider(androidContext()) }
    single { WorkManagerProvider(androidContext()) }
    single { AccountProvider(androidContext()) }
    single { UploadsStorageManager(androidApplication().contentResolver) }
}
