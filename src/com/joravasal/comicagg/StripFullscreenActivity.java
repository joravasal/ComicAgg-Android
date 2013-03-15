package com.joravasal.comicagg;

import com.joravasal.comicaggdata.ComicStripsContent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class StripFullscreenActivity extends Activity {
	public static final String TAG = "StripFullscreenActivity";

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "OnCreate launched");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_strip_fullscreen);

		ImageView stripView = (ImageView) findViewById(R.id.strip_fullscreen);

		stripView.setImageBitmap(ComicStripsContent.ITEMS.get(getIntent()
				.getIntExtra(ComicDetailFragment.STRIP_IMAGE_ID, 0)).image);

		Point outSize = new Point();
		if(VERSION.SDK_INT >= 13){
			this.getWindowManager().getDefaultDisplay().getSize(outSize);
		} else {
			outSize.x = this.getWindowManager().getDefaultDisplay().getWidth();
			outSize.y = this.getWindowManager().getDefaultDisplay().getHeight();
		}
		int screenW = outSize.x;
		int screenH = outSize.y;
		int imageW = (ComicStripsContent.ITEMS.get(0).image.getWidth());
		int imageH = (ComicStripsContent.ITEMS.get(0).image.getHeight());
		// set maximum scroll amount (based on center of image)
		int maxX = 0;
		int maxY = 0;
		if (screenW < imageW){
			maxX = (int) (imageW - screenW)/2;
		}
		if (screenH < imageH){
			maxY = (int) (imageH - screenH)/2;
		}

		// set scroll limits
		final int maxLeft = -maxX;
		final int maxRight = maxX;
		final int maxTop = -maxY;
		final int maxBottom = maxY;

		// set touchlistener
		stripView.setOnTouchListener(new View.OnTouchListener() {
			float downX, downY;
			int totalX, totalY;
			int scrollByX, scrollByY;

			public boolean onTouch(View view, MotionEvent event) {
				float currentX, currentY;
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

					// scrolling to left side of image (pic moving to the right)
					if (currentX > downX) {
						if (totalX == maxLeft) {
							scrollByX = 0;
						}
						if (totalX > maxLeft) {
							totalX = totalX + scrollByX;
						}
						if (totalX < maxLeft) {
							scrollByX = maxLeft - (totalX - scrollByX);
							totalX = maxLeft;
						}
					}

					// scrolling to right side of image (pic moving to the left)
					if (currentX < downX) {
						if (totalX == maxRight) {
							scrollByX = 0;
						}
						if (totalX < maxRight) {
							totalX = totalX + scrollByX;
						}
						if (totalX > maxRight) {
							scrollByX = maxRight - (totalX - scrollByX);
							totalX = maxRight;
						}
					}

					// scrolling to top of image (pic moving to the bottom)
					if (currentY > downY) {
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

				}

				return true;
			}
		});
		stripView.scrollTo(-maxX, -maxY);
	}
}
