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

import org.scribe.model.OAuthConfig;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuth20ServiceImpl;
import org.scribe.oauth.OAuthService;
import org.scribe.builder.api.DefaultApi20;
import org.scribe.extractors.AccessTokenExtractor;
import org.scribe.extractors.JsonTokenExtractor;

public class ComicAggOAuth2Api extends DefaultApi20 {
	
	private static final String AUTHORIZE_URL = 
			"https://dev.comicagg.com/oauth2/authorize/?client_id=%s&response_type=token&state=%s&scope=%s";
	  
	@Override
	public String getAccessTokenEndpoint() {
		return "https://dev.comicagg.com/oauth2/access_token/";
	}

	@Override
	public String getAuthorizationUrl(OAuthConfig conf) {
		return String.format(AUTHORIZE_URL, conf.getApiKey(), "test", conf.getScope());
	}

	@Override
	public Verb getAccessTokenVerb(){
		return Verb.POST;
	}
	
	@Override
	public AccessTokenExtractor getAccessTokenExtractor() {
		return new JsonTokenExtractor();
	}

	@Override
	public OAuthService createService(OAuthConfig config) {
		return new ComicAggOAuth2Service(this, config);
	}

	private class ComicAggOAuth2Service extends OAuth20ServiceImpl {
		private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
		private static final String GRANT_TYPE = "grant_type";
		private DefaultApi20 api;
		private OAuthConfig config;

		public ComicAggOAuth2Service(DefaultApi20 api, OAuthConfig config) {
			super(api, config);
			this.api = api;
			this.config = config;
		}

		@Override
		public Token getAccessToken(Token requestToken, Verifier verifier) {
			OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(),
					api.getAccessTokenEndpoint());
			switch (api.getAccessTokenVerb()) {
			case POST:
				request.addBodyParameter(OAuthConstants.CLIENT_ID,
						config.getApiKey());
				request.addBodyParameter(OAuthConstants.CLIENT_SECRET,
						config.getApiSecret());
				request.addBodyParameter(OAuthConstants.CODE,
						verifier.getValue());
				request.addBodyParameter(OAuthConstants.REDIRECT_URI,
						config.getCallback());
				request.addBodyParameter(GRANT_TYPE,
						GRANT_TYPE_AUTHORIZATION_CODE);
				break;
			case GET:
			default:
				request.addQuerystringParameter(OAuthConstants.CLIENT_ID,
						config.getApiKey());
				request.addQuerystringParameter(OAuthConstants.CLIENT_SECRET,
						config.getApiSecret());
				request.addQuerystringParameter(OAuthConstants.CODE,
						verifier.getValue());
				request.addQuerystringParameter(OAuthConstants.REDIRECT_URI,
						config.getCallback());
				if (config.hasScope())
					request.addQuerystringParameter(OAuthConstants.SCOPE,
							config.getScope());
			}
			Response response = request.send();
			if (response.getCode() != 200){
				System.err.println("Couldn't get an Access Token from ComicAgg. Error code: "+response.getCode());
				return null;
			}
			return api.getAccessTokenExtractor().extract(response.getBody());
		}

		@Override
		public void signRequest(Token accessToken, OAuthRequest request) {
			if (config.getSignatureType() == SignatureType.Header) {
				request.addHeader("authorization", accessToken.getToken());
			} else {
				request.addQuerystringParameter(OAuthConstants.ACCESS_TOKEN,
						accessToken.getToken());
			}
		}
	}

}