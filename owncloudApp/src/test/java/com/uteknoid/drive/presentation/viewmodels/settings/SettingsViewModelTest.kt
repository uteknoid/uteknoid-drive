/**
 * ownCloud Android client application
 *
 * @author Juan Carlos Garrote Gascón
 *
 * Copyright (C) 2021 ownCloud GmbH.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.uteknoid.drive.presentation.viewmodels.settings

import com.uteknoid.drive.presentation.viewmodels.ViewModelTest
import com.uteknoid.drive.providers.AccountProvider
import com.uteknoid.drive.testutil.OC_ACCOUNT
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest : ViewModelTest() {
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var accountProvider: AccountProvider

    @Before
    fun setUp() {
        accountProvider = mockk()

        settingsViewModel = SettingsViewModel(accountProvider)
    }

    @Test
    fun `is there attached account - ok - true`() {
        every { accountProvider.getCurrentOwnCloudAccount() } returns OC_ACCOUNT

        val attachedAccount = settingsViewModel.isThereAttachedAccount()

        assertTrue(attachedAccount)

        verify(exactly = 1) {
            accountProvider.getCurrentOwnCloudAccount()
        }
    }

    @Test
    fun `is there attached account - ok - false`() {
        every { accountProvider.getCurrentOwnCloudAccount() } returns null

        val attachedAccount = settingsViewModel.isThereAttachedAccount()

        assertFalse(attachedAccount)

        verify(exactly = 1) {
            accountProvider.getCurrentOwnCloudAccount()
        }
    }
}
