/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author David Crespo Ríos
 * @author Juan Carlos Garrote Gascón
 * Copyright (C) 2022 ownCloud GmbH.
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
package com.uteknoid.drive

import android.app.Activity
import android.app.Application
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.core.content.pm.PackageInfoCompat
import com.uteknoid.drive.authentication.AccountUtils
import com.uteknoid.drive.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.uteknoid.drive.datamodel.ThumbnailsCacheManager
import com.uteknoid.drive.db.PreferenceManager
import com.uteknoid.drive.dependecyinjection.commonModule
import com.uteknoid.drive.dependecyinjection.localDataSourceModule
import com.uteknoid.drive.dependecyinjection.remoteDataSourceModule
import com.uteknoid.drive.dependecyinjection.repositoryModule
import com.uteknoid.drive.dependecyinjection.useCaseModule
import com.uteknoid.drive.dependecyinjection.viewModelModule
import com.uteknoid.drive.extensions.createNotificationChannel
import com.uteknoid.drive.lib.common.SingleSessionManager
import com.uteknoid.drive.presentation.ui.migration.StorageMigrationActivity
import com.uteknoid.drive.presentation.ui.releasenotes.ReleaseNotesActivity
import com.uteknoid.drive.presentation.ui.security.BiometricActivity
import com.uteknoid.drive.presentation.ui.security.BiometricManager
import com.uteknoid.drive.presentation.ui.security.PatternActivity
import com.uteknoid.drive.presentation.ui.security.PatternManager
import com.uteknoid.drive.presentation.ui.security.passcode.PassCodeActivity
import com.uteknoid.drive.presentation.ui.security.passcode.PassCodeManager
import com.uteknoid.drive.presentation.ui.settings.fragments.SettingsLogsFragment.Companion.PREFERENCE_ENABLE_LOGGING
import com.uteknoid.drive.providers.LogsProvider
import com.uteknoid.drive.providers.MdmProvider
import com.uteknoid.drive.ui.activity.WhatsNewActivity
import com.uteknoid.drive.utils.CONFIGURATION_ALLOW_SCREENSHOTS
import com.uteknoid.drive.utils.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import com.uteknoid.drive.utils.DebugInjector
import com.uteknoid.drive.utils.FILE_SYNC_CONFLICT_CHANNEL_ID
import com.uteknoid.drive.utils.FILE_SYNC_NOTIFICATION_CHANNEL_ID
import com.uteknoid.drive.utils.MEDIA_SERVICE_NOTIFICATION_CHANNEL_ID
import com.uteknoid.drive.utils.UPLOAD_NOTIFICATION_CHANNEL_ID
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import timber.log.Timber

/**
 * Main Application of the project
 *
 *
 * Contains methods to build the "static" strings. These strings were before constants in different
 * classes
 */
class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext

        startLogsIfEnabled()

        DebugInjector.injectDebugTools(appContext)

        createNotificationChannels()

        SingleSessionManager.setUserAgent(userAgent)

        // initialise thumbnails cache on background thread
        ThumbnailsCacheManager.InitDiskCacheTask().execute()

        initDependencyInjection()

        // register global protection with pass code, pattern lock and biometric lock
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                Timber.d("${activity.javaClass.simpleName} onCreate(Bundle) starting")

                // To prevent taking screenshots in MDM
                if (!areScreenshotsAllowed()) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }

                // If there's any lock protection, don't show wizard at this point, show it when lock activities
                // have finished
                if (activity !is PassCodeActivity &&
                    activity !is PatternActivity &&
                    activity !is BiometricActivity
                ) {
                    StorageMigrationActivity.runIfNeeded(activity)
                    if (isFirstRun()) {
                        WhatsNewActivity.runIfNeeded(activity)

                    } else {
                        ReleaseNotesActivity.runIfNeeded(activity)
                    }
                }

                PreferenceManager.migrateFingerprintToBiometricKey(applicationContext)
                PreferenceManager.deleteOldSettingsPreferences(applicationContext)
            }

            override fun onActivityStarted(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onStart() starting")
                PassCodeManager.onActivityStarted(activity)
                PatternManager.onActivityStarted(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    BiometricManager.onActivityStarted(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onResume() starting")
            }

            override fun onActivityPaused(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onPause() ending")
            }

            override fun onActivityStopped(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onStop() ending")
                PassCodeManager.onActivityStopped(activity)
                PatternManager.onActivityStopped(activity)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    BiometricManager.onActivityStopped(activity)
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                Timber.v("${activity.javaClass.simpleName} onSaveInstanceState(Bundle) starting")
            }

            override fun onActivityDestroyed(activity: Activity) {
                Timber.v("${activity.javaClass.simpleName} onDestroy() ending")
            }
        })

    }

    private fun startLogsIfEnabled() {
        val preferenceProvider = SharedPreferencesProviderImpl(applicationContext)

        if (BuildConfig.DEBUG) {
            val alreadySet = preferenceProvider.containsPreference(PREFERENCE_ENABLE_LOGGING)
            if (!alreadySet) {
                preferenceProvider.putBoolean(PREFERENCE_ENABLE_LOGGING, true)
            }
        }

        enabledLogging = preferenceProvider.getBoolean(PREFERENCE_ENABLE_LOGGING, false)

        if (enabledLogging) {
            LogsProvider(applicationContext).startLogging()
        }
    }

    /**
     * Screenshots allowed in debug mode. Devs and tests <3
     * Otherwise, depends on branding.
     */
    private fun areScreenshotsAllowed(): Boolean {
        if (BuildConfig.DEBUG) return true

        val mdmProvider = MdmProvider(applicationContext)
        return mdmProvider.getBrandingBoolean(CONFIGURATION_ALLOW_SCREENSHOTS, R.bool.allow_screenshots)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        createNotificationChannel(
            id = DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.download_notification_channel_name),
            description = getString(R.string.download_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = UPLOAD_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.upload_notification_channel_name),
            description = getString(R.string.upload_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = MEDIA_SERVICE_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.media_service_notification_channel_name),
            description = getString(R.string.media_service_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = FILE_SYNC_CONFLICT_CHANNEL_ID,
            name = getString(R.string.file_sync_notification_channel_name),
            description = getString(R.string.file_sync_notification_channel_description),
            importance = IMPORTANCE_LOW
        )

        createNotificationChannel(
            id = FILE_SYNC_NOTIFICATION_CHANNEL_ID,
            name = getString(R.string.file_sync_notification_channel_name),
            description = getString(R.string.file_sync_notification_channel_description),
            importance = IMPORTANCE_LOW
        )
    }

    private fun isFirstRun(): Boolean {
        if (getLastSeenVersionCode() != 0) {
            return false
        }
        return AccountUtils.getCurrentOwnCloudAccount(appContext) == null
    }

    companion object {
        const val MDM_FLAVOR = "mdm"

        lateinit var appContext: Context
            private set
        var enabledLogging: Boolean = false
            private set

        const val PREFERENCE_KEY_LAST_SEEN_VERSION_CODE = "lastSeenVersionCode"

        /**
         * Next methods give access in code to some constants that need to be defined in string resources to be referred
         * in AndroidManifest.xml file or other xml resource files; or that need to be easy to modify in build time.
         */

        val accountType: String
            get() = appContext.resources.getString(R.string.account_type)

        val versionCode: Int
            get() {
                return try {
                    val pInfo: PackageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                    val longVersionCode: Long = PackageInfoCompat.getLongVersionCode(pInfo)
                    longVersionCode.toInt()
                } catch (e: PackageManager.NameNotFoundException) {
                    0
                }
            }

        val authority: String
            get() = appContext.resources.getString(R.string.authority)

        val authTokenType: String
            get() = appContext.resources.getString(R.string.authority)

        val dataFolder: String
            get() = appContext.resources.getString(R.string.data_folder)

        // user agent
        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        val userAgent: String
            get() {
                val appString = appContext.resources.getString(R.string.user_agent)
                val packageName = appContext.packageName
                var version = ""

                val pInfo: PackageInfo?
                try {
                    pInfo = appContext.packageManager.getPackageInfo(packageName, 0)
                    if (pInfo != null) {
                        version = pInfo.versionName
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.e(e, "Trying to get packageName")
                }

                return String.format(appString, version)
            }

        fun initDependencyInjection() {
            stopKoin()
            startKoin {
                androidContext(appContext)
                modules(
                    listOf(
                        commonModule,
                        viewModelModule,
                        useCaseModule,
                        repositoryModule,
                        localDataSourceModule,
                        remoteDataSourceModule
                    )
                )
            }
        }

        fun getLastSeenVersionCode(): Int {
            val pref = PreferenceManager.getDefaultSharedPreferences(appContext)
            return pref.getInt(PREFERENCE_KEY_LAST_SEEN_VERSION_CODE, 0)
        }
    }
}
