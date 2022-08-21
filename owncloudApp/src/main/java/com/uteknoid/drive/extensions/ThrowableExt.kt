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

package com.uteknoid.drive.extensions

import android.content.res.Resources
import com.uteknoid.drive.R
import com.uteknoid.drive.domain.exceptions.AccountNotNewException
import com.uteknoid.drive.domain.exceptions.AccountNotTheSameException
import com.uteknoid.drive.domain.exceptions.BadOcVersionException
import com.uteknoid.drive.domain.exceptions.FileNotFoundException
import com.uteknoid.drive.domain.exceptions.IncorrectAddressException
import com.uteknoid.drive.domain.exceptions.InstanceNotConfiguredException
import com.uteknoid.drive.domain.exceptions.LocalFileNotFoundException
import com.uteknoid.drive.domain.exceptions.NoConnectionWithServerException
import com.uteknoid.drive.domain.exceptions.NoNetworkConnectionException
import com.uteknoid.drive.domain.exceptions.OAuth2ErrorAccessDeniedException
import com.uteknoid.drive.domain.exceptions.OAuth2ErrorException
import com.uteknoid.drive.domain.exceptions.RedirectToNonSecureException
import com.uteknoid.drive.domain.exceptions.SSLErrorException
import com.uteknoid.drive.domain.exceptions.SSLRecoverablePeerUnverifiedException
import com.uteknoid.drive.domain.exceptions.ServerConnectionTimeoutException
import com.uteknoid.drive.domain.exceptions.ServerNotReachableException
import com.uteknoid.drive.domain.exceptions.ServerResponseTimeoutException
import com.uteknoid.drive.domain.exceptions.ServiceUnavailableException
import com.uteknoid.drive.domain.exceptions.UnauthorizedException
import java.util.Locale

fun Throwable.parseError(
    genericErrorMessage: String,
    resources: Resources,
    showJustReason: Boolean = false
): CharSequence {
    if (!this.message.isNullOrEmpty()) { // If there's an specific error message from layers below use it
        return this.message as String
    } else { // Build the error message otherwise
        val reason = when (this) {
            is NoConnectionWithServerException -> resources.getString(R.string.network_error_socket_exception)
            is NoNetworkConnectionException -> resources.getString(R.string.error_no_network_connection)
            is ServerResponseTimeoutException -> resources.getString(R.string.network_error_socket_timeout_exception)
            is ServerConnectionTimeoutException -> resources.getString(R.string.network_error_connect_timeout_exception)
            is ServerNotReachableException -> resources.getString(R.string.network_host_not_available)
            is ServiceUnavailableException -> resources.getString(R.string.service_unavailable)
            is SSLRecoverablePeerUnverifiedException -> resources.getString(R.string.ssl_certificate_not_trusted)
            is BadOcVersionException -> resources.getString(R.string.auth_bad_oc_version_title)
            is IncorrectAddressException -> resources.getString(R.string.auth_incorrect_address_title)
            is SSLErrorException -> resources.getString(R.string.auth_ssl_general_error_title)
            is UnauthorizedException -> resources.getString(R.string.auth_unauthorized)
            is InstanceNotConfiguredException -> resources.getString(R.string.auth_not_configured_title)
            is FileNotFoundException -> resources.getString(R.string.common_not_found)
            is OAuth2ErrorException -> resources.getString(R.string.auth_oauth_error)
            is OAuth2ErrorAccessDeniedException -> resources.getString(R.string.auth_oauth_error_access_denied)
            is AccountNotNewException -> resources.getString(R.string.auth_account_not_new)
            is AccountNotTheSameException -> resources.getString(R.string.auth_account_not_the_same)
            is RedirectToNonSecureException -> resources.getString(R.string.auth_redirect_non_secure_connection_title)
            is LocalFileNotFoundException -> resources.getString(R.string.local_file_not_found_toast)
            else -> resources.getString(R.string.common_error_unknown)
        }

        return when {
            showJustReason -> {
                reason
            }
            else -> "$genericErrorMessage ${resources.getString(R.string.error_reason)} ${reason.lowercase(Locale.getDefault())}"
        }
    }
}
