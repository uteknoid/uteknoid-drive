/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
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

package com.uteknoid.drive.data.capabilities.db

import android.database.Cursor
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_DAV_CHUNKING_VERSION
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_UNDELETE
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_VERSIONING
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_MULTIPLE
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_RESHARING
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_USER_PROFILE_PICTURE
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_EDITION
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MAYOR
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MICRO
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_MINOR
import com.uteknoid.drive.data.ProviderMeta.ProviderTableMeta.CAPABILITIES_VERSION_STRING
import com.uteknoid.drive.domain.capabilities.model.CapabilityBooleanType.Companion.capabilityBooleanTypeUnknownString

/**
 * Represents one record of the Capabilities table.
 */
@Entity(tableName = CAPABILITIES_TABLE_NAME)
data class OCCapabilityEntity(
    @ColumnInfo(name = CAPABILITIES_ACCOUNT_NAME)
    val accountName: String?,
    @ColumnInfo(name = CAPABILITIES_VERSION_MAYOR)
    val versionMayor: Int,
    @ColumnInfo(name = CAPABILITIES_VERSION_MINOR)
    val versionMinor: Int,
    @ColumnInfo(name = CAPABILITIES_VERSION_MICRO)
    val versionMicro: Int,
    @ColumnInfo(name = CAPABILITIES_VERSION_STRING)
    val versionString: String?,
    @ColumnInfo(name = CAPABILITIES_VERSION_EDITION)
    val versionEdition: String?,
    @ColumnInfo(name = CAPABILITIES_CORE_POLLINTERVAL)
    val corePollInterval: Int,
    @ColumnInfo(name = CAPABILITIES_DAV_CHUNKING_VERSION)
    val davChunkingVersion: String,
    @ColumnInfo(name = CAPABILITIES_SHARING_API_ENABLED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingApiEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_ENABLED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforced: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforcedReadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforcedReadWrite: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicPasswordEnforcedUploadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicExpireDateEnabled: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
    val filesSharingPublicExpireDateDays: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicExpireDateEnforced: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_UPLOAD, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicUpload: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_MULTIPLE, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicMultiple: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingPublicSupportsUploadOnly: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_RESHARING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingResharing: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_FEDERATION_OUTGOING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingFederationOutgoing: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_FEDERATION_INCOMING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingFederationIncoming: Int,
    @ColumnInfo(name = CAPABILITIES_SHARING_USER_PROFILE_PICTURE, defaultValue = capabilityBooleanTypeUnknownString)
    val filesSharingUserProfilePicture: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_BIGFILECHUNKING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesBigFileChunking: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_UNDELETE, defaultValue = capabilityBooleanTypeUnknownString)
    val filesUndelete: Int,
    @ColumnInfo(name = CAPABILITIES_FILES_VERSIONING, defaultValue = capabilityBooleanTypeUnknownString)
    val filesVersioning: Int
) {
    @PrimaryKey(autoGenerate = true) var id: Int = 0

    companion object {
        fun fromCursor(cursor: Cursor): OCCapabilityEntity = cursor.use {
            OCCapabilityEntity(
                it.getString(it.getColumnIndexOrThrow(CAPABILITIES_ACCOUNT_NAME)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_VERSION_MAYOR)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_VERSION_MINOR)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_VERSION_MICRO)),
                it.getString(it.getColumnIndexOrThrow(CAPABILITIES_VERSION_STRING)),
                it.getString(it.getColumnIndexOrThrow(CAPABILITIES_VERSION_EDITION)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_CORE_POLLINTERVAL)),
                it.getString(it.getColumnIndexOrThrow(CAPABILITIES_DAV_CHUNKING_VERSION)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_API_ENABLED)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_ENABLED)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_ONLY)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_READ_WRITE)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED_UPLOAD_ONLY)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_UPLOAD)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_MULTIPLE)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_PUBLIC_SUPPORTS_UPLOAD_ONLY)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_RESHARING)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_FEDERATION_OUTGOING)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_FEDERATION_INCOMING)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_SHARING_USER_PROFILE_PICTURE)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_FILES_BIGFILECHUNKING)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_FILES_UNDELETE)),
                it.getInt(it.getColumnIndexOrThrow(CAPABILITIES_FILES_VERSIONING))
            )
        }
    }
}
