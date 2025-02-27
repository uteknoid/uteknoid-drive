/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud GmbH.
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

package com.uteknoid.drive.ui.dialog;

/**
 * Dialog requiring confirmation before removing a collection of given OCFiles.
 * <p>
 * Triggers the removal according to the user response.
 */

import android.app.Dialog;
import android.os.Bundle;

import com.uteknoid.drive.R;
import com.uteknoid.drive.datamodel.OCFile;
import com.uteknoid.drive.extensions.DialogExtKt;
import com.uteknoid.drive.ui.activity.ComponentsGetter;
import com.uteknoid.drive.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;

import java.util.ArrayList;

public class RemoveFilesDialogFragment extends ConfirmationDialogFragment
        implements ConfirmationDialogFragmentListener {

    private ArrayList<OCFile> mTargetFiles;

    private static final String ARG_TARGET_FILES = "TARGET_FILES";

    /**
     * Public factory method to create new RemoveFilesDialogFragment instances.
     *
     * @param files           Files to remove.
     * @return Dialog ready to show.
     */
    public static RemoveFilesDialogFragment newInstance(ArrayList<OCFile> files) {
        RemoveFilesDialogFragment frag = new RemoveFilesDialogFragment();
        Bundle args = new Bundle();
        int messageStringId;

        boolean containsFolder = false;
        boolean containsDown = false;
        boolean containsAvailableOffline = false;
        for (OCFile file : files) {
            if (file.isFolder()) {
                containsFolder = true;
            }
            if (file.isDown()) {
                containsDown = true;
            }
            if (file.getAvailableOfflineStatus() != OCFile.AvailableOfflineStatus.NOT_AVAILABLE_OFFLINE) {
                containsAvailableOffline = true;
            }
        }

        if (files.size() == 1) {
            // choose message for a single file
            OCFile file = files.get(0);

            messageStringId = (file.isFolder()) ?
                    R.string.confirmation_remove_folder_alert :
                    R.string.confirmation_remove_file_alert;

        } else {
            // choose message for more than one file
            messageStringId = (containsFolder) ?
                    R.string.confirmation_remove_folders_alert :
                    R.string.confirmation_remove_files_alert;

        }

        int localRemoveButton = (!containsAvailableOffline && (containsFolder || containsDown)) ?
                R.string.confirmation_remove_local :
                -1;

        args.putInt(ARG_MESSAGE_RESOURCE_ID, messageStringId);
        if (files.size() == 1) {
            args.putStringArray(ARG_MESSAGE_ARGUMENTS, new String[]{files.get(0).getFileName()});
        }
        args.putInt(ARG_POSITIVE_BTN_RES, R.string.common_yes);
        args.putInt(ARG_NEUTRAL_BTN_RES, R.string.common_no);
        args.putInt(ARG_NEGATIVE_BTN_RES, localRemoveButton);
        args.putParcelableArrayList(ARG_TARGET_FILES, files);
        frag.setArguments(args);

        return frag;
    }

    /**
     * Convenience factory method to create new RemoveFilesDialogFragment instances for a single file
     *
     * @param file           File to remove.
     * @return Dialog ready to show.
     */
    public static RemoveFilesDialogFragment newInstance(OCFile file) {
        ArrayList<OCFile> list = new ArrayList<>();
        list.add(file);
        return newInstance(list);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        mTargetFiles = getArguments().getParcelableArrayList(ARG_TARGET_FILES);

        setOnConfirmationListener(this);

        DialogExtKt.avoidScreenshotsIfNeeded(dialog);

        return dialog;
    }

    /**
     * Performs the removal of the target file, both locally and in the server.
     */
    @Override
    public void onConfirmation(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter) getActivity();
        cg.getFileOperationsHelper().removeFiles(mTargetFiles, false);
    }

    /**
     * Performs the removal of the local copy of the target file
     */
    @Override
    public void onCancel(String callerTag) {
        ComponentsGetter cg = (ComponentsGetter) getActivity();
        cg.getFileOperationsHelper().removeFiles(mTargetFiles, true);
    }

    @Override
    public void onNeutral(String callerTag) {
        // nothing to do here
    }
}