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
package com.uteknoid.drive.presentation.viewmodels

import com.uteknoid.drive.domain.UseCaseResult
import com.uteknoid.drive.domain.user.model.UserQuota
import com.uteknoid.drive.domain.user.usecases.GetStoredQuotaUseCase
import com.uteknoid.drive.domain.utils.Event
import com.uteknoid.drive.presentation.UIResult
import com.uteknoid.drive.presentation.viewmodels.drawer.DrawerViewModel
import com.uteknoid.drive.providers.ContextProvider
import com.uteknoid.drive.testutil.OC_ACCOUNT_NAME
import com.uteknoid.drive.testutil.OC_USER_QUOTA
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@ExperimentalCoroutinesApi
class DrawerViewModelTest : ViewModelTest() {
    private lateinit var drawerViewModel: DrawerViewModel
    private lateinit var getStoredQuotaUseCase: GetStoredQuotaUseCase

    private lateinit var contextProvider: ContextProvider

    private val commonException = Exception()

    @Before
    fun setUp() {
        contextProvider = mockk()

        every { contextProvider.isConnected() } returns true

        Dispatchers.setMain(testCoroutineDispatcher)
        startKoin {
            allowOverride(override = true)
            modules(
                module {
                    factory {
                        contextProvider
                    }
                })
        }

        getStoredQuotaUseCase = mockk()

        testCoroutineDispatcher.pauseDispatcher()

        drawerViewModel = DrawerViewModel(
            getStoredQuotaUseCase,
            coroutinesDispatcherProvider = coroutineDispatcherProvider
        )
    }

    @After
    override fun tearDown() {
        super.tearDown()
        stopKoin()
    }

    @Test
    fun getStoredQuotaOk() {
        every { getStoredQuotaUseCase.execute(any()) } returns UseCaseResult.Success(OC_USER_QUOTA)
        drawerViewModel.getStoredQuota(OC_ACCOUNT_NAME)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<UserQuota>>>(
                Event(UIResult.Loading()), Event(UIResult.Success(OC_USER_QUOTA))
            ),
            liveData = drawerViewModel.userQuota
        )
    }

    @Test
    fun getStoredQuotaException() {
        every { getStoredQuotaUseCase.execute(any()) } returns UseCaseResult.Error(commonException)
        drawerViewModel.getStoredQuota(OC_ACCOUNT_NAME)

        assertEmittedValues(
            expectedValues = listOf<Event<UIResult<UserQuota>>>
                (Event(UIResult.Loading()), Event(UIResult.Error(commonException))),
            liveData = drawerViewModel.userQuota
        )
    }
}
