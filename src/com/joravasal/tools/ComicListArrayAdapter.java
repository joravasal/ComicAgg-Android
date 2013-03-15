package com.joravasal.tools;

import java.util.List;

import com.joravasal.comicagg.R;
import com.joravasal.comicaggdata.ComicListContent;
import com.joravasal.comicaggdata.ComicListContent.ComicItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ComicListArrayAdapter extends
		ArrayAdapter<ComicListContent.ComicItem> {

	private List<ComicListContent.ComicItem> data;
	private int rowlayoutID;
	private static LayoutInflater inflater = null;

	public ComicListArrayAdapter(Context ctxt, int rowlayout,
			List<ComicItem> items) {
		super(ctxt, rowlayout, items);
		this.rowlayoutID = rowlayout;
		this.data = items;
		ComicListArrayAdapter.inflater = (LayoutInflater) ctxt
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = inflater.inflate(rowlayoutID, null);
		}

		TextView title = (TextView) convertView
				.findViewById(R.id.rowcomiclist_item_name);
		TextView unread = (TextView) convertView
				.findViewById(R.id.rowcomiclist_item_unread);

		title.setText(data.get(position).toString());
		String u = data.get(position).getUnreadCount();
		if(Integer.decode(u)>0){
			unread.setText(u);
		} else {
			unread.setText("");
		}
		
		return convertView;
	}
}
