/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Jesús Recio
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

package com.uteknoid.drive.data.capabilities.datasources

import com.uteknoid.drive.data.capabilities.datasources.implementation.OCRemoteCapabilitiesDataSource
import com.uteknoid.drive.lib.resources.status.services.implementation.OCCapabilityService
import com.uteknoid.drive.data.capabilities.datasources.mapper.RemoteCapabilityMapper
import com.uteknoid.drive.testutil.OC_ACCOUNT_NAME
import com.uteknoid.drive.testutil.OC_CAPABILITY
import com.uteknoid.drive.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class OCRemoteCapabilitiesDataSourceTest {
    private lateinit var ocRemoteCapabilitiesDataSource: OCRemoteCapabilitiesDataSource

    private val ocCapabilityService: OCCapabilityService = mockk()
    private val remoteCapabilityMapper = RemoteCapabilityMapper()

    @Before
    fun init() {
        ocRemoteCapabilitiesDataSource =
            OCRemoteCapabilitiesDataSource(
                ocCapabilityService,
                remoteCapabilityMapper
            )
    }

    @Test
    fun readRemoteCapabilities() {
        val accountName = OC_ACCOUNT_NAME

        val remoteCapability = remoteCapabilityMapper.toRemote(OC_CAPABILITY)!!

        val getRemoteCapabilitiesOperationResult = createRemoteOperationResultMock(remoteCapability, true)

        every { ocCapabilityService.getCapabilities() } returns getRemoteCapabilitiesOperationResult

        // Get capability from remote datasource
        val capabilities = ocRemoteCapabilitiesDataSource.getCapabilities(accountName)

        assertNotNull(capabilities)

        assertEquals(OC_CAPABILITY.accountName, capabilities.accountName)
        assertEquals(OC_CAPABILITY.versionMayor, capabilities.versionMayor)
        assertEquals(OC_CAPABILITY.versionMinor, capabilities.versionMinor)
        assertEquals(OC_CAPABILITY.versionMicro, capabilities.versionMicro)
    }
}
