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

import org.scribe.model.Token;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * <p>
 * ComicListActivity is the activity that will present all the unread comics (or
 * those the user is subscripted to) and handle the user actions. It is launched
 * when the user logs in or if there's a saved session.
 * </p>
 * 
 * <p>
 * When the screen size is big enough, this activity will also be responsible of
 * showing the comics, together with the list.
 * </p>
 * 
 */
public class ComicListActivity extends FragmentActivity implements
		ComicListFragment.CallbacksListFragment,
		ComicDetailFragment.CallbacksComicDetail {

	private boolean mTwoPane;
	public boolean showUnread = true; // The list should show only unread
										// comics?
	private static final String TAG = "ComicListActivity";
	private static final int RELOAD_LIST_REQUEST = 1;
	private Token accToken;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "On create");
		super.onCreate(savedInstanceState);
		// Set layout (it depends on the screen size, check the styles.xml in
		// res)
		setContentView(R.layout.activity_comic_list);

		if (findViewById(R.id.comic_detail_container) != null) {
			mTwoPane = true;
			((ComicListFragment) getSupportFragmentManager().findFragmentById(
					R.id.comic_list)).setActivateOnItemClick(true);
		}

		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey(LoginActivity.ACCESS_TOKEN)
				&& extras.containsKey(LoginActivity.ACCESS_SECRET)) {
			accToken = new Token(extras.getString(LoginActivity.ACCESS_TOKEN),
					extras.getString(LoginActivity.ACCESS_SECRET));
		}
		if (extras != null && extras.containsKey("reloadUnread")
				&& extras.getInt("reloadUnread") != 0) {
			((ComicListFragment) getSupportFragmentManager().findFragmentById(
					R.id.comic_list)).loadList(false, showUnread,
					extras.getInt("reloadUnread"));
		}

		if (savedInstanceState != null
				&& savedInstanceState.containsKey(LoginActivity.ACCESS_TOKEN)) {
			accToken = new Token(
					savedInstanceState.getString(LoginActivity.ACCESS_TOKEN),
					savedInstanceState.getString(LoginActivity.ACCESS_SECRET));
		}
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("showunread")) {
			showUnread = savedInstanceState.getBoolean("showunread");
		}
	}

	/**
	 * <p>
	 * Saves the information of the access token for OAuth. Needed in case the
	 * app is closed long enough and it doesn't go through LoginActivity when
	 * restored.
	 * </p>
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(LoginActivity.ACCESS_TOKEN, accToken.getToken());
		outState.putString(LoginActivity.ACCESS_SECRET, accToken.getSecret());
		outState.putBoolean("showunread", showUnread);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.no_comic, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_log_out:
			Intent intent = new Intent(this, LoginActivity.class)
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.putExtra("DeleteToken", true);
			startActivity(intent);
			finish();
			break;
		case R.id.menu_refresh:
			((ComicListFragment) getSupportFragmentManager().findFragmentById(
					R.id.comic_list)).loadList(true, showUnread, 0);
			break;
		case R.id.menu_show_all:
			if (!isOnline()) {
				Toast.makeText(getApplicationContext(),
						getString(R.string.toast_need_internet),
						Toast.LENGTH_LONG).show();
				break;
			}
			if (showUnread) {
				showUnread = false;
				item.setTitle(R.string.menu_show_unread);
			} else {
				showUnread = true;
				item.setTitle(R.string.menu_show_all);
			}
			((ComicListFragment) getSupportFragmentManager().findFragmentById(
					R.id.comic_list)).loadList(true, showUnread, 0);
			// TODO: mantener igual entre sesiones?
			break;
		case R.id.menu_about:
			// TODO: create and open a new window with about info
		case R.id.menu_edit_account:
			// TODO: create and open edit account for user window
		case R.id.menu_mark_all_read:
			// TODO: sent to the server the order of mark all as read (ask for
			// confirmation depends on options)
		case R.id.menu_options:
			// TODO: create and open options window
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == RELOAD_LIST_REQUEST && resultCode == RESULT_OK) {
	    	((ComicListFragment) getSupportFragmentManager().findFragmentById(
					R.id.comic_list)).loadList(false, showUnread, data.getIntExtra("markedRead", 0));
	    }
	}

	@Override
	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	@Override
	public Token getAccToken() {
		return accToken;
	}

	@Override
	public void onItemSelected(String id) {
		if (mTwoPane) {
			Log.d(TAG, "Comic list item selected with multipane");
			Bundle arguments = new Bundle();
			arguments.putString(ComicDetailFragment.ARG_ITEM_ID, id);
			ComicDetailFragment fragment = new ComicDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.comic_detail_container, fragment)
					.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
					// .addToBackStack(null)
					.commit();

		} else {
			Log.d(TAG,
					"Comic list item selected, opening new activity with comic strips");
			Intent detailIntent = new Intent(this, ComicDetailActivity.class);
			detailIntent.putExtra(ComicDetailFragment.ARG_ITEM_ID, id);
			detailIntent.putExtra(LoginActivity.ACCESS_TOKEN,
					accToken.getToken());
			detailIntent.putExtra(LoginActivity.ACCESS_SECRET,
					accToken.getSecret());
			detailIntent.putExtra("ParentActivity", "ComicList");
			startActivityForResult(detailIntent, RELOAD_LIST_REQUEST);
		}
	}

	public void markRead(View v) {
		if (mTwoPane) {
			((ComicDetailFragment) getSupportFragmentManager()
					.findFragmentById(R.id.comic_detail_container)).markRead(v);
			((ComicListFragment) getSupportFragmentManager().findFragmentById(
					R.id.comic_list)).loadList(false, showUnread, 1);
		}
	}
	
	public void changeImageSize(View v) {
		((ComicDetailFragment) getSupportFragmentManager().findFragmentById(
				R.id.comic_detail_container)).changeImageSize(v);
	}

}
