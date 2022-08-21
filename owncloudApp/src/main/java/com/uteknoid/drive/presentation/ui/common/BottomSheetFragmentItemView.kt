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
package com.uteknoid.drive.presentation.ui.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.uteknoid.drive.R
import com.uteknoid.drive.databinding.BottomSheetFragmentItemBinding

class BottomSheetFragmentItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    private var _binding: BottomSheetFragmentItemBinding? = null
    private val binding get() = _binding!!

    private var itemIcon: Drawable?
        get() = binding.itemIcon.drawable
        set(value) {
            binding.itemIcon.setImageDrawable(value)
        }

    var title: CharSequence?
        get() = binding.itemTitle.text
        set(value) {
            binding.itemTitle.text = value
        }

    var itemAdditionalIcon: Drawable?
        get() = binding.itemAdditionalIcon.drawable
        set(value) {
            binding.itemAdditionalIcon.setImageDrawable(value)
        }

    init {
        _binding = BottomSheetFragmentItemBinding.inflate(LayoutInflater.from(context), this, true)

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.BottomSheetFragmentItemView, 0, 0)
        try {
            itemIcon = a.getDrawable(R.styleable.BottomSheetFragmentItemView_itemIcon)
            title = a.getString(R.styleable.BottomSheetFragmentItemView_title)
        } finally {
            a.recycle()
        }
    }

    fun setSelected(iconAdditional: Int) {
        itemAdditionalIcon = ContextCompat.getDrawable(context, iconAdditional)
        val selectedColor = ContextCompat.getColor(context, R.color.primary)
        binding.itemIcon.setColorFilter(selectedColor)
        binding.itemTitle.setTextColor(selectedColor)
        binding.itemAdditionalIcon.setColorFilter(selectedColor)
    }
}
