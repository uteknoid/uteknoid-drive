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
package com.uteknoid.drive.domain.user.usecases

import com.uteknoid.drive.domain.exceptions.UnauthorizedException
import com.uteknoid.drive.domain.user.UserRepository
import com.uteknoid.drive.testutil.OC_ACCOUNT_NAME
import com.uteknoid.drive.testutil.OC_USER_INFO
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetUserInfoAsyncUseCaseTest {
    private val userRepository: UserRepository = spyk()
    private val useCase = GetUserInfoAsyncUseCase(userRepository)
    private val useCaseParams = GetUserInfoAsyncUseCase.Params(OC_ACCOUNT_NAME)

    @Test
    fun getUserInfoSuccess() {
        every { userRepository.getUserInfo(OC_ACCOUNT_NAME) } returns OC_USER_INFO
        val useCaseResult = useCase.execute(useCaseParams)

        assertTrue(useCaseResult.isSuccess)
        assertFalse(useCaseResult.isError)
        assertEquals(OC_USER_INFO, useCaseResult.getDataOrNull())

        verify(exactly = 1) { userRepository.getUserInfo(OC_ACCOUNT_NAME) }
    }

    @Test
    fun getUserInfoWithUnauthorizedException() {
        every { userRepository.getUserInfo(OC_ACCOUNT_NAME) } throws UnauthorizedException()

        val useCaseResult = useCase.execute(useCaseParams)

        assertFalse(useCaseResult.isSuccess)
        assertTrue(useCaseResult.isError)

        assertNull(useCaseResult.getDataOrNull())
        assertTrue(useCaseResult.getThrowableOrNull() is UnauthorizedException)

        verify(exactly = 1) { userRepository.getUserInfo(OC_ACCOUNT_NAME) }
    }
}
