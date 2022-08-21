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

import com.uteknoid.drive.data.authentication.repository.OCAuthenticationRepository
import com.uteknoid.drive.data.capabilities.repository.OCCapabilityRepository
import com.uteknoid.drive.data.files.repository.OCFileRepository
import com.uteknoid.drive.data.folderbackup.FolderBackupRepositoryImpl
import com.uteknoid.drive.data.oauth.OAuthRepositoryImpl
import com.uteknoid.drive.data.server.repository.OCServerInfoRepository
import com.uteknoid.drive.data.sharing.sharees.repository.OCShareeRepository
import com.uteknoid.drive.data.sharing.shares.repository.OCShareRepository
import com.uteknoid.drive.data.user.repository.OCUserRepository
import com.uteknoid.drive.domain.authentication.AuthenticationRepository
import com.uteknoid.drive.domain.authentication.oauth.OAuthRepository
import com.uteknoid.drive.domain.camerauploads.FolderBackupRepository
import com.uteknoid.drive.domain.capabilities.CapabilityRepository
import com.uteknoid.drive.domain.files.FileRepository
import com.uteknoid.drive.domain.server.ServerInfoRepository
import com.uteknoid.drive.domain.sharing.sharees.ShareeRepository
import com.uteknoid.drive.domain.sharing.shares.ShareRepository
import com.uteknoid.drive.domain.user.UserRepository
import org.koin.dsl.module

val repositoryModule = module {
    factory<AuthenticationRepository> { OCAuthenticationRepository(get(), get()) }
    factory<CapabilityRepository> { OCCapabilityRepository(get(), get()) }
    factory<FileRepository> { OCFileRepository(get()) }
    factory<ServerInfoRepository> { OCServerInfoRepository(get()) }
    factory<ShareeRepository> { OCShareeRepository(get()) }
    factory<ShareRepository> { OCShareRepository(get(), get()) }
    factory<UserRepository> { OCUserRepository(get(), get()) }
    factory<OAuthRepository> { OAuthRepositoryImpl(get()) }
    factory<FolderBackupRepository> { FolderBackupRepositoryImpl(get()) }
}
