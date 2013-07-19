package it.michelepiccirillo.paperplane;

import java.util.concurrent.Callable;

import android.graphics.Bitmap;
import it.michelepiccirillo.async.ListenableFuture;

public interface Profile {
	String getDisplayName();
	String getDescription();
	String getGooglePlus();
	String getEmail();
	
	Callable<Bitmap> getPicture();
}
