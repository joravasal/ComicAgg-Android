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

import java.io.FileNotFoundException;
import java.io.InputStream;
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
import com.joravasal.comicaggdata.ComicListContent.ComicItem;
import com.joravasal.comicaggdata.ComicStripsContent;
import com.joravasal.tools.ComicAggOAuth2Api;
import com.joravasal.tools.GlobalVar;
import com.joravasal.tools.XMLtools;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

//TODO if a comic is pressed, and back key is pressed right after, before the comic is loaded, trying to open the comic again before it finished loading will crash the app

/**
 * <p>
 * ComicDetailFragment is the class in charge of populating the content when a
 * comic has been selected in {@link ComicListActivity}. This class will present
 * the user with a list of comic strips and their alternate text. The user can
 * also mark as read (or vote) the comic from this fragment. As a fragment it
 * can be opened from ComicListActivity or ComicDetailActivity.
 * </p>
 * 
 * <p>
 * The list is populated making use of the API in ComicAgg server through the
 * inner class {@link GetComicsStrips} that extends {@link AsyncTask}
 * </p>
 * 
 */
public class ComicDetailFragment extends Fragment {

	private CallbacksComicDetail mCallbacks = sDummyCallbacks;
	public static final String ARG_ITEM_ID = "item_id";
	public static final String COMICAGG = "ComicAgg: ";

	private static final String TAG = "ComicDetailFragment";
	public static final String STRIP_IMAGE_ID = "strip_image_id";
	public static final String VERTICAL_SCROLLING_POSITION = "scroll_pos_y";

	ComicItem comicItem;

	/**
	 * <p>
	 * Interface to define what functions should the parent activity implement
	 * for normal behavior.
	 * </p>
	 * 
	 */
	public interface CallbacksComicDetail {
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

	private static CallbacksComicDetail sDummyCallbacks = new CallbacksComicDetail() {
		@Override
		public boolean isOnline() {
			return false;
		}

		@Override
		public Token getAccToken() {
			return null;
		}
	};

	public ComicDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "OnCreate");
		if (getArguments().containsKey(ARG_ITEM_ID)) {
			comicItem = ComicListContent.ITEM_MAP.get(getArguments().getString(
					ARG_ITEM_ID));

			if (comicItem != null) {
				getActivity().setTitle(COMICAGG + comicItem.toString());
			}
		}

		setHasOptionsMenu(true);
	}

	@TargetApi(14)
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.d(TAG, "OnCreateOptionsMenu");
		inflater.inflate(R.menu.comic_selected_extra, menu);
		// if (!((ComicListActivity) getActivity()).showUnread)
		// menu.findItem(R.id.menu_show_all).setTitle(
		// R.string.menu_show_unread);

		ShareActionProvider mShareActionProvider = (ShareActionProvider) menu
				.findItem(R.id.menu_share).getActionProvider();

		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		// Uri uri = Uri.parse(comicItem.url);
		shareIntent.putExtra(Intent.EXTRA_STREAM, comicItem.url);
		mShareActionProvider.setShareIntent(shareIntent);
	}

	@SuppressLint({ "NewApi" })
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_comic_detail,
				container, false);
		if (comicItem == null && savedInstanceState.containsKey(ARG_ITEM_ID)) {
			comicItem = new ComicItem(
					savedInstanceState.getString(ARG_ITEM_ID),
					savedInstanceState.getString("comicname"),
					savedInstanceState.getString("comicurl"),
					savedInstanceState.getString("unreadcount"));
		}
		int unread = Integer.parseInt(comicItem.unreadCount);
		if (unread == 0) {
			rootView.findViewById(R.id.vote_bar).setVisibility(View.GONE);
		}
		LinearLayout stripList = (LinearLayout) rootView
				.findViewById(R.id.strips_list);
		LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);

		if (comicItem.id.equals(ComicStripsContent.id)
				&& ComicStripsContent.ITEMS.size() > unread) {
			unread = ComicStripsContent.ITEMS.size();
		}
		for (int i = 1; i < unread; i++) {
			ImageView iv = new ImageView(getActivity());
			iv.setId(Integer.MAX_VALUE - i);
			iv.setPadding(16, 16, 16, 0);
			iv.setContentDescription(getString(R.string.strip_description));
			iv.setAdjustViewBounds(true);

			iv.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO check if there is need of opening? or always open?
					openFullscreenStrip(v);
				}
			});

			TextView tv = new TextView(getActivity());
			tv.setId(i);
			tv.setPadding(16, 4, 16, 4);
			tv.setGravity(Gravity.CENTER);

			stripList.addView(iv, layoutParams);
			stripList.addView(tv, layoutParams);
		}

		if (!comicItem.id.equals(ComicStripsContent.id)) {
			new GetComicsStrips(comicItem.id, unread, rootView).execute();
		} else {
			new GetComicsStrips(comicItem.id, unread, rootView)
					.onPostExecute(null);
		}
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(VERTICAL_SCROLLING_POSITION)
				&& VERSION.SDK_INT >= 14) {
			rootView.findViewById(R.id.comic_scrollView).scrollTo(0,
					savedInstanceState.getInt(VERTICAL_SCROLLING_POSITION));
		}
		return rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARG_ITEM_ID, comicItem.id);
		outState.putString("unreadcount", comicItem.unreadCount);
		outState.putString("comicname", comicItem.name);
		outState.putString("comicurl", comicItem.url);
		outState.putInt(VERTICAL_SCROLLING_POSITION, getActivity()
				.findViewById(R.id.comic_scrollView).getScrollY());
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof CallbacksComicDetail)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (CallbacksComicDetail) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = sDummyCallbacks;
	}

	/**
	 * <p>
	 * Function that takes care of marking comic strips as read. Depending on
	 * the view that makes the call, it will upvote, downvote or just mark as
	 * read the comic. The request to the API is made through a class that
	 * extends {@link ASyncTask}.
	 * </p>
	 * 
	 * @param v
	 *            The view that makes the call to the function.
	 * 
	 */
	public void markRead(View v) {
		int vote;
		switch (v.getId()) {
		case R.id.downvote:
			vote = -1;
			break;
		case R.id.upvote:
			vote = 1;
			break;
		default:
			vote = 0;
			break;
		}
		new MarkComicRead(v, vote, comicItem.id).execute();
		comicItem.unreadCount = "0";
	}

	/**
	 * <p>
	 * This function will create a new activity that shows the strip comics full
	 * size. The activity is {@link FullScreenStripPagerActivity}, which allows
	 * to move between all the strips in the comic.
	 * </p>
	 * 
	 * @param v
	 *            The imageView (the comic strip) that calls the function when a
	 *            click event happens on it.
	 */
	public void openFullscreenStrip(View v) {
		Log.d(TAG, "Opening new activity with full screen image");
		int viewID = 0;
		if (v.getId() != R.id.comic0) {
			viewID = Integer.MAX_VALUE - v.getId();
		}
//		startActivity(new Intent(getActivity(),
//				FullScreenStripPagerActivity.class).putExtra(STRIP_IMAGE_ID,
//				viewID));
		startActivity(new Intent(getActivity(),
				FSStripWebViewPagerActivity.class).putExtra(STRIP_IMAGE_ID,
				viewID));
	}

	/**
	 * <p>
	 * This class extends {@link AsyncTask} to take care of the process of
	 * loading the strips of the comic selected by the user from the server on
	 * the background. This way, if there are some problems with the connection,
	 * the UI will not be unresponsive.
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
	private class GetComicsStrips extends AsyncTask<Void, Void, Void> {
		private int unread;
		private String id;
		private View rootView;

		public GetComicsStrips(String id, int unread, View rootView) {
			this.unread = unread;
			this.id = id;
			this.rootView = rootView;
		}

		/**
		 * Takes care of the visibility of the progress circle to show the user
		 * the app is working on something and not idle or broken.
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			View v = rootView.findViewById(R.id.loadingStrips);
			if (v != null) {
				v.setVisibility(View.VISIBLE);
			}
		}

		/**
		 * Creates a HTML request signed with the OAuth access token to the API
		 * in www.comicagg.com with all the necessary check-ups needed. The
		 * request should receive an XML with the unread strips of a comic or
		 * the last one. It reads the XML and creates the content of the comic
		 * as it loads the bitmaps from the Internet.
		 * 
		 * @param v
		 *            Just void param to comply with the structure of ASync
		 *            Task.
		 * 
		 * @return Returns null to comply with the structure of ASync Task
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Void doInBackground(Void... v) {
			// Request the API for the unread list of comics
			Log.d(TAG, "Loading strips");
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

				if (unread != 0) {
					request = new OAuthRequest(Verb.GET, base_url + "unread/"
							+ id + "/");
				} else {
					request = new OAuthRequest(Verb.GET, base_url + "comic/"
							+ id + "/");
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
			} catch (OAuthException e) {
				e.printStackTrace();
				Log.e(TAG, "The connection failed, without Internet?");
				return null;
			}
			if (response.getBody().isEmpty()) {
				Log.e(TAG, "XML was empty!!");
				return null;
			}
			ComicStripsContent.id = id;
			// convert the string into useful data
			Document doc = XMLtools.stringToDoc(response.getBody());
			if (doc == null) {
				cancel(true);
				Log.e(TAG, "The XML from the API has given null content");
			}
			ComicStripsContent.clear();

			Log.d(TAG, "Get each individual strip from XML");
			NodeList nodes = doc.getElementsByTagName("strip");

			int length = nodes.getLength();
			if (length == 0) {
				cancel(true);
				return null;
			}
			for (int i = 0; i < length; i++) {
				Element e = (Element) nodes.item(i);

				Bitmap bm = null;
				try {
					InputStream in = new java.net.URL(e.getAttributes()
							.getNamedItem("imageurl").getNodeValue())
							.openStream();
					bm = BitmapFactory.decodeStream(in);
				} catch (FileNotFoundException exc) {
					Log.e(TAG,
							e.getAttribute("imageurl") + ": "
									+ exc.getMessage());
					exc.printStackTrace();
					bm = BitmapFactory.decodeResource(getResources(),
							R.drawable.broken_image);
				} catch (Exception exc) {
					Log.e(TAG, exc.getMessage());
					exc.printStackTrace();
				} catch (OutOfMemoryError err) {
					Log.e(TAG,
							e.getAttribute("imageurl") + ": "
									+ err.getMessage());
					err.printStackTrace();
					bm = BitmapFactory.decodeResource(getResources(),
							R.drawable.broken_image);
				}

				ComicStripsContent.addItem(
						e.getAttributes().getNamedItem("id").getNodeValue(),
						e.getAttributes().getNamedItem("imageurl")
								.getNodeValue(),
						Html.fromHtml(
								e.getAttributes().getNamedItem("imagetext")
										.getNodeValue()).toString(), e
								.getAttributes().getNamedItem("date")
								.getNodeValue(), bm);

			}
			return null;
		}

		/**
		 * <p>
		 * The function {@link doInBackground(Void...)} might be cancelled if
		 * the document received as response from the API is null or there are
		 * no elements in the document. This may happen if the data base in the
		 * server has no strips from the comic for being too old.
		 * </p>
		 * 
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			Log.e(TAG, "Loading comic strips was cancelled");
			View v = rootView.findViewById(R.id.loadingStrips);
			if (v != null) {
				v.setVisibility(View.GONE);
			}
			Toast.makeText(getActivity(),
					getString(R.string.toast_error_cancelled),
					Toast.LENGTH_LONG).show();
			super.onCancelled();
		}

		/**
		 * <p>
		 * Once the method {@link #doInBackground(Void...)} has finished, it
		 * calls to onPostExecute. This method will populate the imageViews and
		 * textViews with the information that was just received from the
		 * Internet.
		 * </p>
		 * 
		 * @param res
		 *            Always null, it is here as it has to receive something
		 *            from {@link doInBackground(Void...)}.
		 * 
		 * @see android.os.AsyncTask#onPostExecute(Result)
		 */
		@Override
		protected void onPostExecute(Void res) {

			View v = rootView.findViewById(R.id.loadingStrips);
			if (v != null) {
				v.setVisibility(View.GONE);
			}

			if (ComicStripsContent.ITEMS.size() > 0) {
				((ImageView) rootView.findViewById(R.id.comic0))
						.setImageBitmap(ComicStripsContent.ITEMS.get(0).image);
				((TextView) rootView.findViewById(R.id.alt_text_comic0))
						.setText(ComicStripsContent.ITEMS.get(0).alt);
			}
			for (int i = 1; i < unread; i++) {
				((ImageView) rootView.findViewById(Integer.MAX_VALUE - i))
						.setImageBitmap(ComicStripsContent.ITEMS.get(i).image);
				((TextView) rootView.findViewById(i))
						.setText(ComicStripsContent.ITEMS.get(i).alt);
			}
		}
	}

	/**
	 * <p>
	 * This class extends {@link AsyncTask} to take care of the process of
	 * marking the strips of the comic as read (maybe voting in the process.
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
	private class MarkComicRead extends AsyncTask<Void, Void, Void> {
		private View v;
		private int vote;
		private String id;

		public MarkComicRead(View v, int vote, String id) {
			this.v = v;
			this.vote = vote;
			this.id = id;
		}

		/**
		 * Takes care of the visibility of the progress circle to show the user
		 * the app is working on something and not idle or broken.
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			v.findViewById(R.id.voteimage).setVisibility(View.GONE);
			v.findViewById(R.id.loadingvote).setVisibility(View.VISIBLE);
			v.setEnabled(false);
		}

		/**
		 * Creates a HTML POST request signed with the OAuth access token to the
		 * API in www.comicagg.com with all the necessary check-ups needed. The
		 * request as a POST doesn't receive anything, it just marks the comic
		 * as read in the server.
		 * 
		 * @param v
		 *            Just void param to comply with the structure of ASync
		 *            Task.
		 * 
		 * @return Returns null to comply with the structure of ASync Task
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Void doInBackground(Void... v) {
			// Request the API for the unread list of comics
			Log.d(TAG, "Marking comic as read in background");
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
				String base_url;
				if (GlobalVar.USING_DEV_PAGE)
					base_url = getString(R.string.base_url_api_dev);
				else
					base_url = getString(R.string.base_url_api_www);
				OAuthRequest request = new OAuthRequest(Verb.POST, base_url
						+ "comic/" + id + "/");
				request.addBodyParameter("vote", Integer.toString(vote));
				serv.signRequest(mCallbacks.getAccToken(), request);
				response = request.send();
				if (response.getCode() != 200) {
					Log.e(TAG, "Conection error code: " + response.getCode());
					return null;
				}
				Log.d(TAG, "Response gotten");
			} catch (CancellationException e) {
				Log.e(TAG, "The request was cancelled");
				e.printStackTrace();
				return null;
			} catch (OAuthException e) {
				Log.e(TAG, "The connection failed, without Internet?");
				e.printStackTrace();
				return null;
			}
			return null;
		}

		/**
		 * <p>
		 * Once the method {@link #doInBackground(Void...)} has finished, it
		 * calls to onPostExecute. This method will restore the image for the
		 * button pressed and make all voting buttons unavailable.
		 * </p>
		 * 
		 * @param res
		 *            Always null, it is here as it has to receive something
		 *            from {@link doInBackground(Void...)}.
		 * 
		 * @see android.os.AsyncTask#onPostExecute(Result)
		 */
		@Override
		protected void onPostExecute(Void res) {
			((View) v.getParent()).setVisibility(View.GONE);
			Toast.makeText(getActivity(),
					getString(R.string.toast_marked_read), Toast.LENGTH_SHORT)
					.show();
		}
	}
}
