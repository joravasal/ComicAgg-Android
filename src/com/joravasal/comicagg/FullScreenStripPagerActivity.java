package com.joravasal.comicagg;

import com.joravasal.comicaggdata.ComicStripsContent;
import com.joravasal.comicaggdata.ComicStripsContent.StripItem;
import com.joravasal.tools.CustomViewPager;

import android.support.v4.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Build.VERSION;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
public class FullScreenStripPagerActivity extends FragmentActivity {
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
		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.fragment_fullscreen_strip,
					container, false);
			ImageView stripView = (ImageView) v
					.findViewById(R.id.strip_fullscreen);
			TextView altText = (TextView) v.findViewById(R.id.strip_fs_alttext);

			StripItem stripObj = ComicStripsContent.ITEMS.get(mNum);
			stripView.setImageBitmap(stripObj.image);

			if (stripObj.alt.isEmpty()) {
				altText.setVisibility(View.GONE);
			} else {
				altText.setText(stripObj.alt);
			}

			Point outSize = new Point();
			if (VERSION.SDK_INT >= 13) {
				getActivity().getWindowManager().getDefaultDisplay()
						.getSize(outSize);
			} else {
				outSize.x = getActivity().getWindowManager()
						.getDefaultDisplay().getWidth();
				outSize.y = getActivity().getWindowManager()
						.getDefaultDisplay().getHeight();
			}
			int screenW = outSize.x;
			int screenH = outSize.y;
			int imageW = (ComicStripsContent.ITEMS.get(mNum).image.getWidth());
			int imageH = (ComicStripsContent.ITEMS.get(mNum).image.getHeight());
			// set maximum scroll amount.
			// TODO: ideally, instead of using the size of the screen, we'd use
			// the real estate for the ImageView shown to the user. But there is
			// no way to catch its size this early.
			int maxX = 0;
			int maxY = 0;
			// allows to move a bit further than the border of the image
			int xOffset = 0;
			int yOffset = 0;
			// If screen is smaller than the image, we allow as much movement as
			// needed to see the whole image, plus the offset.
			if (screenW < imageW) {
				maxX = (int) (imageW - screenW);
				xOffset = 15;
			}
			if (screenH < imageH) {
				maxY = (int) (imageH - screenH);
				yOffset = 15;
			}

			// set scroll limits
			final int maxLeft = -xOffset;
			final int maxRight = maxX + xOffset;
			final int maxTop = -yOffset;
			final int maxBottom = maxY + yOffset;

			// set touch listener
			stripView.setOnTouchListener(new View.OnTouchListener() {
				float downX, downY;
				int totalX, totalY;
				int scrollByX, scrollByY;

				public boolean onTouch(View view, MotionEvent event) {
					float currentX, currentY;
					CustomViewPager cvp = (CustomViewPager) getActivity()
							.findViewById(R.id.pager);

					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						downX = event.getX();
						downY = event.getY();
						break;

					case MotionEvent.ACTION_MOVE:
						currentX = event.getX();
						currentY = event.getY();
						scrollByX = (int) (downX - currentX);
						scrollByY = (int) (downY - currentY);

						// scrolling to left side of image (pic moving to the
						// right)
						if (currentX > downX) {
							if (totalX == maxLeft) {
								scrollByX = 0;
								cvp.setPagingEnabled(true);
							}
							if (totalX > maxLeft) {
								totalX = totalX + scrollByX;
								cvp.setPagingEnabled(false);
							}
							if (totalX < maxLeft) {
								scrollByX = maxLeft - (totalX - scrollByX);
								totalX = maxLeft;
								cvp.setPagingEnabled(false);
							}
						}

						// scrolling to right side of image (pic moving to the
						// left)
						if (currentX < downX) {
							if (totalX == maxRight) {
								scrollByX = 0;
								cvp.setPagingEnabled(true);
							}
							if (totalX < maxRight) {
								totalX = totalX + scrollByX;
								cvp.setPagingEnabled(false);
							}
							if (totalX > maxRight) {
								scrollByX = maxRight - (totalX - scrollByX);
								totalX = maxRight;
								cvp.setPagingEnabled(false);
							}
						}

						// scrolling to top of image (pic moving to the bottom)
						if (currentY > downY) {
							cvp.setPagingEnabled(false);
							if (totalY == maxTop) {
								scrollByY = 0;
							}
							if (totalY > maxTop) {
								totalY = totalY + scrollByY;
							}
							if (totalY < maxTop) {
								scrollByY = maxTop - (totalY - scrollByY);
								totalY = maxTop;
							}
						}

						// scrolling to bottom of image (pic moving to the top)
						if (currentY < downY) {
							cvp.setPagingEnabled(false);
							if (totalY == maxBottom) {
								scrollByY = 0;
							}
							if (totalY < maxBottom) {
								totalY = totalY + scrollByY;
							}
							if (totalY > maxBottom) {
								scrollByY = maxBottom - (totalY - scrollByY);
								totalY = maxBottom;
							}
						}

						view.scrollBy(scrollByX, scrollByY);
						downX = currentX;
						downY = currentY;
						break;

					case MotionEvent.ACTION_UP:
						cvp.setPagingEnabled(true);
					}

					return true;
				}
			});
			// Put the image at the top left corner (it starts centered).
			stripView.scrollTo(-maxX / 2, -maxY / 2);
			return v;
		}

	}

}
