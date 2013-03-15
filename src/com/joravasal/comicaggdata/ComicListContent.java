package com.joravasal.comicaggdata;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComicListContent {
	
    public static class ComicItem {

		public String id;
        public String name;
        public String url;
        public String unreadCount;

        public ComicItem(String id, String name, String url, String unread) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.unreadCount = unread;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getUnreadCount() {
			return unreadCount;
		}
        
		public void setUnreadCount(String unreadCount) {
			this.unreadCount = unreadCount;
		}
    }


	public static List<ComicItem> ITEMS = new ArrayList<ComicItem>();
	public static Map<String, ComicItem> ITEM_MAP = new HashMap<String, ComicItem>();

    static {
    }

//    private static void addItem(ComicItem item) {
//        ITEMS.add(item);
//        ITEM_MAP.put(item.id, item);
//    }
    
    public static void addItem(String id, String name, String url, String unread) {
    	ComicItem i = new ComicItem(id, name, url, unread);
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
