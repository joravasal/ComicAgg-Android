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
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

/**
 * <p>
 * ComicDetailActivity is the activity that will present all the strips from a
 * comic selected previously in the activity ComicListActivity. It is only used
 * in case the screen estate is too small to show both the list and the comics.
 * It is launched once the user select a comic from the previous activity.
 * </p>
 * 
 * 
 */
public class ComicDetailActivity extends FragmentActivity implements
		ComicDetailFragment.CallbacksComicDetail {

	private static final String TAG = "ComicDetailActivity";
	private Token accToken;
	private int markedRead = 0;
	private boolean isComicListParent = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "OnCreate");
		setContentView(R.layout.activity_comic_detail);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			accToken = new Token(getIntent().getStringExtra(
					LoginActivity.ACCESS_TOKEN), getIntent().getStringExtra(
					LoginActivity.ACCESS_SECRET));

			Bundle arguments = new Bundle();
			arguments.putString(ComicDetailFragment.ARG_ITEM_ID, getIntent()
					.getStringExtra(ComicDetailFragment.ARG_ITEM_ID));
			ComicDetailFragment fragment = new ComicDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.comic_detail_container, fragment).commit();

			isComicListParent = getIntent().getStringExtra("ParentActivity")
					.equals("ComicList");
		} else {
			accToken = new Token(
					savedInstanceState.getString(LoginActivity.ACCESS_TOKEN),
					savedInstanceState.getString(LoginActivity.ACCESS_SECRET));
			markedRead = savedInstanceState.getInt("markedRead", 0);
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
		Log.d(TAG, "Saving instance state");
		super.onSaveInstanceState(outState);
		outState.putString(LoginActivity.ACCESS_TOKEN, accToken.getToken());
		outState.putString(LoginActivity.ACCESS_SECRET, accToken.getSecret());
		outState.putInt("markedRead", markedRead);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			if (isComicListParent) {
				Intent i = new Intent();
				i.putExtra("markedRead", markedRead);
				setResult(RESULT_OK, i);
				finish();
			} else {
				Log.d(TAG, "Home button pressed on action bar");
				NavUtils.navigateUpTo(
						this,
						new Intent(this, ComicListActivity.class)
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
								.putExtra(LoginActivity.ACCESS_TOKEN,
										accToken.getToken())
								.putExtra(LoginActivity.ACCESS_SECRET,
										accToken.getSecret())
								.putExtra("reloadUnread", markedRead));
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		Intent i = new Intent();
		i.putExtra("markedRead", markedRead);
		setResult(RESULT_OK, i);
		super.onBackPressed();
	}

	public void changeImageSize(View v) {
		((ComicDetailFragment) getSupportFragmentManager().findFragmentById(
				R.id.comic_detail_container)).openFullscreenStrip(v);
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

	public void markRead(View v) {
		((ComicDetailFragment) getSupportFragmentManager().findFragmentById(
				R.id.comic_detail_container)).markRead(v);
		markedRead++;
	}
}
