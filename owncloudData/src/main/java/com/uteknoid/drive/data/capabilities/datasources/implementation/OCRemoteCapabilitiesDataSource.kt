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

package com.uteknoid.drive.data.capabilities.datasources.implementation

import com.uteknoid.drive.data.capabilities.datasources.RemoteCapabilitiesDataSource
import com.uteknoid.drive.data.executeRemoteOperation
import com.uteknoid.drive.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.uteknoid.drive.domain.capabilities.model.OCCapability
import com.uteknoid.drive.lib.resources.status.services.CapabilityService

class OCRemoteCapabilitiesDataSource(
    private val capabilityService: CapabilityService,
    private val remoteCapabilityMapper: RemoteCapabilityMapper
) : RemoteCapabilitiesDataSource {

    override fun getCapabilities(
        accountName: String
    ): OCCapability {
        executeRemoteOperation { capabilityService.getCapabilities() }.let { remoteCapability ->
            return remoteCapabilityMapper.toModel(remoteCapability)!!.apply {
                this.accountName = accountName
            }
        }
    }
}
