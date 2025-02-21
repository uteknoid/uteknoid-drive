/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author Christian Schabesberger
 * @author David González Verdugo
 * Copyright (C) 2012 Bartek Przybylski
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

package com.uteknoid.drive.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import com.uteknoid.drive.R;
import com.uteknoid.drive.utils.PreferenceUtils;
import timber.log.Timber;

/**
 *  Extending ExtendedListFragment. This allows dividing list in groups.
 */
public class ExpandableListFragment extends ExtendedListFragment implements OnChildClickListener {

    protected ExpandableListView mList;

    public void setListAdapter(ExpandableListAdapter listAdapter) {
        mList.setAdapter(listAdapter);
        mList.invalidate();
    }

    public ExpandableListView getListView() {
        return mList;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Timber.v("onCreateView");

        View v = inflater.inflate(R.layout.list_fragment_expandable, null);

        // Allow or disallow touches with other visible windows
        v.setFilterTouchesWhenObscured(
                PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(getContext())
        );

        mEmptyListMessage = v.findViewById(R.id.empty_list_view);
        mList = v.findViewById(R.id.list_root);
        mList.setOnChildClickListener(this);

        mList.setDivider(getResources().getDrawable(R.drawable.uploader_list_separator));
        mList.setDividerHeight(1);

        //        if (savedInstanceState != null) {
        //            int referencePosition = savedInstanceState.getInt(KEY_SAVED_LIST_POSITION);
        //            setReferencePosition(referencePosition);
        //        }

        // Pull down refresh
        mRefreshListLayout = v.findViewById(R.id.swipe_refresh_files);
        mRefreshEmptyLayout = v.findViewById(R.id.swipe_refresh_files_emptyView);

        onCreateSwipeToRefresh(mRefreshListLayout);
        onCreateSwipeToRefresh(mRefreshEmptyLayout);

        mList.setEmptyView(mRefreshEmptyLayout);

        return v;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        // to be @overridden
        Timber.w("onChildClick(). This method should be overridden!");
        return false;
    }

}
