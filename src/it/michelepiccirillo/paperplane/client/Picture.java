/* AroundMe - Social Network mobile basato sulla geolocalizzazione
 * Copyright (C) 2012 AroundMe Working Group
 *   
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.michelepiccirillo.paperplane.client;


import it.michelepiccirillo.async.*;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

/**
 * Immagine associata ad un oggetto proveniente da Facebook.
 * 
 * Questa classe implementa {@link java.util.concurrent.Callable} e pertanto &egrave; utilizzabile in modo asincrono in associazione
 * con una {@link AsyncQueue} per recuperare dalla rete i dati necessari al suo funzionamento.
 * 
 * <p>La classe &egrave; provvista di una cache interna di dimensioni configurabili ({@link Setup#PICTURE_CACHE_SIZE})
 * per limitare il numero di richieste su rete. Per limitare l'allocazione di memoria, le {@code Picture} con lo stesso ID
 * vengono riutilizzate invece di essere istanziate nuovamente.</p>
 * 
 * @author Michele Piccirillo <michele.piccirillo@gmail.com>
 */
public class Picture implements Callable<Bitmap> {
	public static final int DEFAULT_CACHE_SIZE = 4 * 1024 * 1024;
	
	private static int cacheSize = DEFAULT_CACHE_SIZE;
	
	public static void setCacheSize(int cacheSize) {
		Picture.cacheSize = cacheSize;
		cache = new LruCache<Long, Bitmap>(cacheSize) {
			protected int sizeOf(Long key, Bitmap value) {
				return value.getByteCount();
			};
		};
	}
	
	public static int getCacheSize() {
		return cacheSize;
	}
	
	
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
	
	// Non vogliamo che esistano due istanze con lo stesso id, ma permettiamo al GC di 
	// reclamarle se non sono piu' referenziate altrove
	private static Map<Long, WeakReference<Picture>> instances = new HashMap<Long, WeakReference<Picture>>();
	
	private static LruCache<Long, Bitmap> cache;
	
	static {
		setCacheSize(DEFAULT_CACHE_SIZE);
	}
	
	/**
	 * Restituisce il riferimento all'immagine che Facebook associa all'ID indicato.
	 * 
	 * @param id l'identificativo dell'entit&agrave; Facebook
	 * @return l'immagine associata
	 */
	public static Picture get(long id) {
		WeakReference<Picture> ref = instances.get(id);
		Picture instance = (ref == null) ? null : ref.get();
		
		if(instance == null) {
			instance = new Picture(id);
			instances.put(id, new WeakReference<Picture>(instance));
		}

		return instance;
	}
	
	/**
	 * Svuota la cache delle immagini per liberare memoria.
	 */
	public static void flushCache() {
		cache.evictAll();
	}
	
	private final long id;
		
	private Picture(long id) {
		this.id = id;
	}
	
	/**
	 * Restituisce l'ID dell'entit&agrave; associata a quest'immagine.
	 * 
	 * @return l'ID dell'entita&agrave; associata a quest'immagine
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Restituisce la {@link Bitmap} memorizzata nella cache per quest'immagine, se presente.
	 * 
	 * @return la {@code Bitmap} memorizzata, o {@code null} se non presente nella cache
	 */
	public Bitmap getCachedBitmap() {
		return cache.get(id);
	}
	
	/**
	 * Scarica e decodifica l'immagine dalla rete, se non presente in cache.
	 * 
	 * Essendo questo un metodo bloccante, non andrebbe richiamato nel <em>main thread</em>. Il
	 * modo di utilizzo consigliato &egrave; quello di usare l'intera istanza come task per una
	 * {@link AsyncQueue}.
	 * 
	 * @return l'immagine scaricata dalla rete
	 * @throws Exception in caso di errori di rete, I/O o decodifica
	 * 
	 * @see AsyncQueue#exec(Callable, FutureListener)
	 */
	public Bitmap call() throws Exception {
		Bitmap cachedBmp = getCachedBitmap();
		if(cachedBmp != null) 
			return cachedBmp;

		return null;
		// FIXME
		/*return (new HttpTask<Bitmap>("GET", Setup.PICTURE_URL, id) {

			@Override
			protected Bitmap read(InputStream in) throws Exception {
				Bitmap bmp = BitmapFactory.decodeStream(new FlushedInputStream(in));
				if(bmp == null) 
					throw new RuntimeException("Cannot decode image");
				
				cache.put(id, bmp);
				
				return bmp;
			}
			
		}).call();*/
	}
	
	/**
	 * Aggiorna asincronamente una {@code ImageView} con l'immagine a cui questa istanza fa riferimento.
	 * 
	 * L'utilizzo di questo metodo &egrave; il modo consigliato di aggiornare asincronamente delle {@code View} che 
	 * possono essere soggette a riciclo (ad esempio per l'utilizzo in un {@code Adapter}), in quanto previene automaticamente
	 * problemi di <em>race</em> tra task che potrebbero star operando contemporaneamente sullo stesso widget.
	 * 
	 * @param async l'AsyncQueue usata per eseguire il task in background
	 * @param view il widget da aggiornare
	 * @param defaultRes la risorsa da usare come immagine in attesa che il download termini
	 * @param errorRes la risorsa da usare come immagine nel caso in cui si verifichi un errore durante il download
	 */
	public void asyncUpdate(AsyncQueue async, final ImageView view, int defaultRes, final int errorRes) {
		// FIXME
		/*Long pictureId = (Long) view.getTag(R.id.tag_pictureid);
		if(pictureId != null) {
			if(pictureId.equals(getId())) 
				return;
			
			view.setTag(R.id.tag_pictureid, null);
			view.setImageResource(defaultRes);
			
			Object taskObj = view.getTag(R.id.tag_task);
			if(taskObj != null && taskObj instanceof ListenableFuture<?>) {
				((ListenableFuture<?>) taskObj).cancel(false);
				view.setTag(R.id.tag_task, null);
			}
		}
		
		ListenableFuture<Bitmap> task = async.exec(this);
		
		view.setTag(R.id.tag_pictureid, getId());
		view.setTag(R.id.tag_task, task);
		
		task.setListener(new FutureListener<Bitmap>() {
			@Override
			public void onSuccess(Bitmap object) {
				view.setImageBitmap(object);
				view.setTag(R.id.tag_task, null);
			}

			@Override
			public void onError(Throwable e) {
				view.setImageResource(errorRes);
				view.setTag(R.id.tag_task, null);
				
			}
		});*/
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == null || !(o instanceof Picture))
			return false;
		
		return ((Picture) o).getId() == getId();
	}
}
