/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 *
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.uteknoid.drive.ui.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class FileListOption : Parcelable {
    ALL_FILES, SHARED_BY_LINK, AV_OFFLINE;

    fun isAllFiles() = this == ALL_FILES
    fun isSharedByLink() = this == SHARED_BY_LINK
    fun isAvailableOffline() = this == AV_OFFLINE
}
