package it.michelepiccirillo.paperplane;



import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ProfileAdapter extends ArrayAdapter<Profile>{

	public ProfileAdapter(Context ctx, List<Profile> profiles) {
		super(ctx, R.layout.list_profile, profiles);
	}
	
	private static class ViewHolder {
		TextView txtName;
		TextView txtDescription;
		ImageView imgPicture;
	}
	
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder h = null;
		
		if(view == null){
			LayoutInflater layoutInflater = ((Activity)getContext()).getLayoutInflater();
			view = layoutInflater.inflate(R.layout.list_profile, null);
			
			h = new ViewHolder();
			h.txtName = (TextView) view.findViewById(R.id.txtName);
			h.txtDescription = (TextView) view.findViewById(R.id.txtDescription);
			h.imgPicture = (ImageView) view.findViewById(R.id.imgPicture);
			
			view.setTag(R.id.tag_viewholder, h);
		} else {
			h = (ViewHolder) view.getTag(R.id.tag_viewholder);
		}
		
		Profile profile = getItem(position);	
		view.setTag(R.id.tag_profile, profile);
		
		h.txtName.setText(profile.getDisplayName());
		h.txtDescription.setText(profile.getEmail());
		
		// FIXME Picture
		
		return view;
		
	}

}
