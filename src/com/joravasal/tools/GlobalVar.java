package com.joravasal.tools;

/**
 * In case some static variable should be reached anywhere in the software, it
 * should be listed here.
 * 
 * @author yprum
 * 
 */
public abstract class GlobalVar {
	/**
	 * Variable that controls which version of the webpage is being used.
	 * Set to {@code true} if we are using the dev.comicagg.com page for testing.
	 */
	public final static boolean USING_DEV_PAGE = true;
}
