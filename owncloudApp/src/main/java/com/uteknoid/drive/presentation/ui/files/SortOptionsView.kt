/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2020 ownCloud GmbH.
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
package com.uteknoid.drive.presentation.ui.files

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.uteknoid.drive.R
import com.uteknoid.drive.databinding.SortOptionsLayoutBinding
import com.uteknoid.drive.db.PreferenceManager
import com.uteknoid.drive.presentation.ui.files.SortOrder.Companion.fromPreference
import com.uteknoid.drive.presentation.ui.files.SortType.Companion.fromPreference
import com.uteknoid.drive.utils.FileStorageUtils

class SortOptionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    var onSortOptionsListener: SortOptionsListener? = null
    var onCreateFolderListener: CreateFolderListener? = null

    private var _binding: SortOptionsLayoutBinding? = null
    private val binding get() = _binding!!

    // Enable list view by default.
    var viewTypeSelected: ViewType = ViewType.VIEW_TYPE_LIST
        set(viewType) {
            binding.viewTypeSelector.setImageDrawable(ContextCompat.getDrawable(context, viewType.getOppositeViewType().toDrawableRes()))
            field = viewType
        }

    // Enable sort by name by default.
    var sortTypeSelected: SortType = SortType.SORT_TYPE_BY_NAME
        set(sortType) {
            if (field == sortType) {
                // TODO: Should be changed directly, not here.
                sortOrderSelected = sortOrderSelected.getOppositeSortOrder()
            }
            binding.sortTypeTitle.text = context.getText(sortType.toStringRes())
            field = sortType
        }

    // Enable sort ascending by default.
    var sortOrderSelected: SortOrder = SortOrder.SORT_ORDER_ASCENDING
        set(sortOrder) {
            binding.sortTypeIcon.setImageDrawable(ContextCompat.getDrawable(context, sortOrder.toDrawableRes()))
            field = sortOrder
        }

    init {
        _binding = SortOptionsLayoutBinding.inflate(LayoutInflater.from(context), this, true)

        // Select sort type and order according to preference.
        val sortBy = PreferenceManager.getSortOrder(getContext(), FileStorageUtils.FILE_DISPLAY_SORT)
        sortTypeSelected = fromPreference(sortBy)

        val isAscending = PreferenceManager.getSortAscending(getContext(), FileStorageUtils.FILE_DISPLAY_SORT)
        sortOrderSelected = fromPreference(isAscending)

        binding.sortTypeSelector.setOnClickListener {
            onSortOptionsListener?.onSortTypeListener(
                sortTypeSelected,
                sortOrderSelected
            )
        }
        binding.viewTypeSelector.setOnClickListener {
            onSortOptionsListener?.onViewTypeListener(
                viewTypeSelected.getOppositeViewType()
            )
        }
    }

    fun selectAdditionalView(additionalView: AdditionalView) {
        when (additionalView) {
            AdditionalView.CREATE_FOLDER -> {
                binding.viewTypeSelector.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_create_dir))
                binding.viewTypeSelector.setOnClickListener {
                    onCreateFolderListener?.onCreateFolderListener()
                }
            }
            AdditionalView.VIEW_TYPE -> {
                viewTypeSelected = viewTypeSelected
                binding.viewTypeSelector.setOnClickListener {
                    onSortOptionsListener?.onViewTypeListener(
                        viewTypeSelected.getOppositeViewType()
                    )
                }
            }
        }
    }

    interface SortOptionsListener {
        fun onSortTypeListener(sortType: SortType, sortOrder: SortOrder)
        fun onViewTypeListener(viewType: ViewType)
    }

    interface CreateFolderListener {
        fun onCreateFolderListener()
    }

    enum class AdditionalView {
        CREATE_FOLDER, VIEW_TYPE
    }
}
