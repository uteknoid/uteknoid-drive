/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.uteknoid.drive.presentation.manager

import android.accounts.Account
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import com.uteknoid.drive.MainApp.Companion.appContext
import com.uteknoid.drive.R
import com.uteknoid.drive.datamodel.ThumbnailsCacheManager
import com.uteknoid.drive.domain.UseCaseResult
import com.uteknoid.drive.domain.capabilities.usecases.GetStoredCapabilitiesUseCase
import com.uteknoid.drive.domain.exceptions.FileNotFoundException
import com.uteknoid.drive.domain.user.model.UserAvatar
import com.uteknoid.drive.domain.user.usecases.GetUserAvatarAsyncUseCase
import com.uteknoid.drive.ui.DefaultAvatarTextDrawable
import com.uteknoid.drive.utils.BitmapUtils
import org.koin.core.component.KoinComponent
import org.koin.core.error.InstanceCreationException
import org.koin.core.component.inject
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * The avatar is loaded if available in the cache and bound to the received UI element. The avatar is not
 * fetched from the server if not available, unless the parameter 'fetchFromServer' is set to 'true'.
 *
 * If there is no avatar stored and cannot be fetched, a colored icon is generated with the first
 * letter of the account username.
 *
 * If this is not possible either, a predefined user icon is bound instead.
 */
class AvatarManager : KoinComponent {

    fun getAvatarForAccount(
        account: Account,
        fetchIfNotCached: Boolean,
        displayRadius: Float
    ): Drawable? {
        val imageKey = getImageKeyForAccount(account)

        // Check disk cache in background thread
        val avatarBitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(imageKey)
        avatarBitmap?.let {
            Timber.i("Avatar retrieved from cache with imageKey: $imageKey")
            return BitmapUtils.bitmapToCircularBitmapDrawable(appContext.resources, it)
        }

        val shouldFetchAvatar = try {
            val getStoredCapabilitiesUseCase: GetStoredCapabilitiesUseCase by inject()
            val storedCapabilities = getStoredCapabilitiesUseCase.execute(GetStoredCapabilitiesUseCase.Params(account.name))
            storedCapabilities?.isFetchingAvatarAllowed() ?: true
        } catch (instanceCreationException: InstanceCreationException) {
            Timber.e(instanceCreationException, "Koin may not be initialized at this point")
            true
        }

        // Avatar not found in disk cache, fetch from server.
        if (fetchIfNotCached && shouldFetchAvatar) {
            Timber.i("Avatar with imageKey $imageKey is not available in cache. Fetching from server...")
            val getUserAvatarAsyncUseCase: GetUserAvatarAsyncUseCase by inject()
            val useCaseResult =
                getUserAvatarAsyncUseCase.execute(GetUserAvatarAsyncUseCase.Params(accountName = account.name))
            handleAvatarUseCaseResult(account, useCaseResult)?.let { return it }
        }

        // generate placeholder from user name
        try {
            Timber.i("Avatar with imageKey $imageKey is not available in cache. Generating one...")
            return DefaultAvatarTextDrawable.createAvatar(account.name, displayRadius)

        } catch (e: Exception) {
            // nothing to do, return null to apply default icon
            Timber.e(e, "Error calculating RGB value for active account icon.")
        }
        return null
    }

    /**
     * Converts size of file icon from dp to pixel
     *
     * @return int
     */
    private fun getAvatarDimension(): Int = appContext.resources.getDimension(R.dimen.file_avatar_size).roundToInt()

    private fun getImageKeyForAccount(account: Account) = "a_${account.name}"

    /**
     * If [GetUserAvatarAsyncUseCase] is success, add avatar to cache and return a circular drawable.
     * If there is no avatar available in server, remove it from cache.
     */
    fun handleAvatarUseCaseResult(
        account: Account,
        useCaseResult: UseCaseResult<UserAvatar>
    ): Drawable? {
        Timber.d("Fetch avatar use case is success: ${useCaseResult.isSuccess}")
        val imageKey = getImageKeyForAccount(account)

        if (useCaseResult.isSuccess) {
            val userAvatar = useCaseResult.getDataOrNull()
            userAvatar?.let {
                try {
                    var bitmap = BitmapFactory.decodeByteArray(it.avatarData, 0, it.avatarData.size)
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap, getAvatarDimension(), getAvatarDimension())
                    // Add avatar to cache
                    bitmap?.let {
                        ThumbnailsCacheManager.addBitmapToCache(imageKey, bitmap)
                        Timber.d("User avatar saved into cache -> %s", imageKey)
                        return BitmapUtils.bitmapToCircularBitmapDrawable(appContext.resources, bitmap)
                    }
                } catch (t: Throwable) {
                    // the app should never break due to a problem with avatars
                    Timber.e(t, "Generation of avatar for $imageKey failed")
                    if (t is OutOfMemoryError) {
                        System.gc()
                    }
                    null
                }
            }

        } else if (useCaseResult.getThrowableOrNull() is FileNotFoundException) {
            Timber.i("No avatar available, removing cached copy")
            ThumbnailsCacheManager.removeBitmapFromCache(imageKey)
        }
        return null
    }
}
