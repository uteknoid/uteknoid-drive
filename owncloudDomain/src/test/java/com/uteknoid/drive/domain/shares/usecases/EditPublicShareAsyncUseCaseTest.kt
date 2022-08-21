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

package com.uteknoid.drive.domain.shares.usecases

import com.uteknoid.drive.domain.exceptions.UnauthorizedException
import com.uteknoid.drive.domain.sharing.shares.ShareRepository
import com.uteknoid.drive.domain.sharing.shares.usecases.EditPublicShareAsyncUseCase
import com.uteknoid.drive.testutil.OC_SHARE
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditPublicShareAsyncUseCaseTest {
    private val shareRepository: ShareRepository = spyk()
    private val useCase = EditPublicShareAsyncUseCase(shareRepository)
    private val useCaseParams = EditPublicShareAsyncUseCase.Params(
        OC_SHARE.remoteId,
        "",
        "",
        OC_SHARE.expirationDate,
        OC_SHARE.permissions,
        false,
        OC_SHARE.accountOwner
    )

    @Test
    fun editPublicShareOk() {
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertFalse(useCaseResult.isError)
        assertEquals(Unit, useCaseResult.getDataOrNull())

        verify(exactly = 1) {
            shareRepository.updatePublicShare(
                remoteId = OC_SHARE.remoteId,
                name = "",
                password = "",
                expirationDateInMillis = OC_SHARE.expirationDate,
                permissions = OC_SHARE.permissions,
                publicUpload = false,
                accountName = OC_SHARE.accountOwner
            )
        }
    }

    @Test
    fun editPublicShareWithUnauthorizedException() {
        every {
            shareRepository.updatePublicShare(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertFalse(useCaseResult.isSuccess)
        assertTrue(useCaseResult.isError)

        assertNull(useCaseResult.getDataOrNull())
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) {
            shareRepository.updatePublicShare(
                remoteId = OC_SHARE.remoteId,
                name = "",
                password = "",
                expirationDateInMillis = OC_SHARE.expirationDate,
                permissions = OC_SHARE.permissions,
                publicUpload = false,
                accountName = OC_SHARE.accountOwner
            )
        }
    }
}
