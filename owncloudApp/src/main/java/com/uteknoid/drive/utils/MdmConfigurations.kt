/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
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

package com.uteknoid.drive.utils

import androidx.annotation.StringDef

const val CONFIGURATION_LOCK_DELAY_TIME = "lock_delay_time_configuration"
const val CONFIGURATION_SERVER_URL = "server_url_configuration"
const val CONFIGURATION_SERVER_URL_INPUT_VISIBILITY = "server_url_input_visibility_configuration"
const val CONFIGURATION_ALLOW_SCREENSHOTS = "allow_screenshots_configuration"

@StringDef(
    CONFIGURATION_LOCK_DELAY_TIME,
    CONFIGURATION_SERVER_URL,
    CONFIGURATION_SERVER_URL_INPUT_VISIBILITY,
    CONFIGURATION_ALLOW_SCREENSHOTS
)
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class MDMConfigurations
