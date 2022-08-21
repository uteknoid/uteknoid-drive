/**
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * @author Christian Schabesberger
 * Copyright (C) 2020 ownCloud GmbH.
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package com.uteknoid.drive.ui.activity

import android.content.Intent
import android.view.View
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.uteknoid.drive.R
import com.uteknoid.drive.authentication.AccountUtils
import com.uteknoid.drive.datamodel.FileDataStorageManager
import com.uteknoid.drive.datamodel.OCFile
import com.uteknoid.drive.utils.AvatarUtils

/**
 * Base class providing toolbar registration functionality, see [.setupToolbar].
 */
abstract class ToolbarActivity : BaseActivity() {

    /**
     * Toolbar setup that must be called in implementer's [.onCreate] after [.setContentView] if they
     * want to use the toolbar.
     */
    open fun setupStandardToolbar(
        title: String?,
        displayHomeAsUpEnabled: Boolean,
        homeButtonEnabled: Boolean,
        displayShowTitleEnabled: Boolean
    ) {
        useStandardToolbar(true)

        val standardToolbar = getStandardToolbar()

        title?.let { standardToolbar.title = it }
        setSupportActionBar(standardToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled)
        supportActionBar?.setHomeButtonEnabled(homeButtonEnabled)
        supportActionBar?.setDisplayShowTitleEnabled(displayShowTitleEnabled)
    }

    open fun setupRootToolbar(
        title: String,
        isSearchEnabled: Boolean
    ) {
        useStandardToolbar(false)

        val toolbarTitle = findViewById<TextView>(R.id.root_toolbar_title)
        val searchView = findViewById<SearchView>(R.id.root_toolbar_search_view)
        val avatarView = findViewById<ImageView>(R.id.root_toolbar_avatar)

        toolbarTitle.apply {
            isVisible = true
            text = title
            if (isSearchEnabled) {
                setOnClickListener {
                    toolbarTitle.isVisible = false
                    searchView.isVisible = true
                    searchView.isIconified = false
                }
                toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_search, 0)
            } else {
                toolbarTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        searchView.apply {
            isVisible = false
            setOnCloseListener {
                searchView.visibility = View.GONE
                toolbarTitle.visibility = VISIBLE
                false
            }
        }

        AccountUtils.getCurrentOwnCloudAccount(baseContext) ?: return

        AvatarUtils().loadAvatarForAccount(
            avatarView,
            AccountUtils.getCurrentOwnCloudAccount(baseContext),
            true,
            baseContext.resources.getDimension(R.dimen.toolbar_avatar_radius)
        )
        avatarView.setOnClickListener {
            startActivity(Intent(baseContext, ManageAccountsActivity::class.java))
        }
    }

    private fun useStandardToolbar(isToolbarStandard: Boolean) {
        getRootToolbar().isVisible = !isToolbarStandard
        getStandardToolbar().isVisible = isToolbarStandard
    }

    open fun updateStandardToolbar(
        title: String = getString(R.string.default_display_name_for_root_folder),
        displayHomeAsUpEnabled: Boolean = true,
        homeButtonEnabled: Boolean = true
    ) {

        if (getStandardToolbar().isVisible) {
            supportActionBar?.title = title
            supportActionBar?.setDisplayHomeAsUpEnabled(displayHomeAsUpEnabled)
            supportActionBar?.setHomeButtonEnabled(homeButtonEnabled)
        } else {
            setupStandardToolbar(title, displayHomeAsUpEnabled, displayHomeAsUpEnabled, true)
        }
    }

    private fun getRootToolbar(): ConstraintLayout = findViewById(R.id.root_toolbar)

    private fun getStandardToolbar(): Toolbar = findViewById(R.id.standard_toolbar)

    /**
     * checks if the given file is the root folder.
     *
     * @param file file to be checked if it is the root folder
     * @return `true` if it is `null` or the root folder, else returns `false`
     */
    fun isRoot(file: OCFile?): Boolean {
        return file == null ||
                (file.isFolder && file.parentId == FileDataStorageManager.ROOT_PARENT_ID.toLong())
    }
}
