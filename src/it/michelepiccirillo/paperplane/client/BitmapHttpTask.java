package it.michelepiccirillo.paperplane.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapHttpTask extends HttpTask<Bitmap>{
	/** 
	 * Workaround per vecchie versioni di BitmapFactory.
	 *  
	 * <blockquote>
	 * <p>A bug in the previous versions of {@link BitmapFactory#decodeStream(InputStream)} may prevent the code 
	 * from working over a slow connection. Decode a {@code new FlushedInputStream(inputStream)} instead to fix 
	 * the problem.</p>
	 * 
	 * <p>This ensures that skip() actually skips the provided number of bytes, unless we reach the end of file.</p>
	 * </blockquote>
	 * 
	 * @see http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html
	 */
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

	public BitmapHttpTask(URL url) {
		super(url, Method.GET, new Transcoder<Bitmap>() {

			@Override
			public Bitmap read(InputStream in) throws Exception {
				Bitmap bmp = BitmapFactory.decodeStream(new FlushedInputStream(in));
				if(bmp == null) 
					throw new RuntimeException("Cannot decode image");
				
				
				return bmp;
			}

			@Override
			public void write(Bitmap object, OutputStream out) throws Exception {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean isReadonly() {
				return true;
			}
			
		}, null);
	}

}
