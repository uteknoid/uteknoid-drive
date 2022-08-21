/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * @author David González Verdugo
 * @author Christian Schabesberger
 * @author Shashvat Kedia
 * @author Abel García de Prada
 * Copyright (C) 2012  Bartek Przybylski
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

package com.uteknoid.drive.presentation.ui.authentication

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.uteknoid.drive.BuildConfig
import com.uteknoid.drive.MainApp.Companion.accountType
import com.uteknoid.drive.R
import com.uteknoid.drive.authentication.oauth.OAuthUtils
import com.uteknoid.drive.data.authentication.KEY_USER_ID
import com.uteknoid.drive.data.authentication.OAUTH2_OIDC_SCOPE
import com.uteknoid.drive.databinding.AccountSetupBinding
import com.uteknoid.drive.domain.authentication.oauth.model.ResponseType
import com.uteknoid.drive.domain.authentication.oauth.model.TokenRequest
import com.uteknoid.drive.domain.exceptions.NoNetworkConnectionException
import com.uteknoid.drive.domain.exceptions.OwncloudVersionNotSupportedException
import com.uteknoid.drive.domain.exceptions.ServerNotReachableException
import com.uteknoid.drive.domain.exceptions.StateMismatchException
import com.uteknoid.drive.domain.exceptions.UnauthorizedException
import com.uteknoid.drive.domain.server.model.AuthenticationMethod
import com.uteknoid.drive.domain.server.model.ServerInfo
import com.uteknoid.drive.extensions.checkPasscodeEnforced
import com.uteknoid.drive.extensions.goToUrl
import com.uteknoid.drive.extensions.manageOptionLockSelected
import com.uteknoid.drive.extensions.parseError
import com.uteknoid.drive.extensions.showErrorInToast
import com.uteknoid.drive.extensions.showMessageInSnackbar
import com.uteknoid.drive.interfaces.ISecurityEnforced
import com.uteknoid.drive.interfaces.LockType
import com.uteknoid.drive.lib.common.accounts.AccountTypeUtils
import com.uteknoid.drive.lib.common.accounts.AccountUtils
import com.uteknoid.drive.lib.common.network.CertificateCombinedException
import com.uteknoid.drive.presentation.UIResult
import com.uteknoid.drive.presentation.ui.settings.SettingsActivity
import com.uteknoid.drive.presentation.viewmodels.authentication.OCAuthenticationViewModel
import com.uteknoid.drive.presentation.viewmodels.oauth.OAuthViewModel
import com.uteknoid.drive.providers.ContextProvider
import com.uteknoid.drive.providers.MdmProvider
import com.uteknoid.drive.ui.dialog.SslUntrustedCertDialog
import com.uteknoid.drive.utils.CONFIGURATION_SERVER_URL
import com.uteknoid.drive.utils.CONFIGURATION_SERVER_URL_INPUT_VISIBILITY
import com.uteknoid.drive.utils.DocumentProviderUtils.Companion.notifyDocumentProviderRoots
import com.uteknoid.drive.utils.PreferenceUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class LoginActivity : AppCompatActivity(), SslUntrustedCertDialog.OnSslUntrustedCertListener, ISecurityEnforced {

    private val authenticationViewModel by viewModel<OCAuthenticationViewModel>()
    private val oauthViewModel by viewModel<OAuthViewModel>()
    private val contextProvider by inject<ContextProvider>()
    private val mdmProvider by inject<MdmProvider>()

    private var loginAction: Byte = ACTION_CREATE
    private var authTokenType: String? = null
    private var userAccount: Account? = null
    private lateinit var serverBaseUrl: String

    private var oidcSupported = false

    private lateinit var binding: AccountSetupBinding

    // For handling AbstractAccountAuthenticator responses
    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var resultBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPasscodeEnforced(this)

        // Protection against screen recording
        if (!BuildConfig.DEBUG) {
            window.addFlags(FLAG_SECURE)
        } // else, let it go, or taking screenshots & testing will not be possible

        // Get values from intent
        loginAction = intent.getByteExtra(EXTRA_ACTION, ACTION_CREATE)
        authTokenType = intent.getStringExtra(KEY_AUTH_TOKEN_TYPE)
        userAccount = intent.getParcelableExtra(EXTRA_ACCOUNT)

        // Get values from savedInstanceState
        if (savedInstanceState == null) {
            if (authTokenType == null && userAccount != null) {
                authenticationViewModel.supportsOAuth2((userAccount as Account).name)
            }
        } else {
            authTokenType = savedInstanceState.getString(KEY_AUTH_TOKEN_TYPE)
        }

        // UI initialization
        binding = AccountSetupBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (loginAction != ACTION_CREATE) {
            binding.accountUsername.isEnabled = false
            binding.accountUsername.isFocusable = false
        }

        if (savedInstanceState == null) {
            if (userAccount != null) {
                authenticationViewModel.getBaseUrl((userAccount as Account).name)
            } else {
                serverBaseUrl = getString(R.string.server_url).trim()
            }

            userAccount?.let {
                AccountUtils.getUsernameForAccount(it)?.let { username ->
                    binding.accountUsername.setText(username)
                }
            }
        }

        binding.root.filterTouchesWhenObscured =
            PreferenceUtils.shouldDisallowTouchesWithOtherVisibleWindows(this@LoginActivity)

        initBrandableOptionsUI()

        binding.thumbnail.setOnClickListener { checkOcServer() }

        binding.embeddedCheckServerButton.setOnClickListener { checkOcServer() }

        binding.loginButton.setOnClickListener {
            if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) == authTokenType) { // OAuth
                startOIDCOauthorization()
            } else { // Basic
                authenticationViewModel.loginBasic(
                    binding.accountUsername.text.toString().trim(),
                    binding.accountPassword.text.toString(),
                    if (loginAction != ACTION_CREATE) userAccount?.name else null
                )
            }
        }

        binding.settingsLink.setOnClickListener {
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        initLiveDataObservers()
    }

    private fun initLiveDataObservers() {
        // LiveData observers
        authenticationViewModel.serverInfo.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> getServerInfoIsLoading()
                is UIResult.Success -> getServerInfoIsSuccess(uiResult)
                is UIResult.Error -> getServerInfoIsError(uiResult)
            }
        }

        authenticationViewModel.loginResult.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> loginIsLoading()
                is UIResult.Success -> loginIsSuccess(uiResult)
                is UIResult.Error -> loginIsError(uiResult)
            }
        }

        authenticationViewModel.supportsOAuth2.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> updateAuthTokenTypeAndInstructions(uiResult)
                is UIResult.Error -> showErrorInToast(
                    genericErrorMessageId = R.string.supports_oauth2_error,
                    throwable = uiResult.error
                )
            }
        }

        authenticationViewModel.baseUrl.observe(this) { event ->
            when (val uiResult = event.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> updateBaseUrlAndHostInput(uiResult)
                is UIResult.Error -> showErrorInToast(
                    genericErrorMessageId = R.string.get_base_url_error,
                    throwable = uiResult.error
                )
            }
        }
    }

    private fun checkOcServer() {
        val uri = binding.hostUrlInput.text.toString().trim()
        if (uri.isNotEmpty()) {
            authenticationViewModel.getServerInfo(serverUrl = uri)
        } else {
            binding.serverStatusText.run {
                text = getString(R.string.auth_can_not_auth_against_server).also { Timber.d(it) }
                isVisible = true
            }
        }
    }

    private fun getServerInfoIsSuccess(uiResult: UIResult<ServerInfo>) {
        updateCenteredRefreshButtonVisibility(shouldBeVisible = false)
        uiResult.getStoredData()?.run {
            serverBaseUrl = baseUrl
            binding.hostUrlInput.run {
                setText(baseUrl)
                doAfterTextChanged {
                    //If user modifies url, reset fields and force him to check url again
                    if (authenticationViewModel.serverInfo.value == null || baseUrl != binding.hostUrlInput.text.toString()) {
                        showOrHideBasicAuthFields(shouldBeVisible = false)
                        binding.loginButton.isVisible = false
                        binding.serverStatusText.run {
                            text = ""
                            visibility = INVISIBLE
                        }
                    }
                }
            }

            binding.serverStatusText.run {
                if (isSecureConnection) {
                    text = getString(R.string.auth_secure_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0)
                } else {
                    text = getString(R.string.auth_connection_established)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock_open, 0, 0, 0)
                }
                isVisible = true
            }

            when (authenticationMethod) {
                AuthenticationMethod.BASIC_HTTP_AUTH -> {
                    authTokenType = BASIC_TOKEN_TYPE
                    showOrHideBasicAuthFields(shouldBeVisible = true)
                    binding.accountUsername.doAfterTextChanged { updateLoginButtonVisibility() }
                    binding.accountPassword.doAfterTextChanged { updateLoginButtonVisibility() }
                }

                AuthenticationMethod.BEARER_TOKEN -> {
                    showOrHideBasicAuthFields(shouldBeVisible = false)
                    authTokenType = OAUTH_TOKEN_TYPE
                    binding.loginButton.isVisible = true
                }

                else -> {
                    binding.serverStatusText.run {
                        text = getString(R.string.auth_unsupported_auth_method)
                        setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                        isVisible = true
                    }
                }
            }
        }
    }

    private fun getServerInfoIsLoading() {
        binding.serverStatusText.run {
            text = getString(R.string.auth_testing_connection)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
        }
    }

    private fun getServerInfoIsError(uiResult: UIResult<ServerInfo>) {
        updateCenteredRefreshButtonVisibility(shouldBeVisible = true)
        when (uiResult.getThrowableOrNull()) {
            is CertificateCombinedException ->
                showUntrustedCertDialog(uiResult.getThrowableOrNull() as CertificateCombinedException)
            is OwncloudVersionNotSupportedException -> binding.serverStatusText.run {
                text = getString(R.string.server_not_supported)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
            is NoNetworkConnectionException -> binding.serverStatusText.run {
                text = getString(R.string.error_no_network_connection)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
            }
            else -> binding.serverStatusText.run {
                text = uiResult.getThrowableOrNull()?.parseError("", resources, true)
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            }
        }
        binding.serverStatusText.isVisible = true
        showOrHideBasicAuthFields(shouldBeVisible = false)
    }

    private fun loginIsSuccess(uiResult: UIResult<String>) {
        binding.authStatusText.run {
            isVisible = false
            text = ""
        }

        // Return result to account authenticator, multiaccount does not work without this
        val accountName = uiResult.getStoredData()
        val intent = Intent()
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName)
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, contextProvider.getString(R.string.account_type))
        resultBundle = intent.extras
        setResult(Activity.RESULT_OK, intent)

        notifyDocumentProviderRoots(applicationContext)

        finish()
    }

    private fun loginIsLoading() {
        binding.authStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            isVisible = true
            text = getString(R.string.auth_trying_to_login)
        }
    }

    private fun loginIsError(uiResult: UIResult<String>) {
        when (uiResult.getThrowableOrNull()) {
            is NoNetworkConnectionException, is ServerNotReachableException -> {
                binding.serverStatusText.run {
                    text = getString(R.string.error_no_network_connection)
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.no_network, 0, 0, 0)
                }
                showOrHideBasicAuthFields(shouldBeVisible = false)
            }
            else -> {
                binding.serverStatusText.isVisible = false
                binding.authStatusText.run {
                    text = uiResult.getThrowableOrNull()?.parseError("", resources, true)
                    isVisible = true
                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
                }
            }
        }
    }

    /**
     * OAuth step 1: Get authorization code
     * Firstly, try the OAuth authorization with Open Id Connect, checking whether there's an available .well-known url
     * to use or not
     */
    private fun startOIDCOauthorization() {
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.progress_small, 0, 0, 0)
            text = resources.getString(R.string.oauth_login_connection)
        }

        oauthViewModel.getOIDCServerConfiguration(serverBaseUrl)
        oauthViewModel.oidcDiscovery.observe(this) {
            when (val uiResult = it.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> {
                    Timber.d("Service discovery: ${uiResult.data}")
                    oidcSupported = true
                    uiResult.data?.let { oidcServerConfiguration ->
                        val registrationEndpoint = oidcServerConfiguration.registrationEndpoint
                        if (registrationEndpoint != null) {
                            registerClient(
                                authorizationEndpoint = oidcServerConfiguration.authorizationEndpoint.toUri(),
                                registrationEndpoint = registrationEndpoint
                            )
                        } else {
                            performGetAuthorizationCodeRequest(oidcServerConfiguration.authorizationEndpoint.toUri())
                        }
                    }
                }
                is UIResult.Error -> {
                    Timber.e(uiResult.error, "OIDC failed. Try with normal OAuth")
                    startNormalOauthorization()
                }
            }
        }
    }

    /**
     * Register client if possible.
     */
    private fun registerClient(
        authorizationEndpoint: Uri,
        registrationEndpoint: String
    ) {
        oauthViewModel.registerClient(registrationEndpoint)
        oauthViewModel.registerClient.observe(this) {
            when (val uiResult = it.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> {
                    Timber.d("Client registered: ${it.peekContent().getStoredData()}")
                    uiResult.data?.let { clientRegistrationInfo ->
                        performGetAuthorizationCodeRequest(
                            authorizationEndpoint = authorizationEndpoint,
                            clientId = clientRegistrationInfo.clientId
                        )
                    }
                }
                is UIResult.Error -> {
                    Timber.e(uiResult.error, "Client registration failed.")
                    performGetAuthorizationCodeRequest(authorizationEndpoint)
                }
            }
        }
    }

    /**
     * OAuth step 1: Get authorization code
     * If OIDC is not available, falling back to normal OAuth
     */
    private fun startNormalOauthorization() {
        val oauth2authorizationEndpoint =
            Uri.parse("$serverBaseUrl${File.separator}${getString(R.string.oauth2_url_endpoint_auth)}")
        performGetAuthorizationCodeRequest(oauth2authorizationEndpoint)
    }

    private fun performGetAuthorizationCodeRequest(
        authorizationEndpoint: Uri,
        clientId: String = getString(R.string.oauth2_client_id)
    ) {
        Timber.d("A browser should be opened now to authenticate this user.")

        val customTabsBuilder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        val customTabsIntent: CustomTabsIntent = customTabsBuilder.build()

        val authorizationEndpointUri = OAuthUtils.buildAuthorizationRequest(
            authorizationEndpoint = authorizationEndpoint,
            redirectUri = OAuthUtils.buildRedirectUri(applicationContext).toString(),
            clientId = clientId,
            responseType = ResponseType.CODE.string,
            scope = if (oidcSupported) OAUTH2_OIDC_SCOPE else "",
            codeChallenge = oauthViewModel.codeChallenge,
            state = oauthViewModel.oidcState
        )

        try {
            customTabsIntent.launchUrl(
                this,
                authorizationEndpointUri
            )
        } catch (e: ActivityNotFoundException) {
            binding.serverStatusText.visibility = INVISIBLE
            showMessageInSnackbar(message = this.getString(R.string.file_list_no_app_for_perform_action))
            Timber.e("No Activity found to handle Intent")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            handleGetAuthorizationCodeResponse(it)
        }
    }

    private fun handleGetAuthorizationCodeResponse(intent: Intent) {
        val authorizationCode = intent.data?.getQueryParameter("code")
        val state = intent.data?.getQueryParameter("state")

        if (state != oauthViewModel.oidcState) {
            Timber.e("OAuth request to get authorization code failed. State mismatching, maybe somebody is trying a CSRF attack.")
            updateOAuthStatusIconAndText(StateMismatchException())
        } else {
            if (authorizationCode != null) {
                Timber.d("Authorization code received [$authorizationCode]. Let's exchange it for access token")
                exchangeAuthorizationCodeForTokens(authorizationCode)
            } else {
                val authorizationError = intent.data?.getQueryParameter("error")
                val authorizationErrorDescription = intent.data?.getQueryParameter("error_description")

                Timber.e("OAuth request to get authorization code failed. Error: [$authorizationError]. Error description: [$authorizationErrorDescription]")
                val authorizationException =
                    if (authorizationError == "access_denied") UnauthorizedException() else Throwable()
                updateOAuthStatusIconAndText(authorizationException)
            }
        }
    }

    /**
     * OAuth step 2: Exchange the received authorization code for access and refresh tokens
     */
    private fun exchangeAuthorizationCodeForTokens(authorizationCode: String) {
        binding.serverStatusText.text = getString(R.string.auth_getting_authorization)

        val clientRegistrationInfo = oauthViewModel.registerClient.value?.peekContent()?.getStoredData()

        val clientAuth = if (clientRegistrationInfo?.clientId != null && clientRegistrationInfo.clientSecret != null) {
            OAuthUtils.getClientAuth(clientRegistrationInfo.clientSecret as String, clientRegistrationInfo.clientId)

        } else {
            OAuthUtils.getClientAuth(getString(R.string.oauth2_client_secret), getString(R.string.oauth2_client_id))
        }

        // Use oidc discovery one, or build an oauth endpoint using serverBaseUrl + Setup string.
        val tokenEndPoint = oauthViewModel.oidcDiscovery.value?.peekContent()?.getStoredData()?.tokenEndpoint
            ?: "$serverBaseUrl${File.separator}${contextProvider.getString(R.string.oauth2_url_endpoint_access)}"

        val requestToken = TokenRequest.AccessToken(
            baseUrl = serverBaseUrl,
            tokenEndpoint = tokenEndPoint,
            authorizationCode = authorizationCode,
            redirectUri = OAuthUtils.buildRedirectUri(applicationContext).toString(),
            clientAuth = clientAuth,
            codeVerifier = oauthViewModel.codeVerifier
        )

        oauthViewModel.requestToken(requestToken)

        oauthViewModel.requestToken.observe(this) {
            when (val uiResult = it.peekContent()) {
                is UIResult.Loading -> {}
                is UIResult.Success -> {
                    Timber.d("Tokens received ${uiResult.data}, trying to login, creating account and adding it to account manager")
                    val tokenResponse = uiResult.data ?: return@observe

                    authenticationViewModel.loginOAuth(
                        username = tokenResponse.additionalParameters?.get(KEY_USER_ID).orEmpty(),
                        authTokenType = OAUTH_TOKEN_TYPE,
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken.orEmpty(),
                        scope = if (oidcSupported) OAUTH2_OIDC_SCOPE else tokenResponse.scope,
                        updateAccountWithUsername = if (loginAction != ACTION_CREATE) userAccount?.name else null,
                        clientRegistrationInfo = clientRegistrationInfo
                    )
                }
                is UIResult.Error -> {
                    Timber.e(uiResult.error, "OAuth request to exchange authorization code for tokens failed")
                    updateOAuthStatusIconAndText(uiResult.error)
                }
            }
        }
    }

    private fun updateAuthTokenTypeAndInstructions(uiResult: UIResult<Boolean?>) {
        val supportsOAuth2 = uiResult.getStoredData()
        authTokenType = if (supportsOAuth2 != null && supportsOAuth2) OAUTH_TOKEN_TYPE else BASIC_TOKEN_TYPE

        binding.instructionsMessage.run {
            if (loginAction == ACTION_UPDATE_EXPIRED_TOKEN) {
                text =
                    if (AccountTypeUtils.getAuthTokenTypeAccessToken(accountType) == authTokenType) {
                        getString(R.string.auth_expired_oauth_token_toast)
                    } else {
                        getString(R.string.auth_expired_basic_auth_toast)
                    }
                isVisible = true
            } else {
                isVisible = false
            }
        }
    }

    private fun updateBaseUrlAndHostInput(uiResult: UIResult<String>) {
        uiResult.getStoredData()?.let { serverUrl ->
            serverBaseUrl = serverUrl

            binding.hostUrlInput.run {
                setText(serverBaseUrl)
                isEnabled = false
                isFocusable = false
            }

            if (loginAction != ACTION_CREATE && serverBaseUrl.isNotEmpty()) {
                checkOcServer()
            }
        }
    }

    /**
     * Show untrusted cert dialog
     */
    private fun showUntrustedCertDialog(certificateCombinedException: CertificateCombinedException) { // Show a dialog with the certificate info
        val dialog = SslUntrustedCertDialog.newInstanceForFullSslError(certificateCombinedException)
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)
        dialog.show(ft, UNTRUSTED_CERT_DIALOG_TAG)
    }

    override fun onSavedCertificate() {
        Timber.d("Server certificate is trusted")
        checkOcServer()
    }

    override fun onCancelCertificate() {
        Timber.d("Server certificate is not trusted")
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
            text = getString(R.string.ssl_certificate_not_trusted)
        }
    }

    override fun onFailedSavingCertificate() {
        Timber.d("Server certificate could not be saved")
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_warning, 0, 0, 0)
            text = getString(R.string.ssl_validator_not_saved)
        }
    }

    /* Show or hide Basic Auth fields and reset its values */
    private fun showOrHideBasicAuthFields(shouldBeVisible: Boolean) {
        binding.accountUsernameContainer.run {
            isVisible = shouldBeVisible
            isFocusable = shouldBeVisible
            isEnabled = shouldBeVisible
            if (shouldBeVisible) requestFocus()
        }
        binding.accountPasswordContainer.run {
            isVisible = shouldBeVisible
            isFocusable = shouldBeVisible
            isEnabled = shouldBeVisible
        }

        if (!shouldBeVisible) {
            binding.accountUsername.setText("")
            binding.accountPassword.setText("")
        }

        binding.authStatusText.run {
            isVisible = false
            text = ""
        }
        binding.loginButton.isVisible = false
    }

    private fun updateCenteredRefreshButtonVisibility(shouldBeVisible: Boolean) {
        if (!contextProvider.getBoolean(R.bool.show_server_url_input)) {
            binding.centeredRefreshButton.isVisible = shouldBeVisible
        }
    }

    private fun initBrandableOptionsUI() {
        val showInput = mdmProvider.getBrandingBoolean(mdmKey = CONFIGURATION_SERVER_URL_INPUT_VISIBILITY, booleanKey = R.bool.show_server_url_input)
        binding.hostUrlFrame.isVisible = showInput
        binding.centeredRefreshButton.isVisible = !showInput
        if (!showInput) {
            binding.centeredRefreshButton.setOnClickListener { checkOcServer() }
        }

        val url = mdmProvider.getBrandingString(mdmKey = CONFIGURATION_SERVER_URL, stringKey = R.string.server_url)
        if (url.isNotEmpty()) {
            binding.hostUrlInput.setText(url)
            checkOcServer()
        }

        binding.loginLayout.run {
            if (contextProvider.getBoolean(R.bool.use_login_background_image)) {
                binding.loginBackgroundImage.isVisible = true
            } else {
                setBackgroundColor(resources.getColor(R.color.login_background_color))
            }
        }

        binding.welcomeLink.run {
            if (contextProvider.getBoolean(R.bool.show_welcome_link)) {
                isVisible = true
                text = contextProvider.getString(R.string.login_welcome_text).takeUnless { it.isBlank() }
                    ?: String.format(contextProvider.getString(R.string.auth_register), contextProvider.getString(R.string.app_name))
                setOnClickListener {
                    setResult(Activity.RESULT_CANCELED)
                    goToUrl(url = getString(R.string.welcome_link_url))
                }
            } else isVisible = false
        }
    }

    private fun updateOAuthStatusIconAndText(authorizationException: Throwable?) {
        binding.serverStatusText.run {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.common_error, 0, 0, 0)
            text =
                if (authorizationException is UnauthorizedException) {
                    getString(R.string.auth_oauth_error_access_denied)
                } else {
                    getString(R.string.auth_oauth_error)
                }
        }
    }

    private fun updateLoginButtonVisibility() {
        binding.loginButton.run {
            isVisible = binding.accountUsername.text.toString().isNotBlank() && binding.accountPassword.text.toString().isNotBlank()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_AUTH_TOKEN_TYPE, authTokenType)
    }

    override fun finish() {
        if (accountAuthenticatorResponse != null) { // send the result bundle back if set, otherwise send an error.
            if (resultBundle != null) {
                accountAuthenticatorResponse?.onResult(resultBundle)
            } else {
                accountAuthenticatorResponse?.onError(
                    AccountManager.ERROR_CODE_CANCELED,
                    "canceled"
                )
            }
            accountAuthenticatorResponse = null
        }
        super.finish()
    }

    override fun optionLockSelected(type: LockType) {
        manageOptionLockSelected(type)
    }
}
