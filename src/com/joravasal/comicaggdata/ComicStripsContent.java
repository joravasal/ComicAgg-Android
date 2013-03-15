package com.joravasal.comicaggdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;

public class ComicStripsContent {
	
	public static class StripItem {

		public String id;
        public String url;
        public String alt;
        public String date;
        public Bitmap image;

        public StripItem(String id, String url, String alt, String date, Bitmap bm) {
            this.id = id;
            this.url = url;
            this.alt = alt;
            this.date = date;
            this.image = bm;
        }
        @Override
        public String toString() {
            return url + " (" + alt + ")";
        }
    }

	public static String id;
	public static List<StripItem> ITEMS = new ArrayList<StripItem>();
	public static Map<String, StripItem> ITEM_MAP = new HashMap<String, StripItem>();

    static {
    }

//    private static void addItem(ComicItem item) {
//        ITEMS.add(item);
//        ITEM_MAP.put(item.id, item);
//    }
    
    public static void addItem(String id, String url, String alt, String date, Bitmap bm) {
    	StripItem i = new StripItem(id, url, alt, date, bm);
        ITEMS.add(i);
        ITEM_MAP.put(id, i);
    }
    
    public static void clear(){
    	ITEMS.clear();
    	ITEM_MAP.clear();
    }
    
//    private static void setAsRead(int id){
//		ITEM_MAP.get(id).setUnreadCount("0");
//    }
}
