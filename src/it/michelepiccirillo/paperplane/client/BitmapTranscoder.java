package it.michelepiccirillo.paperplane.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public class BitmapTranscoder implements Transcoder<Bitmap> {
	private static class FlushedInputStream extends FilterInputStream {
	    public FlushedInputStream(InputStream inputStream) {
	        super(inputStream);
	    }

	    @Override
	    public long skip(long n) throws IOException {
	        long totalBytesSkipped = 0L;
	        while (totalBytesSkipped < n) {
	            long bytesSkipped = in.skip(n - totalBytesSkipped);
	            if (bytesSkipped == 0L) {
	                  int b = read();
	                  if (b < 0) {
	                      break;  // we reached EOF
	                  } else {
	                      bytesSkipped = 1; // we read one byte
	                  }
	           }
	            totalBytesSkipped += bytesSkipped;
	        }
	        return totalBytesSkipped;
	    }
	}
	
	private CompressFormat format;
	private int quality;
	
	public BitmapTranscoder(CompressFormat format, int quality) {
		this.format = format;
		this.quality = quality;
	}
	
	@Override
	public Bitmap read(InputStream in) throws Exception {
		Bitmap bmp = BitmapFactory.decodeStream(new FlushedInputStream(in));
		if(bmp == null) 
			throw new RuntimeException("Cannot decode image");
		
		
		return bmp;
	}

	@Override
	public void write(Bitmap object, OutputStream out) throws Exception {
		object.compress(format, quality, out);
	}

	@Override
	public boolean isReadonly() {
		return false;
	}
	
}
