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

package com.uteknoid.drive.data

import com.uteknoid.drive.domain.exceptions.AccountException
import com.uteknoid.drive.domain.exceptions.AccountNotFoundException
import com.uteknoid.drive.domain.exceptions.AccountNotNewException
import com.uteknoid.drive.domain.exceptions.AccountNotTheSameException
import com.uteknoid.drive.domain.exceptions.BadOcVersionException
import com.uteknoid.drive.domain.exceptions.CancelledException
import com.uteknoid.drive.domain.exceptions.ConflictException
import com.uteknoid.drive.domain.exceptions.CopyIntoDescendantException
import com.uteknoid.drive.domain.exceptions.DelayedForWifiException
import com.uteknoid.drive.domain.exceptions.FileNotFoundException
import com.uteknoid.drive.domain.exceptions.ForbiddenException
import com.uteknoid.drive.domain.exceptions.IncorrectAddressException
import com.uteknoid.drive.domain.exceptions.InstanceNotConfiguredException
import com.uteknoid.drive.domain.exceptions.InvalidCharacterException
import com.uteknoid.drive.domain.exceptions.InvalidCharacterInNameException
import com.uteknoid.drive.domain.exceptions.InvalidLocalFileNameException
import com.uteknoid.drive.domain.exceptions.InvalidOverwriteException
import com.uteknoid.drive.domain.exceptions.LocalFileNotFoundException
import com.uteknoid.drive.domain.exceptions.LocalStorageFullException
import com.uteknoid.drive.domain.exceptions.LocalStorageNotCopiedException
import com.uteknoid.drive.domain.exceptions.LocalStorageNotMovedException
import com.uteknoid.drive.domain.exceptions.LocalStorageNotRemovedException
import com.uteknoid.drive.domain.exceptions.MoveIntoDescendantException
import com.uteknoid.drive.domain.exceptions.NoConnectionWithServerException
import com.uteknoid.drive.domain.exceptions.NoNetworkConnectionException
import com.uteknoid.drive.domain.exceptions.OAuth2ErrorAccessDeniedException
import com.uteknoid.drive.domain.exceptions.OAuth2ErrorException
import com.uteknoid.drive.domain.exceptions.PartialCopyDoneException
import com.uteknoid.drive.domain.exceptions.PartialMoveDoneException
import com.uteknoid.drive.domain.exceptions.QuotaExceededException
import com.uteknoid.drive.domain.exceptions.RedirectToNonSecureException
import com.uteknoid.drive.domain.exceptions.SSLErrorException
import com.uteknoid.drive.domain.exceptions.ServerConnectionTimeoutException
import com.uteknoid.drive.domain.exceptions.ServerNotReachableException
import com.uteknoid.drive.domain.exceptions.ServerResponseTimeoutException
import com.uteknoid.drive.domain.exceptions.ServiceUnavailableException
import com.uteknoid.drive.domain.exceptions.ShareForbiddenException
import com.uteknoid.drive.domain.exceptions.ShareNotFoundException
import com.uteknoid.drive.domain.exceptions.ShareWrongParameterException
import com.uteknoid.drive.domain.exceptions.SpecificForbiddenException
import com.uteknoid.drive.domain.exceptions.SpecificMethodNotAllowedException
import com.uteknoid.drive.domain.exceptions.SpecificServiceUnavailableException
import com.uteknoid.drive.domain.exceptions.SpecificUnsupportedMediaTypeException
import com.uteknoid.drive.domain.exceptions.SyncConflictException
import com.uteknoid.drive.domain.exceptions.UnauthorizedException
import com.uteknoid.drive.domain.exceptions.UnhandledHttpCodeException
import com.uteknoid.drive.domain.exceptions.UnknownErrorException
import com.uteknoid.drive.domain.exceptions.WrongServerResponseException
import com.uteknoid.drive.lib.common.network.CertificateCombinedException
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult
import java.net.SocketTimeoutException

fun <T> executeRemoteOperation(operation: () -> RemoteOperationResult<T>): T {
    operation.invoke().also {
        return handleRemoteOperationResult(it)
    }
}

private fun <T> handleRemoteOperationResult(
    remoteOperationResult: RemoteOperationResult<T>
): T {
    if (remoteOperationResult.isSuccess) {
        return remoteOperationResult.data
    }

    when (remoteOperationResult.code) {
        RemoteOperationResult.ResultCode.WRONG_CONNECTION -> throw NoConnectionWithServerException()
        RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION -> throw NoNetworkConnectionException()
        RemoteOperationResult.ResultCode.TIMEOUT -> {
            if (remoteOperationResult.exception is SocketTimeoutException) throw ServerResponseTimeoutException()
            else throw ServerConnectionTimeoutException()
        }
        RemoteOperationResult.ResultCode.HOST_NOT_AVAILABLE -> throw ServerNotReachableException()
        RemoteOperationResult.ResultCode.SERVICE_UNAVAILABLE -> throw ServiceUnavailableException()
        RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> throw remoteOperationResult.exception as CertificateCombinedException
        RemoteOperationResult.ResultCode.BAD_OC_VERSION -> throw BadOcVersionException()
        RemoteOperationResult.ResultCode.INCORRECT_ADDRESS -> throw IncorrectAddressException()
        RemoteOperationResult.ResultCode.SSL_ERROR -> throw SSLErrorException()
        RemoteOperationResult.ResultCode.UNAUTHORIZED -> throw UnauthorizedException()
        RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED -> throw InstanceNotConfiguredException()
        RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> throw FileNotFoundException()
        RemoteOperationResult.ResultCode.OAUTH2_ERROR -> throw OAuth2ErrorException()
        RemoteOperationResult.ResultCode.OAUTH2_ERROR_ACCESS_DENIED -> throw OAuth2ErrorAccessDeniedException()
        RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW -> throw AccountNotNewException()
        RemoteOperationResult.ResultCode.ACCOUNT_NOT_THE_SAME -> throw AccountNotTheSameException()
        RemoteOperationResult.ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION -> throw RedirectToNonSecureException()
        RemoteOperationResult.ResultCode.UNHANDLED_HTTP_CODE -> throw UnhandledHttpCodeException()
        RemoteOperationResult.ResultCode.UNKNOWN_ERROR -> throw UnknownErrorException()
        RemoteOperationResult.ResultCode.CANCELLED -> throw CancelledException()
        RemoteOperationResult.ResultCode.INVALID_LOCAL_FILE_NAME -> throw InvalidLocalFileNameException()
        RemoteOperationResult.ResultCode.INVALID_OVERWRITE -> throw InvalidOverwriteException()
        RemoteOperationResult.ResultCode.CONFLICT -> throw ConflictException()
        RemoteOperationResult.ResultCode.SYNC_CONFLICT -> throw SyncConflictException()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_FULL -> throw LocalStorageFullException()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED -> throw LocalStorageNotMovedException()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_COPIED -> throw LocalStorageNotCopiedException()
        RemoteOperationResult.ResultCode.QUOTA_EXCEEDED -> throw QuotaExceededException()
        RemoteOperationResult.ResultCode.ACCOUNT_NOT_FOUND -> throw AccountNotFoundException()
        RemoteOperationResult.ResultCode.ACCOUNT_EXCEPTION -> throw AccountException()
        RemoteOperationResult.ResultCode.INVALID_CHARACTER_IN_NAME -> throw InvalidCharacterInNameException()
        RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_REMOVED -> throw LocalStorageNotRemovedException()
        RemoteOperationResult.ResultCode.FORBIDDEN -> throw ForbiddenException()
        RemoteOperationResult.ResultCode.SPECIFIC_FORBIDDEN -> throw SpecificForbiddenException()
        RemoteOperationResult.ResultCode.INVALID_MOVE_INTO_DESCENDANT -> throw MoveIntoDescendantException()
        RemoteOperationResult.ResultCode.INVALID_COPY_INTO_DESCENDANT -> throw CopyIntoDescendantException()
        RemoteOperationResult.ResultCode.PARTIAL_MOVE_DONE -> throw PartialMoveDoneException()
        RemoteOperationResult.ResultCode.PARTIAL_COPY_DONE -> throw PartialCopyDoneException()
        RemoteOperationResult.ResultCode.SHARE_WRONG_PARAMETER -> throw ShareWrongParameterException()
        RemoteOperationResult.ResultCode.WRONG_SERVER_RESPONSE -> throw WrongServerResponseException()
        RemoteOperationResult.ResultCode.INVALID_CHARACTER_DETECT_IN_SERVER -> throw InvalidCharacterException()
        RemoteOperationResult.ResultCode.DELAYED_FOR_WIFI -> throw DelayedForWifiException()
        RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND -> throw LocalFileNotFoundException()
        RemoteOperationResult.ResultCode.SPECIFIC_SERVICE_UNAVAILABLE -> throw SpecificServiceUnavailableException(remoteOperationResult.httpPhrase)
        RemoteOperationResult.ResultCode.SPECIFIC_UNSUPPORTED_MEDIA_TYPE -> throw SpecificUnsupportedMediaTypeException()
        RemoteOperationResult.ResultCode.SPECIFIC_METHOD_NOT_ALLOWED -> throw SpecificMethodNotAllowedException()
        RemoteOperationResult.ResultCode.SHARE_NOT_FOUND -> throw ShareNotFoundException(remoteOperationResult.httpPhrase)
        RemoteOperationResult.ResultCode.SHARE_FORBIDDEN -> throw ShareForbiddenException(remoteOperationResult.httpPhrase)
        else -> throw Exception()
    }
}
