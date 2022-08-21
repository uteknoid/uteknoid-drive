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

package com.uteknoid.drive.providers

import android.content.Context
import com.uteknoid.drive.BuildConfig
import com.uteknoid.drive.MainApp
import com.uteknoid.drive.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.uteknoid.drive.data.storage.ScopedStorageProvider
import com.uteknoid.drive.lib.common.http.LogInterceptor
import com.uteknoid.drive.lib.common.utils.LoggingHelper
import timber.log.Timber
import java.io.File

class LogsProvider(
    private val context: Context
) {
    private val sharedPreferencesProvider = SharedPreferencesProviderImpl(context)

    fun startLogging() {
        val dataFolder = MainApp.dataFolder
        val localStorageProvider = ScopedStorageProvider(dataFolder, context)

        // Set folder for store logs
        LoggingHelper.startLogging(
            directory = File(localStorageProvider.getLogsPath()),
            storagePath = dataFolder
        )
        Timber.d("${BuildConfig.BUILD_TYPE} start logging ${BuildConfig.VERSION_NAME} ${BuildConfig.COMMIT_SHA1}")

        initHttpLogs()
    }

    fun stopLogging() {
        LoggingHelper.stopLogging()
    }

    fun initHttpLogs() {
        val httpLogsEnabled: Boolean = sharedPreferencesProvider.getBoolean(PREFERENCE_LOG_HTTP, false)
        LogInterceptor.httpLogsEnabled = httpLogsEnabled
    }

    fun shouldLogHttpRequests(logsEnabled: Boolean) {
        sharedPreferencesProvider.putBoolean(PREFERENCE_LOG_HTTP, logsEnabled)
        LogInterceptor.httpLogsEnabled = logsEnabled
    }

    companion object {
        private const val PREFERENCE_LOG_HTTP = "set_httpLogs"
    }
}
