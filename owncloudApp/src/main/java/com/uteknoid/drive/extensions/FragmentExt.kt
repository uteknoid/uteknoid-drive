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

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.showErrorInSnackbar(genericErrorMessageId: Int, throwable: Throwable?) =
    throwable?.let {
        showMessageInSnackbar(it.parseError(getString(genericErrorMessageId), resources))
    }

fun Fragment.showMessageInSnackbar(
    message: CharSequence,
    duration: Int = Snackbar.LENGTH_LONG
) {
    val requiredView = view ?: return
    Snackbar.make(requiredView, message, duration).show()
}

fun Fragment.showAlertDialog(
    title: String,
    message: String,
    positiveButtonText: String = getString(android.R.string.ok),
    positiveButtonListener: ((DialogInterface, Int) -> Unit)? = null,
    negativeButtonText: String = "",
    negativeButtonListener: ((DialogInterface, Int) -> Unit)? = null
) {
    val requiredActivity = activity ?: return
    AlertDialog.Builder(requiredActivity)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positiveButtonText, positiveButtonListener)
        .setNegativeButton(negativeButtonText, negativeButtonListener)
        .show()
        .avoidScreenshotsIfNeeded()
}

fun Fragment.hideSoftKeyboard() {
    val focusedView = requireActivity().currentFocus
    focusedView?.let {
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            focusedView.windowToken,
            0
        )
    }
}