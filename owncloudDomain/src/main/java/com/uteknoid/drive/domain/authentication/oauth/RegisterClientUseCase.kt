/**
 * ownCloud Android client application
 *
 * @author Abel García de Prada
 * Copyright (C) 2021 ownCloud GmbH.
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
package com.uteknoid.drive.domain.authentication.oauth

import com.uteknoid.drive.domain.BaseUseCaseWithResult
import com.uteknoid.drive.domain.authentication.oauth.model.ClientRegistrationInfo
import com.uteknoid.drive.domain.authentication.oauth.model.ClientRegistrationRequest

class RegisterClientUseCase(
    private val oAuthRepository: OAuthRepository
) : BaseUseCaseWithResult<ClientRegistrationInfo, RegisterClientUseCase.Params>() {

    override fun run(params: Params): ClientRegistrationInfo =
        oAuthRepository.registerClient(clientRegistrationRequest = params.clientRegistrationRequest)

    data class Params(
        val clientRegistrationRequest: ClientRegistrationRequest
    )
}
