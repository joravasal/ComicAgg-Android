package com.joravasal.comicagg;

import java.io.ByteArrayOutputStream;

import com.joravasal.comicaggdata.ComicStripsContent;
import com.joravasal.comicaggdata.ComicStripsContent.StripItem;
import android.support.v4.app.FragmentActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * <p>
 * This class extends the functionality of {@link FragmentActivity} hosting a
 * {@link ViewPager} to move between the strips of a comic. All this with a
 * full screen and showing the comics at full size. In a future this class will
 * allow as well to zoom in/out in the comics. Mostly like any gallery for
 * browsing images would allow.
 * </p>
 * 
 */
public class FSStripWebViewPagerActivity extends FragmentActivity {
	public static final String TAG = "FullScreenStrip Pager Activity";
	ViewPager comicPager;
	FullscreenStripAdapter fsStripAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate activity");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pager_fullscreen_strip);

		int image_id = getIntent().getIntExtra(
				ComicDetailFragment.STRIP_IMAGE_ID, 0);
		fsStripAdapter = new FullscreenStripAdapter(getSupportFragmentManager());

		comicPager = (ViewPager) findViewById(R.id.pager);
		comicPager.setAdapter(fsStripAdapter);
		comicPager.setCurrentItem(image_id);

	}

	/**
	 * <p>
	 * Custom adapter to set the {@link FragmentStatePagerAdapter} with the
	 * appropriate data.
	 * </p>
	 * 
	 */
	public static class FullscreenStripAdapter extends
			FragmentStatePagerAdapter {
		public static final String TAG = "FullScreenStrip Pager Adapter";

		public FullscreenStripAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
			Log.d(TAG, "New adapter created");
		}

		@Override
		public int getCount() {
			Log.d(TAG,
					"GetCount called, number of items: "
							+ Integer.toString(ComicStripsContent.ITEMS.size()));
			return ComicStripsContent.ITEMS.size();
		}

		@Override
		public Fragment getItem(int position) {
			Log.d(TAG,
					"New fragment requested, item #"
							+ Integer.toString(position));
			return FSStripFragment.newInstance(position);
		}
	}

	/**
	 * <p>
	 * Custom fragment to show the strip information inside the pager, image and
	 * alt text.
	 * 
	 * This fragment has the code to control the image through touch events.
	 * </p>
	 */
	public static class FSStripFragment extends Fragment {
		public static final String TAG = "FullScreenStrip Pager Fragment";
		int mNum;

		/**
		 * Create a new instance of FSStripFragment.
		 * 
		 * @param num
		 *            Which instance of the data we want to represent, an index
		 *            on our array of comic strips for our case.
		 * 
		 * @return The new fragment created.
		 */
		static FSStripFragment newInstance(int num) {
			Log.d(TAG, "Creating new instance of FSStripFragment");
			FSStripFragment f = new FSStripFragment();

			Bundle args = new Bundle();
			args.putInt("num", num);
			f.setArguments(args);
			return f;
		}

		/**
		 * When creating, retrieve this instance's number from its arguments and
		 * save it as global variable.
		 * 
		 * @param savedInstanceState
		 *            Bundle with arguments for the creation of the fragment.
		 */
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mNum = getArguments() != null ? getArguments().getInt("num") : 0;
		}

		/**
		 * The Fragment's UI has two elements, an imageView with the comic strip
		 * and a textView for the altText (if present). We set up the touch
		 * listener for the imageView to browse through it and control when the
		 * pager moves to the next image.
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.fragment_fs_strip_webview,
					container, false);
			WebView stripView = (WebView) v
					.findViewById(R.id.strip_fullscreen);
			TextView altText = (TextView) v.findViewById(R.id.strip_fs_alttext);

			StripItem stripObj = ComicStripsContent.ITEMS.get(mNum);
			//stripView.setImageBitmap(stripObj.image);

//			Bitmap map = stripObj.image;
//		    ByteArrayOutputStream bbb = new ByteArrayOutputStream();
//		    map.compress(Bitmap.CompressFormat.PNG, 100, bbb);
//
//		    byte[] bit = bbb.toByteArray();
//
//		    String imgToString = Base64.encode(bit, Base64.DEFAULT).toString();
//
//		    String imgTag = "<img src='data:image/png;base64," + imgToString               
//		    	    + "' align='left' bgcolor='ff0000'/>"; 
//		    stripView.loadData(imgTag, "text/html", "utf-8");
			
			Display display = getActivity().getWindowManager().getDefaultDisplay();
			@SuppressWarnings("deprecation")
			int width=display.getWidth();

//			String data="<html><head><title></title><meta name=\"viewport\"\"content=\"width="+width+", initial-scale=0.65 \" /></head>";
//			data=data+"<body><br/><center><img src=\""+stripObj.url+"\" /></center></body></html>";
			String data="<html><head><title></title></head>";
			data=data+"<body><br/><center><img src=\""+stripObj.url+"\" width=\"90%\"/></center><br/><br/></body></html>";
			
			stripView.setInitialScale(30);
			WebSettings stripSettings = stripView.getSettings(); 
			stripSettings.setBuiltInZoomControls(true);
			stripSettings.setDisplayZoomControls(false);
			stripSettings.setLoadWithOverviewMode(true);
			stripSettings.setUseWideViewPort(true);
			
			stripView.loadData(data, "text/html", null);
			
			
			if (stripObj.alt.isEmpty()) {
				altText.setVisibility(View.GONE);
			} else {
				altText.setText(stripObj.alt);
			}

			return v;
		}

	}

}
