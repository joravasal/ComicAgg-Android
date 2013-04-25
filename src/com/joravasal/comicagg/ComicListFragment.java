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

import java.util.concurrent.CancellationException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.joravasal.comicaggdata.ComicListContent;
import com.joravasal.tools.ComicAggOAuth2Api;
import com.joravasal.tools.ComicListArrayAdapter;
import com.joravasal.tools.GlobalVar;
import com.joravasal.tools.XMLtools;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * <p>
 * ComicListFragment is the class in charge of populating the main fragment that
 * appears in {@link ComicListActivity}. This class will present the user with a
 * list of unread comics or comics subscriptions so one can be chosen and read.
 * </p>
 * 
 * <p>
 * The list is populated making use of the API in ComicAgg server through the
 * inner class {@link LoadList} that extends {@link AsyncTask}
 * </p>
 * 
 */
public class ComicListFragment extends ListFragment {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private CallbacksListFragment mCallbacks = sDummyCallbacks;
	private int mActivatedPosition = ListView.INVALID_POSITION;
	private static final String TAG = "ComicListFragment";

	private boolean showUnread = true;
	private int unread_count = -9;

	/**
	 * <p>
	 * Interface to define what functions should the parent activity implement
	 * for normal behavior.
	 * </p>
	 * 
	 */
	public interface CallbacksListFragment {
		/**
		 * <p>
		 * Function to specify what happens when an item from the list is
		 * selected.
		 * </p>
		 * 
		 * @param id
		 *            The id of the list item selected.
		 * 
		 */
		public void onItemSelected(String id);

		/**
		 * <p>
		 * Simple function to check for Internet connectivity.
		 * </p>
		 * 
		 * @return A boolean specifying if there is a connection (true) or there
		 *         is none (false).
		 * 
		 */
		public boolean isOnline();

		/**
		 * <p>
		 * Simple function to get the Token object for OAuth2 authentication.
		 * </p>
		 * 
		 * @return The OAuth token object.
		 * 
		 */
		public Token getAccToken();
	}

	private static CallbacksListFragment sDummyCallbacks = new CallbacksListFragment() {
		@Override
		public void onItemSelected(String id) {
		}

		@Override
		public boolean isOnline() {
			return false;
		}

		@Override
		public Token getAccToken() {
			return null;
		}
	};

	public ComicListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "Empty OnCreate");
	}

	/**
	 * <p>
	 * Reload the list if there is Internet connection when the view and
	 * activity has been created.
	 * </p>
	 * 
	 * @see android.app.Fragment#onActivityCreated
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (savedInstanceState == null) {
			if (mCallbacks.isOnline())
				new LoadList(showUnread).execute();
			else
				Toast.makeText(getActivity(),
						getString(R.string.toast_need_internet),
						Toast.LENGTH_LONG).show();
		} else {
			if (savedInstanceState.containsKey("unreadcount")) {
				unread_count = savedInstanceState.getInt("unreadcount");
			}
			if (savedInstanceState.containsKey("showunread")) {
				showUnread = savedInstanceState.getBoolean("showunread");
			}
			loadList(false, showUnread, 0);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(TAG, "OnCreateView");
		return inflater.inflate(R.layout.fragment_comic_list, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.d(TAG, "OnViewCreated");
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof CallbacksListFragment)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (CallbacksListFragment) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);
		mActivatedPosition = position;
		mCallbacks.onItemSelected(ComicListContent.ITEMS.get(position).id);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
		if (unread_count >= 0) {
			outState.putInt("unreadcount", unread_count);
		}
		if (!showUnread) {
			outState.putBoolean("showunread", showUnread);
		}
	}

	/**
	 * <p>
	 * Selects the style of the list. Selects one item if there's enough screen
	 * space or does not select any, the selected item will be loaded.
	 * </p>
	 * 
	 * @param activateOnItemClick
	 *            Boolean that defines whether a selection should be made of the
	 *            item clicked.
	 * 
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	public void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	/**
	 * <p>
	 * Function that takes care of the creation of a new async task to reload
	 * the list (if there is Internet connection).
	 * </p>
	 * 
	 * @param reload
	 *            True if there is need to ask the API to reload the list of
	 *            comics.
	 * @param showUnread
	 *            Boolean to specify if the list should present unread comics or
	 *            all subscriptions.
	 * @param minusComics
	 *            If reload is false, the number of unread comics in the title
	 *            of the list will be reduced with this integer. Only used when
	 *            marking as read.
	 * 
	 */
	public void loadList(boolean reload, boolean showUnread, int minusComics) {
		this.showUnread = showUnread;
		if (reload) {
			if (mCallbacks.isOnline()) {
				new LoadList(showUnread).execute();
			} else {
				Toast.makeText(getActivity(),
						getString(R.string.toast_need_internet),
						Toast.LENGTH_LONG).show();
			}
		} else {
			Parcelable state = getListView().onSaveInstanceState();
			setListAdapter(new ComicListArrayAdapter(getActivity(),
					R.layout.row_comiclist, ComicListContent.ITEMS));
			if (showUnread) {
				((TextView) getActivity().findViewById(R.id.title_comic_list))
						.setText(getString(R.string.unread_comics));
			} else {
				((TextView) getActivity().findViewById(R.id.title_comic_list))
						.setText(getString(R.string.comics_subscriptions));
			}
			TextView tv = (TextView) getActivity().findViewById(
					R.id.unread_comic_list);
			unread_count -= minusComics;
			tv.setText(Integer.toString(unread_count));
			getListView().onRestoreInstanceState(state);
		}
		
	}

	/**
	 * <p>
	 * This class extends {@link AsyncTask} to take care of the process of
	 * loading the list of unread comics (or comic subscriptions) from the
	 * server on the background. This way, if there are some problems with the
	 * connection, the UI will not be unresponsive.
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
	private class LoadList extends AsyncTask<Void, Void, Document> {
		private boolean showUnread;

		public LoadList(boolean unread) {
			this.showUnread = unread;
		}

		/**
		 * Takes care of the visibility of the progress circle to show the user
		 * the app is working on something and not idle or broken.
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			View v1 = getActivity().findViewById(R.id.loadingList);
			View v2 = getActivity().findViewById(R.id.unread_comic_list);
			if (v1 != null && v2 != null) {
				v1.setVisibility(View.VISIBLE);
				v2.setVisibility(View.GONE);
			}
		}

		/**
		 * Creates a HTML request signed with the OAuth access token to the API
		 * in www.comicagg.com with all the necessary check-ups needed. The
		 * request to the API should receive a XML file with a list of comics.
		 * 
		 * @param v
		 *            Just void param to comply with the structure of ASync
		 *            Task.
		 * 
		 * @return A Document object with the whole XML received as an answer to
		 *         our request.
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Document doInBackground(Void... v) {
			// Request the API for the unread list of comics
			Log.d(TAG, "Loading list");
			Response response = null;
			try {
				if (mCallbacks.getAccToken() == null) {
					Log.e(TAG, "AccessToken was null!");
				}
				OAuthService serv = new ServiceBuilder()
						.provider(ComicAggOAuth2Api.class)
						.apiKey(getString(R.string.client_id))
						.apiSecret(getString(R.string.client_secret))
						.scope("write").callback("comicagg://oauth2")
						.signatureType(SignatureType.Header).build();
				OAuthRequest request;
				String base_url;
				if (GlobalVar.USING_DEV_PAGE)
					base_url = getString(R.string.base_url_api_dev);
				else
					base_url = getString(R.string.base_url_api_www);
				if (showUnread) {
					request = new OAuthRequest(Verb.GET, base_url + "unread/");
				} else {
					request = new OAuthRequest(Verb.GET, base_url + "subscription/");
				}
				serv.signRequest(mCallbacks.getAccToken(), request);
				response = request.send();
				if (response.getCode() != 200) {
					Log.e(TAG, "Conection error code: " + response.getCode());
					return null;
				}
				Log.d(TAG, "XML gotten");
			} catch (CancellationException e) {
				Log.e(TAG, "The request was cancelled");
				e.printStackTrace();
				return null;
			} catch (OAuthException e) {
				Log.e(TAG, "The connection failed, without Internet?");
				e.printStackTrace();
				return null;
			}
			if (response.getBody().isEmpty()) {
				Log.e(TAG, "XML was empty!!");
				changeListTitle("ERR", showUnread);
				return null;
			}

			// convert the string into useful data
			return XMLtools.stringToDoc(response.getBody());
		}

		/**
		 * <p>
		 * Once the method {@link #doInBackground(Void...)} has finished, it
		 * calls to onPostExecute. This method will create all the comic list
		 * content received as an answer from the API if the document received
		 * is in good shape. It adapts the title of the comic list as well.
		 * </p>
		 * 
		 * @param doc
		 *            The document containing the XML info from the API.
		 * 
		 * @see android.os.AsyncTask#onPostExecute(Result)
		 */
		@Override
		protected void onPostExecute(Document doc) {
			View v = getActivity().findViewById(R.id.loadingList);
			if (doc == null) {
				Toast.makeText(getActivity(),
						getString(R.string.toast_error_on_xml_list),
						Toast.LENGTH_LONG).show();
				Log.e(TAG, "The XML from the API has given null content");
				changeListTitle("ERR", showUnread);
				
				if (v != null) {
					v.setVisibility(View.GONE);
				}
				return;
			}
			// Clean out content class, so there's no repeated elements
			ComicListContent.clear();

			Log.d(TAG, "Get each individual comic from XML");
			NodeList nodes = doc.getElementsByTagName("comic");

			int length = nodes.getLength();
			for (int i = 0; i < length; i++) {
				Element e = (Element) nodes.item(i);
				if (showUnread)
					ComicListContent
							.addItem(e.getAttributes().getNamedItem("id")
									.getNodeValue(), e.getAttributes()
									.getNamedItem("name").getNodeValue(), e
									.getAttributes().getNamedItem("website")
									.getNodeValue(), e.getAttributes()
									.getNamedItem("unreadcount").getNodeValue());
				else
					ComicListContent
							.addItem(e.getAttributes().getNamedItem("id")
									.getNodeValue(), e.getAttributes()
									.getNamedItem("name").getNodeValue(), "",
									"0");
			}

			unread_count = length;
			changeListTitle(Integer.toString(length), showUnread);
			Log.d(TAG, "Load comics onto listadapter");
			
			View v2 = getActivity().findViewById(R.id.unread_comic_list);
			if (v != null && v2 != null) {
				v.setVisibility(View.GONE);
				v2.setVisibility(View.VISIBLE);
			}
			setListAdapter(new ComicListArrayAdapter(getActivity(),
					R.layout.row_comiclist, ComicListContent.ITEMS));
		}

		/**
		 * <p>
		 * Puts a message in the title of the comic list. Used to show the
		 * unread count of comics, the number of subscriptions or some error.
		 * </p>
		 * 
		 * @param message
		 *            The string that will be appended in the comic-list title.
		 * @param unread
		 *            Boolean specifying if the list is showing the unread
		 *            comics or the subscriptions.
		 */
		private void changeListTitle(String message, boolean unread) {
			if (unread) {
				((TextView) getActivity().findViewById(R.id.title_comic_list))
						.setText(getString(R.string.unread_comics));
			} else {
				((TextView) getActivity().findViewById(R.id.title_comic_list))
						.setText(getString(R.string.comics_subscriptions));
			}
			((TextView) getActivity().findViewById(R.id.unread_comic_list))
					.setText(message);
		}
	}
}
