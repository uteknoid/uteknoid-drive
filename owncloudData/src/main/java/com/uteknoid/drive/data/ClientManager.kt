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
package com.uteknoid.drive.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import com.uteknoid.drive.data.authentication.SELECTED_ACCOUNT
import com.uteknoid.drive.data.preferences.datasources.SharedPreferencesProvider
import com.uteknoid.drive.lib.common.ConnectionValidator
import com.uteknoid.drive.lib.common.OwnCloudAccount
import com.uteknoid.drive.lib.common.OwnCloudClient
import com.uteknoid.drive.lib.common.SingleSessionManager
import com.uteknoid.drive.lib.common.authentication.OwnCloudCredentials
import com.uteknoid.drive.lib.common.authentication.OwnCloudCredentialsFactory.getAnonymousCredentials
import com.uteknoid.drive.lib.resources.users.services.UserService
import com.uteknoid.drive.lib.resources.users.services.implementation.OCUserService

class ClientManager(
    private val accountManager: AccountManager,
    private val preferencesProvider: SharedPreferencesProvider,
    val context: Context,
    val accountType: String,
    private val connectionValidator: ConnectionValidator
) {
    // This client will maintain cookies across the whole login process.
    private var ownCloudClient: OwnCloudClient? = null

    init {
        SingleSessionManager.setConnectionValidator(connectionValidator)
    }

    /**
     * Returns a client for the login process.
     * Helpful to keep the cookies from the status request to the final login and user info retrieval.
     * For regular uses, use [getClientForAccount]
     */
    fun getClientForAnonymousCredentials(
        path: String,
        requiresNewClient: Boolean,
        ownCloudCredentials: OwnCloudCredentials? = getAnonymousCredentials()
    ): OwnCloudClient {
        val safeClient = ownCloudClient

        return if (requiresNewClient || safeClient == null) {
            OwnCloudClient(Uri.parse(path),
                connectionValidator,
                true,
                SingleSessionManager.getDefaultSingleton(),
                context).apply {
                credentials = ownCloudCredentials
            }.also {
                ownCloudClient = it
            }
        } else {
            safeClient
        }
    }

    private fun getClientForAccount(
        accountName: String?
    ): OwnCloudClient {
        val account: Account? = if (accountName.isNullOrBlank()) {
            getCurrentAccount()
        } else {
            accountManager.getAccountsByType(accountType).firstOrNull { it.name == accountName }
        }
        val ownCloudAccount = OwnCloudAccount(account, context)
        return SingleSessionManager.getDefaultSingleton().getClientFor(ownCloudAccount, context, connectionValidator)
    }

    private fun getCurrentAccount(): Account? {
        val ocAccounts = accountManager.getAccountsByType(accountType)

        val accountName = preferencesProvider.getString(SELECTED_ACCOUNT, null)

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        accountName?.let { selectedAccountName ->
            ocAccounts.firstOrNull { it.name == selectedAccountName }?.let { return it }
        }

        // take first account as fallback
        return ocAccounts.firstOrNull()
    }

    fun getUserService(accountName: String? = ""): UserService {
        val ownCloudClient = getClientForAccount(accountName)
        return OCUserService(client = ownCloudClient)
    }
}
