/**
 * ownCloud Android client application
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


package com.uteknoid.drive.ui.preview;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.uteknoid.drive.MainApp;
import com.uteknoid.drive.datamodel.OCFile;
import com.uteknoid.drive.lib.common.accounts.AccountUtils;
import com.uteknoid.drive.lib.common.authentication.OwnCloudBasicCredentials;
import com.uteknoid.drive.lib.common.authentication.OwnCloudBearerCredentials;
import com.uteknoid.drive.lib.common.authentication.OwnCloudCredentials;
import timber.log.Timber;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Task for prepare video player asynchronously
 */
public class PrepareVideoPlayerAsyncTask extends AsyncTask<Object, Void, MediaSource> {

    private final Context mContext;
    private final WeakReference<OnPrepareVideoPlayerTaskListener> mListener;
    private final OCFile mFile;
    private final Account mAccount;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    public PrepareVideoPlayerAsyncTask(Context context, OnPrepareVideoPlayerTaskListener listener, OCFile file, Account account) {
        mContext = context;
        mListener = new WeakReference<>(listener);
        mFile = file;
        mAccount = account;
    }

    @Override
    protected MediaSource doInBackground(Object... params) {

        MediaSource mediaSource = null;

        Uri uri;

        try {
            // If the file is already downloaded, reproduce it locally, if not, do streaming
            uri = mFile.isDown() ? mFile.getStorageUri() :
                    Uri.parse(AccountUtils.getWebDavUrlForAccount(mContext, mAccount) +
                            Uri.encode(mFile.getRemotePath(), "/"));

            boolean useBandwidthMeter = true;

            DefaultBandwidthMeter bandwidthMeter = useBandwidthMeter ? BANDWIDTH_METER : null;

            HttpDataSource.Factory httpDataSourceFactory =
                    buildHttpDataSourceFactory(bandwidthMeter, mFile, mAccount);

            // Produces DataSource instances through which media data is loaded.
            DataSource.Factory mediaDataSourceFactory = new DefaultDataSourceFactory(mContext,
                    bandwidthMeter, httpDataSourceFactory);

            // This represents the media to be played.
            mediaSource = buildMediaSource(mediaDataSourceFactory, uri);

        } catch (AccountUtils.AccountNotFoundException e) {
            Timber.e(e);
        }

        return mediaSource;
    }

    /**
     * Build the media source needed to play the video
     *
     * @param mediaDataSourceFactory
     * @param uri
     * @return media to be played
     */
    private MediaSource buildMediaSource(DataSource.Factory mediaDataSourceFactory, Uri uri) {
        return new ProgressiveMediaSource.Factory(mediaDataSourceFactory, new DefaultExtractorsFactory()).createMediaSource(MediaItem.fromUri(uri));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param bandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                       DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(
            DefaultBandwidthMeter bandwidthMeter,
            OCFile file,
            Account account) {

        if (file.isDown()) {

            return new DefaultHttpDataSource.Factory();

        } else {

            try {
                OwnCloudCredentials credentials = AccountUtils.getCredentialsForAccount(MainApp.Companion.getAppContext(), account);

                String login = credentials.getUsername();
                String password = credentials.getAuthToken();

                Map<String, String> params = new HashMap<>(1);

                if (credentials instanceof OwnCloudBasicCredentials) { // Basic auth
                    String cred = login + ":" + password;
                    String auth = "Basic " + Base64.encodeToString(cred.getBytes(), Base64.URL_SAFE);
                    params.put("Authorization", auth);
                } else if (credentials instanceof OwnCloudBearerCredentials) { // OAuth
                    String bearerToken = credentials.getAuthToken();
                    String auth = "Bearer " + bearerToken;
                    params.put("Authorization", auth);
                }

                return new CustomHttpDataSourceFactory(MainApp.Companion.getUserAgent(),
                        bandwidthMeter, params);

            } catch (AuthenticatorException | IOException | OperationCanceledException e) {
                Timber.e(e);
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(MediaSource mediaSource) {
        super.onPostExecute(mediaSource);
        if (mediaSource != null) {
            OnPrepareVideoPlayerTaskListener listener = mListener.get();
            if (listener != null) {
                listener.OnPrepareVideoPlayerTaskCallback(mediaSource);
            }
        }
    }

    /*
     * Interface to retrieve data from prepare video player task
     */
    public interface OnPrepareVideoPlayerTaskListener {

        void OnPrepareVideoPlayerTaskCallback(MediaSource mediaSource);
    }
}
