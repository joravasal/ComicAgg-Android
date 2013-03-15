package com.joravasal.comicagg;

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
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.joravasal.tools.ComicAggOAuth2Api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * <p>
 * LoginActivity is the main activity of ComicAgg app. It is the first to load
 * when the user clicks the app icon and shows a LogIn screen if there was no
 * previous connection or the data from the previous one was deleted.
 * </p>
 * 
 * <p>
 * The activity is in charge of authenticating to the server of <a
 * href="http://www.comicagg.com">ComicAgg</a> using OAuth2. In order to
 * simplify the connection process it uses the library Scribe, of which you can
 * find the code in <a href="https://github.com/fernandezpablo85/scribe-java">
 * GitHub</a> (my thanks to the author Pablo Fernandez). The class sends the
 * accessToken to the next intent so it can sign future requests to the API.
 * </p>
 * 
 */
public class LoginActivity extends Activity {

	final static private String SCOPE = "write";
	final static private String CALLBACK = "comicagg://oauth2";
	final static public String ACCESS_TOKEN = "accToken";
	final static public String ACCESS_SECRET = "accSecret";

	private OAuthService serv = null; // OAuth service to make requests
										// (initialized in OnResume)
	private Token accToken = null;
	private SharedPreferences sharedPref = null;

	private static final String TAG = "LoginActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Create LoginAct");

		setContentView(R.layout.activity_login);
		sharedPref = getPreferences(Context.MODE_PRIVATE);

		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey("DeleteToken")
				&& extras.getBoolean("DeleteToken")) {
			sharedPref.edit().clear().commit();
		}

		accToken = new Token(sharedPref.getString(ACCESS_TOKEN, null),
				sharedPref.getString(ACCESS_SECRET, null));

		if (accToken.getToken() != null) {
			Log.d(TAG, "Token saved, load ComicList intent");
			Intent intent = new Intent(this, ComicListActivity.class);
			intent.putExtra(ACCESS_TOKEN, accToken.getToken());
			intent.putExtra(ACCESS_SECRET, accToken.getSecret());
			startActivity(intent);
		}

	}

	/**
	 * <p>
	 * This method is fired when the only button on the UI is pressed. The
	 * OnClick relation is within the layout of this activity
	 * (activity_login.xml).
	 * </p>
	 * <p>
	 * The method prepares the intent to get the user authorization from the
	 * server in ComicAgg. At this time, it ask for a browser to be opened, some
	 * time in the future it may open its own WebView.
	 * </p>
	 * 
	 * @param v
	 *            The view that fired this function. In this case, it is always
	 *            the same button, so we skip the check up.
	 * 
	 */
	public void login(View v) {
		Log.d(TAG, "Login button pressed");
		if (!isOnline()) {
			Toast.makeText(getApplicationContext(), R.string.toast_not_online,
					Toast.LENGTH_LONG).show();
			Log.e(TAG, "No internet, cancel login");
			return;
		}
		// The oauth service "serv" is created in OnResume
		// Obtain the URL to authenticate the user
		String authUrl = serv.getAuthorizationUrl(null);

		Log.d(TAG, "Load web intent for authorizing");
		Intent webintent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
		startActivity(webintent);
		// TODO: Crear una ventana por encima de la aplicacion sin salir ni
		// crear un nuevo intent
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "On resume");

		serv = new ServiceBuilder().provider(ComicAggOAuth2Api.class)
				.apiKey(getString(R.string.client_id))
				.apiSecret(getString(R.string.client_secret)).scope(SCOPE)
				.callback(CALLBACK).build();

		Uri uri = getIntent().getData();
		if (uri != null && uri.toString().startsWith(CALLBACK)) {
			// The app was called back from other app (a browser for instance)
			Log.d(TAG, "There is a callback");
			if (uri.getQueryParameter("error") != null
					&& !uri.getQueryParameter("error").isEmpty()) {
				// Damn it, it didn't work! it may be that the user didn't give
				// permission.
				Toast.makeText(getApplicationContext(),
						getString(R.string.toast_access_token_null),
						Toast.LENGTH_LONG).show();
				return;
			}
			// If everything went fine, we should receive a Code parameter from
			// the browser
			Verifier ver = new Verifier(uri.getQueryParameter("code"));
			if (isOnline() && ver.getValue() != null
					&& !ver.getValue().isEmpty()) {
				Log.d(TAG, "Call to access token with verifier recieved");
				// With the code received, we ask the server to give us an
				// access token.
				// Call asyncTask to resolve the connection
				new GetAccessToken().execute(ver);
			} else
				Toast.makeText(getApplicationContext(),
						getString(R.string.toast_connection_failed),
						Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * <p>
	 * This simple method checks if there is an Internet connection available in
	 * the device or is currently connecting.
	 * </p>
	 * 
	 * @return A boolean specifying that there is connection (true) or there
	 *         isn't (false).
	 * 
	 */
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	/**
	 * <p>
	 * The class GetAccessToken extends {@link AsyncTask} to take care of the
	 * process of getting an access token from the server on the background.
	 * This way, if there are some problems with the connection, the UI will not
	 * be unresponsive.
	 * </p>
	 * 
	 * <p>
	 * The class overrides three functions:
	 * <ul>
	 * <li>{@link #onPreExecute}</li>
	 * <li>{@link #doInBackground}</li>
	 * <li>{@link #onPostExecute}</li>
	 * </ul>
	 * </p>
	 * 
	 */
	class GetAccessToken extends AsyncTask<Verifier, Void, Token> {
		/**
		 * Takes care of the visibility of the progress circle to show the user
		 * the app is working on something and not idle or broken.
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			findViewById(R.id.loadingAccessToken).setVisibility(View.VISIBLE);
			findViewById(R.id.loginB).setVisibility(View.GONE);
		}

		/**
		 * The method simply makes a call to the function getAccessToken in the
		 * OAuthService class provided by the library Scribe.
		 * 
		 * @param ver
		 *            It is an array of Verifier (from the Scribe library)
		 *            objects. Normally there is only need of one.
		 * 
		 * @return The access token is returned using the class in the Scribe
		 *         library, Token.
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Token doInBackground(Verifier... ver) {
			return serv.getAccessToken(null, (Verifier) ver[0]);
		}

		/**
		 * <p>
		 * Once the method {@link #doInBackground(Verifier...)} has finished, it
		 * calls to onPostExecute. This method will save the acquired access
		 * token in SharedPreferences and then call the next intent,
		 * {@link ComicListActivity}.
		 * </p>
		 * 
		 * @param tk
		 *            The access token sent by the previous method
		 *            {@link #doInBackground(Verifier...)}.
		 * 
		 * @see android.os.AsyncTask#onPostExecute(Result)
		 */
		@Override
		protected void onPostExecute(Token tk) {
			super.onPostExecute(tk);
			accToken = tk;
			// TODO: how to get refresh token
			if (accToken != null) {
				sharedPref.edit().putString(ACCESS_TOKEN, accToken.getToken())
						.putString(ACCESS_SECRET, accToken.getSecret())
						.commit();
				Intent intent = new Intent(getApplicationContext(),
						ComicListActivity.class);
				intent.putExtra(ACCESS_TOKEN, accToken.getToken());
				intent.putExtra(ACCESS_SECRET, accToken.getSecret());
				startActivity(intent);
			} else {
				Toast.makeText(getApplicationContext(),
						R.string.toast_access_token_null, Toast.LENGTH_LONG)
						.show();
			}
		}
	}

}