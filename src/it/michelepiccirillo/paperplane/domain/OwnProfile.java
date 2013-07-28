package it.michelepiccirillo.paperplane.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.zip.CRC32;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class OwnProfile extends NetworkProfile implements Parcelable, Externalizable {
	private Bitmap picture;

	public OwnProfile() {
		// TODO Auto-generated constructor stub
	}

	public void setDisplayName(String name) {
		this.displayName = name;
	}

	public void setDescription(String bio) {
		this.description = bio;
	}

	public void setGooglePlus(String googlePlus) {
		this.googlePlus = googlePlus;
	}

	public void setEmail(String email) {
		this.email = email;
	}


	public void setPictureBitmap(Bitmap bitmap) {
		this.picture = bitmap;
	}

	public Bitmap getPictureBitmap() {
		return picture;
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
		parcel.writeString(getDisplayName());
		parcel.writeString(getDescription());
		parcel.writeString(getGooglePlus());
		parcel.writeString(getEmail());
		parcel.writeParcelable(picture, 0);
	}
	
	public static final Creator<OwnProfile> CREATOR = new Creator<OwnProfile>() {

		@Override
		public OwnProfile createFromParcel(Parcel source) {
			OwnProfile p = new OwnProfile();
			p.setDisplayName(source.readString());
			p.setDescription(source.readString());
			p.setGooglePlus(source.readString());
			p.setEmail(source.readString());
			p.setPictureBitmap((Bitmap) source.readParcelable(null));
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
		setDescription(input.readUTF());
		setGooglePlus(input.readUTF());
		setEmail(input.readUTF());
		
		int size = input.readInt();
		byte[] data = new byte[size];
		
		final int CHUNK = 512;
		for(int i = 0; i < size; i += CHUNK) {
			input.read(data, i, Math.min(CHUNK, size - i));
		}

		setPictureBitmap(BitmapFactory.decodeByteArray(data, 0, size));
	}

	@Override
	public void writeExternal(ObjectOutput output) throws IOException {
		output.writeUTF(getDisplayName());
		output.writeUTF(getDescription());
		output.writeUTF(getGooglePlus());
		output.writeUTF(getEmail());
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		picture.compress(CompressFormat.PNG, 100, out);

		byte[] data = out.toByteArray();
		int size = data.length;

		output.writeInt(size);
		
		final int CHUNK = 512;
		for(int i = 0; i < size; i += CHUNK) {
			output.write(data, i, Math.min(CHUNK, size - i));
			output.flush();
		}
	}
}
