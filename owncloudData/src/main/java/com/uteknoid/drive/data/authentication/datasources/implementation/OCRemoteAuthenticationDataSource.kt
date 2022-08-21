/**
 * ownCloud Android client application
 *
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
package com.uteknoid.drive.data.authentication.datasources.implementation

import com.uteknoid.drive.data.ClientManager
import com.uteknoid.drive.data.authentication.datasources.RemoteAuthenticationDataSource
import com.uteknoid.drive.data.executeRemoteOperation
import com.uteknoid.drive.data.user.datasources.implementation.toDomain
import com.uteknoid.drive.domain.user.model.UserInfo
import com.uteknoid.drive.lib.common.OwnCloudClient
import com.uteknoid.drive.lib.common.OwnCloudClient.WEBDAV_FILES_PATH_4_0
import com.uteknoid.drive.lib.common.authentication.OwnCloudCredentials
import com.uteknoid.drive.lib.common.authentication.OwnCloudCredentialsFactory
import com.uteknoid.drive.lib.resources.files.GetBaseUrlRemoteOperation
import com.uteknoid.drive.lib.resources.users.GetRemoteUserInfoOperation

class OCRemoteAuthenticationDataSource(
    private val clientManager: ClientManager
) : RemoteAuthenticationDataSource {
    override fun loginBasic(serverPath: String, username: String, password: String): Pair<UserInfo, String?> =
        login(OwnCloudCredentialsFactory.newBasicCredentials(username, password), serverPath)

    override fun loginOAuth(serverPath: String, username: String, accessToken: String): Pair<UserInfo, String?> =
        login(OwnCloudCredentialsFactory.newBearerCredentials(username, accessToken), serverPath)

    private fun login(ownCloudCredentials: OwnCloudCredentials, serverPath: String): Pair<UserInfo, String?> {

        val client: OwnCloudClient =
            clientManager.getClientForAnonymousCredentials(
                path = serverPath,
                requiresNewClient = false
            ).apply { credentials = ownCloudCredentials }

        val getBaseUrlRemoteOperation = GetBaseUrlRemoteOperation()
        val rawBaseUrl = executeRemoteOperation { getBaseUrlRemoteOperation.execute(client) }

        val userBaseUri = rawBaseUrl?.replace(WEBDAV_FILES_PATH_4_0, "")
                ?: client.baseUri.toString()

        // Get user info. It is needed to save the account into the account manager
        lateinit var userInfo: UserInfo

        executeRemoteOperation {
            GetRemoteUserInfoOperation().execute(client)
        }.let { userInfo = it.toDomain() }

        return Pair(userInfo, userBaseUri)
    }
}
