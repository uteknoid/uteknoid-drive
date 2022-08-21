/**
 * ownCloud Android client application
 *
 * @author David González Verdugo
 * @author Juan Carlos Garrote Gascón
 * @author David Crespo Ríos
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

package com.uteknoid.drive.dependecyinjection

import com.uteknoid.drive.MainApp
import com.uteknoid.drive.presentation.ui.security.passcode.PasscodeAction
import com.uteknoid.drive.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.uteknoid.drive.presentation.viewmodels.capabilities.OCCapabilityViewModel
import com.uteknoid.drive.presentation.viewmodels.drawer.DrawerViewModel
import com.uteknoid.drive.presentation.viewmodels.logging.LogListViewModel
import com.uteknoid.drive.presentation.viewmodels.migration.MigrationViewModel
import com.uteknoid.drive.presentation.viewmodels.oauth.OAuthViewModel
import com.uteknoid.drive.presentation.viewmodels.security.BiometricViewModel
import com.uteknoid.drive.presentation.viewmodels.security.PassCodeViewModel
import com.uteknoid.drive.presentation.viewmodels.security.PatternViewModel
import com.uteknoid.drive.presentation.viewmodels.settings.SettingsAdvancedViewModel
import com.uteknoid.drive.presentation.viewmodels.settings.SettingsLogsViewModel
import com.uteknoid.drive.presentation.viewmodels.settings.SettingsMoreViewModel
import com.uteknoid.drive.presentation.viewmodels.settings.SettingsPictureUploadsViewModel
import com.uteknoid.drive.presentation.viewmodels.settings.SettingsSecurityViewModel
import com.uteknoid.drive.presentation.viewmodels.settings.SettingsVideoUploadsViewModel
import com.uteknoid.drive.presentation.viewmodels.settings.SettingsViewModel
import com.uteknoid.drive.presentation.viewmodels.sharing.OCShareViewModel
import com.uteknoid.drive.presentation.viewmodels.releasenotes.ReleaseNotesViewModel
import com.uteknoid.drive.ui.dialog.RemoveAccountDialogViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel { DrawerViewModel(get(), get()) }

    viewModel { (accountName: String) ->
        OCCapabilityViewModel(accountName, get(), get(), get())
    }

    viewModel { (filePath: String, accountName: String) ->
        OCShareViewModel(filePath, accountName, get(), get(), get(), get(), get(), get(), get(), get(), get())
    }

    viewModel { (action: PasscodeAction) ->
        PassCodeViewModel(get(), get(), action)
    }

    viewModel { OCAuthenticationViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { OAuthViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SettingsSecurityViewModel(get(), get()) }
    viewModel { SettingsLogsViewModel(get(), get(), get()) }
    viewModel { SettingsMoreViewModel(get()) }
    viewModel { SettingsPictureUploadsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsVideoUploadsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsAdvancedViewModel(get()) }
    viewModel { RemoveAccountDialogViewModel(get(), get(), get(), get()) }
    viewModel { LogListViewModel(get()) }
    viewModel { MigrationViewModel(MainApp.dataFolder, get(), get(), get(), get(), get(), get()) }
    viewModel { PatternViewModel(get()) }
    viewModel { BiometricViewModel(get(), get()) }
    viewModel { ReleaseNotesViewModel(get(), get()) }
}
