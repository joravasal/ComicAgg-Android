package com.joravasal.tools;

/*
* Copyright (C) 2013  Jorge Avalos-Salguero
*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import android.os.AsyncTask;

///////////////////////////////////
///////////////////////////////////
//                               //
//     NOT USED ATM              //
//                               //
///////////////////////////////////
///////////////////////////////////

public class XMLfromAPI extends AsyncTask<String, Void, String> {
	private static OAuthService serv;
	private static Token accT;

	public XMLfromAPI(String client_id, String client_secret, Token access_token) {
		accT = access_token;
		serv = new ServiceBuilder()
		.provider(ComicAggOAuth2Api.class)
		.apiKey(client_id)
		.apiSecret(client_secret)
		.scope("write").callback("comicagg://oauth2")
		.signatureType(SignatureType.Header)
		.build();
	}
	
	@Override
	//Returns empty string if no internet
	protected String doInBackground(String... api) {
		OAuthRequest request = new OAuthRequest(Verb.GET, api[0]);
		serv.signRequest(accT, request);
		Response resp = request.send();
		return resp.getBody();
	}

}
