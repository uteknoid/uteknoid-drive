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

package com.uteknoid.drive.extensions

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import com.uteknoid.drive.R
import com.uteknoid.drive.data.preferences.datasources.implementation.SharedPreferencesProviderImpl
import com.uteknoid.drive.enums.LockEnforcedType
import com.uteknoid.drive.enums.LockEnforcedType.Companion.parseFromInteger
import com.uteknoid.drive.interfaces.BiometricStatus
import com.uteknoid.drive.interfaces.IEnableBiometrics
import com.uteknoid.drive.interfaces.ISecurityEnforced
import com.uteknoid.drive.interfaces.LockType
import com.uteknoid.drive.lib.common.network.WebdavUtils
import com.uteknoid.drive.presentation.ui.security.BiometricActivity
import com.uteknoid.drive.presentation.ui.security.PatternActivity
import com.uteknoid.drive.presentation.ui.security.passcode.PassCodeActivity
import com.uteknoid.drive.presentation.ui.settings.PrivacyPolicyActivity
import com.uteknoid.drive.presentation.ui.settings.fragments.SettingsSecurityFragment.Companion.EXTRAS_LOCK_ENFORCED
import com.uteknoid.drive.ui.dialog.ShareLinkToDialog
import com.uteknoid.drive.ui.helpers.ShareSheetHelper
import com.uteknoid.drive.utils.MimetypeIconUtil
import timber.log.Timber
import java.io.File

fun Activity.showErrorInSnackbar(genericErrorMessageId: Int, throwable: Throwable?) =
    throwable?.let {
        showMessageInSnackbar(
            message = it.parseError(getString(genericErrorMessageId), resources)
        )
    }

fun Activity.showMessageInSnackbar(
    layoutId: Int = android.R.id.content,
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(findViewById(layoutId), message, duration).show()
}

fun Activity.showErrorInToast(
    genericErrorMessageId: Int,
    throwable: Throwable?,
    duration: Int = Toast.LENGTH_SHORT
) =
    throwable?.let {
        Toast.makeText(
            this,
            it.parseError(getString(genericErrorMessageId), resources),
            duration
        ).show()
    }

fun Activity.goToUrl(
    url: String,
    flags: Int? = null
) {
    if (url.isNotEmpty()) {
        val uriUrl = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uriUrl)
        if (flags != null) intent.addFlags(flags)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showMessageInSnackbar(message = this.getString(R.string.file_list_no_app_for_perform_action))
            Timber.e("No Activity found to handle Intent")
        }
    }
}

fun Activity.openPrivacyPolicy() {
    val urlPrivacyPolicy = getString(R.string.url_privacy_policy)

    val cantBeOpenedWithWebView = urlPrivacyPolicy.endsWith("pdf")
    if (cantBeOpenedWithWebView) {
        goToUrl(urlPrivacyPolicy)
    } else {
        val intent = Intent(this, PrivacyPolicyActivity::class.java)
        startActivity(intent)
    }
}

fun Activity.sendEmail(
    email: String,
    subject: String? = null,
    text: String? = null
) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse(email)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Intent.EXTRA_SUBJECT, subject)
        if (text != null) putExtra(Intent.EXTRA_TEXT, text)
    }

    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        showMessageInSnackbar(message = this.getString(R.string.file_list_no_app_for_perform_action))
        Timber.e("No Activity found to handle Intent")
    }
}

private fun getIntentForSavedMimeType(data: Uri, type: String): Intent {
    val intentForSavedMimeType = Intent(Intent.ACTION_VIEW)
    intentForSavedMimeType.setDataAndType(data, type)
    intentForSavedMimeType.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    return intentForSavedMimeType
}

private fun getIntentForGuessedMimeType(storagePath: String, type: String, data: Uri): Intent? {
    var intentForGuessedMimeType: Intent? = null
    if (storagePath.lastIndexOf('.') >= 0) {
        val guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1))
        if (guessedMimeType != null && guessedMimeType != type) {
            intentForGuessedMimeType = Intent(Intent.ACTION_VIEW)
            intentForGuessedMimeType.setDataAndType(data, guessedMimeType)
            intentForGuessedMimeType.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
    }
    return intentForGuessedMimeType
}

fun Activity.openFile(file: File?) {
    if (file != null) {

        val intentForSavedMimeType = getIntentForSavedMimeType(
            getExposedFileUri(this, file.path)!!,
            MimetypeIconUtil.getBestMimeTypeByFilename(file.name)
        )

        val intentForGuessedMimeType = getIntentForGuessedMimeType(
            file.path,
            MimetypeIconUtil.getBestMimeTypeByFilename(file.name), getExposedFileUri(this, file.path)!!
        )

        openFileWithIntent(intentForSavedMimeType, intentForGuessedMimeType)
    } else {
        Timber.e("Trying to open a NULL file")
    }
}

private fun getExposedFileUri(context: Context, localPath: String): Uri? {
    var exposedFileUri: Uri? = null

    if (localPath.isEmpty()) {
        return null
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        // TODO - use FileProvider with any Android version, with deeper testing -> 2.2.0
        exposedFileUri = Uri.parse(
            ContentResolver.SCHEME_FILE + "://" + WebdavUtils.encodePath(localPath)
        )
    } else {
        // Use the FileProvider to get a content URI
        try {
            exposedFileUri = FileProvider.getUriForFile(
                context,
                context.getString(R.string.file_provider_authority),
                File(localPath)
            )
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "File can't be exported")
        }
    }

    return exposedFileUri
}

fun Activity.openFileWithIntent(intentForSavedMimeType: Intent, intentForGuessedMimeType: Intent?) {
    val openFileWithIntent: Intent = intentForGuessedMimeType ?: intentForSavedMimeType
    val launchables: List<ResolveInfo> =
        this.packageManager.queryIntentActivities(openFileWithIntent, PackageManager.MATCH_DEFAULT_ONLY)
    if (launchables.isNotEmpty()) {
        try {
            this.startActivity(
                Intent.createChooser(
                    openFileWithIntent, this.getString(R.string.actionbar_open_with)
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            showMessageInSnackbar(
                message = this.getString(
                    R.string.file_list_no_app_for_file_type
                )
            )
        }
    } else {
        showMessageInSnackbar(
            message = this.getString(
                R.string.file_list_no_app_for_file_type
            )
        )
    }
}

fun AppCompatActivity.sendFile(file: File?) {
    if (file != null) {
        val sendIntent: Intent = makeIntent(file, this)
        // Show dialog, without the own app
        val packagesToExclude = arrayOf<String>(this.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val shareSheetIntent = ShareSheetHelper().getShareSheetIntent(
                sendIntent,
                this,
                R.string.activity_chooser_send_file_title,
                packagesToExclude
            )
            this.startActivity(shareSheetIntent)
        } else {
            val chooserDialog: DialogFragment = ShareLinkToDialog.newInstance(sendIntent, packagesToExclude)
            chooserDialog.show(this.supportFragmentManager, "CHOOSER_DIALOG")
        }
    } else {
        Timber.e("Trying to send a NULL file")
    }
}

private fun makeIntent(file: File?, context: Context): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND)
    if (file != null) {
        // set MimeType
        sendIntent.type = MimetypeIconUtil.getBestMimeTypeByFilename(file.name)
        sendIntent.putExtra(
            Intent.EXTRA_STREAM,
            getExposedFileUri(context, file.path)
        )
    }
    sendIntent.putExtra(Intent.ACTION_SEND, true) // Send Action
    return sendIntent
}

fun Activity.hideSoftKeyboard() {
    val focusedView = currentFocus
    focusedView?.let {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            focusedView.windowToken,
            0
        )
    }
}

fun Activity.checkPasscodeEnforced(securityEnforced: ISecurityEnforced) {
    val sharedPreferencesProvider = SharedPreferencesProviderImpl(this)

    val lockEnforced: Int = this.resources.getInteger(R.integer.lock_enforced)
    val passcodeConfigured = sharedPreferencesProvider.getBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)
    val patternConfigured = sharedPreferencesProvider.getBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)

    when (parseFromInteger(lockEnforced)) {
        LockEnforcedType.DISABLED -> {}
        LockEnforcedType.EITHER_ENFORCED -> {
            if (!passcodeConfigured && !patternConfigured) {
                val options = arrayOf(getString(R.string.security_enforced_first_option), getString(R.string.security_enforced_second_option))
                var optionSelected = 0

                AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(getString(R.string.security_enforced_title))
                    .setSingleChoiceItems(options, LockType.PASSCODE.ordinal) { _, which -> optionSelected = which }
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        when (LockType.parseFromInteger(optionSelected)) {
                            LockType.PASSCODE -> securityEnforced.optionLockSelected(LockType.PASSCODE)
                            LockType.PATTERN -> securityEnforced.optionLockSelected(LockType.PATTERN)
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        LockEnforcedType.PASSCODE_ENFORCED -> {
            if (!passcodeConfigured) {
                manageOptionLockSelected(LockType.PASSCODE)
            }
        }
        LockEnforcedType.PATTERN_ENFORCED -> {
            if (!patternConfigured) {
                manageOptionLockSelected(LockType.PATTERN)
            }
        }
    }
}

fun Activity.manageOptionLockSelected(type: LockType) {

    SharedPreferencesProviderImpl(this).let {
        // Remove passcode
        it.removePreference(PassCodeActivity.PREFERENCE_PASSCODE)
        it.putBoolean(PassCodeActivity.PREFERENCE_SET_PASSCODE, false)

        // Remove pattern
        it.removePreference(PatternActivity.PREFERENCE_PATTERN)
        it.putBoolean(PatternActivity.PREFERENCE_SET_PATTERN, false)

        // Remove biometric
        it.putBoolean(BiometricActivity.PREFERENCE_SET_BIOMETRIC, false)
    }

    when (type) {
        LockType.PASSCODE -> startActivity(Intent(this, PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_CREATE
            putExtra(EXTRAS_LOCK_ENFORCED, true)
        })
        LockType.PATTERN -> startActivity(Intent(this, PatternActivity::class.java).apply {
            action = PatternActivity.ACTION_REQUEST_WITH_RESULT
            putExtra(EXTRAS_LOCK_ENFORCED, true)
        })
    }
}

fun Activity.showBiometricDialog(iEnableBiometrics: IEnableBiometrics) {
    AlertDialog.Builder(this)
        .setCancelable(false)
        .setTitle(getString(R.string.biometric_dialog_title))
        .setPositiveButton(R.string.common_yes) { dialog, _ ->
            iEnableBiometrics.onOptionSelected(BiometricStatus.ENABLED_BY_USER)
            dialog.dismiss()
        }
        .setNegativeButton(R.string.common_no) { dialog, _ ->
            iEnableBiometrics.onOptionSelected(BiometricStatus.DISABLED_BY_USER)
            dialog.dismiss()
        }
        .show()
}



