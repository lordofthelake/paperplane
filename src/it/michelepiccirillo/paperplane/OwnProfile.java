package it.michelepiccirillo.paperplane;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.Callable;

import com.google.gson.annotations.Expose;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class OwnProfile implements Parcelable, Profile, Externalizable {
	
	private Bitmap picture;
	
	@Expose private String displayName;
	@Expose private String bio;
	@Expose private String email;
	@Expose private String googlePlus;

	public OwnProfile() {
		// TODO Auto-generated constructor stub
	}

	public void setDisplayName(String name) {
		this.displayName = name;
	}
	@Override
	public String getDisplayName() {
		return displayName;
	}
	
	public void setBio(String bio) {
		this.bio = bio;
	}

	@Override
	public String getDescription() {
		return bio;
	}
	
	public void setGooglePlus(String googlePlus) {
		this.googlePlus = googlePlus;
	}

	@Override
	public String getGooglePlus() {
		return googlePlus;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String getEmail() {
		return email;
	}
	
	public void setPicture(Bitmap bitmap) {
		this.picture = bitmap;
	}

	@Override
	public Callable<Bitmap> getPicture() {
		return new Callable<Bitmap>() {

			@Override
			public Bitmap call() throws Exception {
				return picture;
			}
			
		};
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int arg1) {
		parcel.writeString(displayName);
		parcel.writeString(bio);
		parcel.writeString(googlePlus);
		parcel.writeString(email);
		parcel.writeParcelable(picture, 0);
	}
	
	public static final Creator<OwnProfile> CREATOR = new Creator<OwnProfile>() {

		@Override
		public OwnProfile createFromParcel(Parcel source) {
			OwnProfile p = new OwnProfile();
			p.setDisplayName(source.readString());
			p.setBio(source.readString());
			p.setGooglePlus(source.readString());
			p.setEmail(source.readString());
			p.setPicture((Bitmap) source.readParcelable(null));
			return p;
		}

		@Override
		public OwnProfile[] newArray(int size) {
			return new OwnProfile[size];
		}
		
	};

	@Override
	public void readExternal(ObjectInput input) throws IOException,
			ClassNotFoundException {
		setDisplayName(input.readUTF());
		setBio(input.readUTF());
		setGooglePlus(input.readUTF());
		setEmail(input.readUTF());
		
		int length = input.readInt();
		Log.d("OwnProfile", "length: " + length);
		byte[] data = new byte[length];
		input.read(data, 0, length);
		
		setPicture(BitmapFactory.decodeByteArray(data, 0, length));
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeUTF(getDisplayName());
		output.writeUTF(getDescription());
		output.writeUTF(getGooglePlus());
		output.writeUTF(getEmail());
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		picture.compress(CompressFormat.PNG, 0, out);
		
		byte[] data = out.toByteArray();
		output.writeInt(data.length);
		output.write(data);
	}

}
