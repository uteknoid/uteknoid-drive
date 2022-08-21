/**
 * ownCloud Android client application
 *
 * @author Christian Schabesberger
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

package com.uteknoid.drive.utils

import android.content.Context
import android.os.Build
import android.os.StrictMode
import com.facebook.stetho.Stetho

object DebugInjector {
    open fun injectDebugTools(context: Context) {
        Stetho.initializeWithDefaults(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .penaltyLog()
                    .detectNonSdkApiUsage()
                    .detectUnsafeIntentLaunch()
                    .build()
            )
        }
    }
}
