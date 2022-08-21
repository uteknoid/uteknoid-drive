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

package com.uteknoid.drive.data.file.datasource

import com.uteknoid.drive.data.files.datasources.implementation.OCRemoteFileDataSource
import com.uteknoid.drive.lib.resources.files.services.implementation.OCFileService
import com.uteknoid.drive.lib.common.operations.RemoteOperationResult
import com.uteknoid.drive.testutil.OC_SERVER_INFO
import com.uteknoid.drive.utils.createRemoteOperationResultMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.Exception

class OCRemoteFileDataSourceTest {
    private lateinit var ocRemoteFileDataSource: OCRemoteFileDataSource

    private val ocFileService: OCFileService = mockk()

    @Before
    fun init() {
        ocRemoteFileDataSource = OCRemoteFileDataSource(ocFileService)
    }

    @Test
    fun checkPathExistenceTrue() {
        val checkPathExistenceRemoteResult: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(data = true, isSuccess = true)

        every {
            ocFileService.checkPathExistence(OC_SERVER_INFO.baseUrl, true)
        } returns checkPathExistenceRemoteResult

        val checkPathExistence = ocRemoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, true)

        assertNotNull(checkPathExistence)
        assertEquals(checkPathExistenceRemoteResult.data, checkPathExistence)

        verify { ocFileService.checkPathExistence(OC_SERVER_INFO.baseUrl, true) }
    }

    @Test
    fun checkPathExistenceFalse() {
        val checkPathExistenceRemoteResult: RemoteOperationResult<Boolean> =
            createRemoteOperationResultMock(data = false, isSuccess = true)

        every {
            ocFileService.checkPathExistence(OC_SERVER_INFO.baseUrl, true)
        } returns checkPathExistenceRemoteResult

        val checkPathExistence = ocRemoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, true)

        assertNotNull(checkPathExistence)
        assertEquals(checkPathExistenceRemoteResult.data, checkPathExistence)

        verify { ocFileService.checkPathExistence(OC_SERVER_INFO.baseUrl, true) }
    }

    @Test(expected = Exception::class)
    fun checkPathExistenceException() {
        every {
            ocFileService.checkPathExistence(OC_SERVER_INFO.baseUrl, true)
        } throws Exception()

        ocRemoteFileDataSource.checkPathExistence(OC_SERVER_INFO.baseUrl, true)
    }
}
