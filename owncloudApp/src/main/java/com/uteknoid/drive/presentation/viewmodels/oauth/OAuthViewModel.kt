/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
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

package com.uteknoid.drive.presentation.viewmodels.oauth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.uteknoid.drive.MainApp
import com.uteknoid.drive.authentication.oauth.OAuthUtils
import com.uteknoid.drive.domain.authentication.oauth.OIDCDiscoveryUseCase
import com.uteknoid.drive.domain.authentication.oauth.RegisterClientUseCase
import com.uteknoid.drive.domain.authentication.oauth.RequestTokenUseCase
import com.uteknoid.drive.domain.authentication.oauth.model.ClientRegistrationInfo
import com.uteknoid.drive.domain.authentication.oauth.model.OIDCServerConfiguration
import com.uteknoid.drive.domain.authentication.oauth.model.TokenRequest
import com.uteknoid.drive.domain.authentication.oauth.model.TokenResponse
import com.uteknoid.drive.domain.utils.Event
import com.uteknoid.drive.extensions.ViewModelExt.runUseCaseWithResult
import com.uteknoid.drive.presentation.UIResult
import com.uteknoid.drive.providers.CoroutinesDispatcherProvider

class OAuthViewModel(
    private val getOIDCDiscoveryUseCase: OIDCDiscoveryUseCase,
    private val requestTokenUseCase: RequestTokenUseCase,
    private val registerClientUseCase: RegisterClientUseCase,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    val codeVerifier: String = OAuthUtils().generateRandomCodeVerifier()
    val codeChallenge: String = OAuthUtils().generateCodeChallenge(codeVerifier)
    val oidcState: String = OAuthUtils().generateRandomState()

    private val _oidcDiscovery = MediatorLiveData<Event<UIResult<OIDCServerConfiguration>>>()
    val oidcDiscovery: LiveData<Event<UIResult<OIDCServerConfiguration>>> = _oidcDiscovery

    fun getOIDCServerConfiguration(
        serverUrl: String
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = false,
        liveData = _oidcDiscovery,
        useCase = getOIDCDiscoveryUseCase,
        useCaseParams = OIDCDiscoveryUseCase.Params(baseUrl = serverUrl)
    )

    private val _registerClient = MediatorLiveData<Event<UIResult<ClientRegistrationInfo>>>()
    val registerClient: LiveData<Event<UIResult<ClientRegistrationInfo>>> = _registerClient

    fun registerClient(
        registrationEndpoint: String
    ) {
        val registrationRequest = OAuthUtils.buildClientRegistrationRequest(
            registrationEndpoint = registrationEndpoint,
            MainApp.appContext
        )

        runUseCaseWithResult(
            coroutineDispatcher = coroutinesDispatcherProvider.io,
            showLoading = false,
            liveData = _registerClient,
            useCase = registerClientUseCase,
            useCaseParams = RegisterClientUseCase.Params(registrationRequest)
        )
    }

    private val _requestToken = MediatorLiveData<Event<UIResult<TokenResponse>>>()
    val requestToken: LiveData<Event<UIResult<TokenResponse>>> = _requestToken

    fun requestToken(
        tokenRequest: TokenRequest
    ) = runUseCaseWithResult(
        coroutineDispatcher = coroutinesDispatcherProvider.io,
        showLoading = false,
        liveData = _requestToken,
        useCase = requestTokenUseCase,
        useCaseParams = RequestTokenUseCase.Params(tokenRequest = tokenRequest)
    )
}
