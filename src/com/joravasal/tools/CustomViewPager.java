package com.joravasal.tools;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * <p>
 * This custom {@link ViewPager} allow control of the scrolling to the next and
 * previous images. This scrolling can be cancelled calling the function
 * {@code setPagingEnabled(false)}, and the contrary with value {@code true}.
 * </p>
 */
public class CustomViewPager extends ViewPager {

	private boolean isEnabled;

	public CustomViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.isEnabled = true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (this.isEnabled) {
			return super.onTouchEvent(event);
		}

		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (this.isEnabled) {
			return super.onInterceptTouchEvent(event);
		}

		return false;
	}

	public void setPagingEnabled(boolean enabled) {
		this.isEnabled = enabled;
	}
}
