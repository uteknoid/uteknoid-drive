/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

import com.uteknoid.drive.MainApp
import com.uteknoid.drive.R
import com.uteknoid.drive.authentication.AccountUtils
import com.uteknoid.drive.data.ClientManager
import com.uteknoid.drive.data.authentication.datasources.RemoteAuthenticationDataSource
import com.uteknoid.drive.data.authentication.datasources.implementation.OCRemoteAuthenticationDataSource
import com.uteknoid.drive.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.uteknoid.drive.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.uteknoid.drive.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.uteknoid.drive.data.files.datasources.RemoteFileDataSource
import com.uteknoid.drive.data.files.datasources.implementation.OCRemoteFileDataSource
import com.uteknoid.drive.data.oauth.datasource.RemoteOAuthDataSource
import com.uteknoid.drive.data.oauth.datasource.impl.RemoteOAuthDataSourceImpl
import com.uteknoid.drive.data.server.datasources.RemoteServerInfoDataSource
import com.uteknoid.drive.data.server.datasources.implementation.OCRemoteServerInfoDataSource
import com.uteknoid.drive.data.sharing.sharees.datasources.RemoteShareeDataSource
import com.uteknoid.drive.data.sharing.sharees.datasources.implementation.OCRemoteShareeDataSource
import com.uteknoid.drive.data.sharing.sharees.datasources.mapper.RemoteShareeMapper
import com.uteknoid.drive.data.sharing.shares.datasources.RemoteShareDataSource
import com.uteknoid.drive.data.sharing.shares.datasources.implementation.OCRemoteShareDataSource
import com.uteknoid.drive.data.sharing.shares.datasources.mapper.RemoteShareMapper
import com.uteknoid.drive.data.user.datasources.RemoteUserDataSource
import com.uteknoid.drive.data.user.datasources.implementation.OCRemoteUserDataSource
import com.uteknoid.drive.lib.common.ConnectionValidator
import com.uteknoid.drive.lib.common.OwnCloudAccount
import com.uteknoid.drive.lib.common.SingleSessionManager
import com.uteknoid.drive.lib.resources.files.services.FileService
import com.uteknoid.drive.lib.resources.files.services.implementation.OCFileService
import com.uteknoid.drive.lib.resources.oauth.services.OIDCService
import com.uteknoid.drive.lib.resources.oauth.services.implementation.OCOIDCService
import com.uteknoid.drive.lib.resources.shares.services.ShareService
import com.uteknoid.drive.lib.resources.shares.services.ShareeService
import com.uteknoid.drive.lib.resources.shares.services.implementation.OCShareService
import com.uteknoid.drive.lib.resources.shares.services.implementation.OCShareeService
import com.uteknoid.drive.lib.resources.status.services.CapabilityService
import com.uteknoid.drive.lib.resources.status.services.ServerInfoService
import com.uteknoid.drive.lib.resources.status.services.implementation.OCCapabilityService
import com.uteknoid.drive.lib.resources.status.services.implementation.OCServerInfoService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val remoteDataSourceModule = module {
    single { AccountUtils.getCurrentOwnCloudAccount(androidContext()) }
    single { OwnCloudAccount(get(), androidContext()) }
    single { SingleSessionManager.getDefaultSingleton().getClientFor(get(), androidContext(), get()) }

    single { ConnectionValidator(androidContext(), androidContext().resources.getBoolean(R.bool.clear_cookies_on_validation)) }
    single { ClientManager(get(), get(), androidContext(), MainApp.accountType, get()) }

    single<CapabilityService> { OCCapabilityService(get()) }
    single<FileService> { OCFileService(get()) }
    single<ServerInfoService> { OCServerInfoService() }
    single<OIDCService> { OCOIDCService() }
    single<ShareService> { OCShareService(get()) }
    single<ShareeService> { OCShareeService(get()) }

    factory<RemoteAuthenticationDataSource> { OCRemoteAuthenticationDataSource(get()) }
    factory<RemoteCapabilitiesDataSource> { OCRemoteCapabilitiesDataSource(get(), get()) }
    factory<RemoteFileDataSource> { OCRemoteFileDataSource(get()) }
    factory<RemoteOAuthDataSource> { RemoteOAuthDataSourceImpl(get(), get()) }
    factory<RemoteServerInfoDataSource> { OCRemoteServerInfoDataSource(get(), get()) }
    factory<RemoteShareDataSource> { OCRemoteShareDataSource(get(), get()) }
    factory<RemoteShareeDataSource> { OCRemoteShareeDataSource(get(), get()) }
    factory<RemoteUserDataSource> {
        OCRemoteUserDataSource(get(), androidContext().resources.getDimension(R.dimen.file_avatar_size).toInt())
    }

    factory { RemoteCapabilityMapper() }
    factory { RemoteShareMapper() }
    factory { RemoteShareeMapper() }
}
