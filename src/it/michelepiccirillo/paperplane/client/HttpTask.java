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


import it.michelepiccirillo.paperplane.async.AsyncQueue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import android.util.Log;

/**
 * Task per una richiesta HTTP con il backend remoto.
 * 
 * Implementa {@link Callable}, il che permette di eseguire la richiesta in background in una
 * {@link AsyncQueue}. 
 * 
 * <p>Le classi che la estendono dovrebbero generalmente implementare i 
 * metodi {@link HttpTask#read(InputStream)} e {@link #write(OutputStream)}, rispettivamente
 * per leggere e scrivere i dati necessari associati alla richiesta HTTP.</p>
 * 
 * <p>Supporto l'uso di una {@link Identity} per impostare automaticamente gli header di 
 * autenticazione (il nome dell'header usato &egrave; definito in {@link Setup#BACKEND_AUTH_HEADER}.</p>
 * 
 * @param <T> Il tipo di dato letto come risultato dell'interrogazione HTTP
 * 
 * @author Michele Piccirillo <michele.piccirillo@gmail.com>
 */
public class HttpTask<T> implements Callable<T> {	
	
	public enum Method { 
		OPTIONS("OPTIONS"),
		GET("GET"),
		HEAD("HEAD"),
		POST("POST", true),
		PUT("PUT", true),
		DELETE("DELETE"),
		TRACE("TRACE"),
		CONNECT("CONNECT");
	
		private final String token;
		private final boolean hasBody;
		
		private Method(String token, boolean hasBody) {
			this.token = token;
			this.hasBody = hasBody;
		}
		
		private Method(String token) {
			this(token, false);
		}
		
		public boolean hasBody() {
			return hasBody;
		}
		
		public String getToken() {
			return token;
		}
		
		@Override
		public String toString() {
			return token;
		}
	}
	
	private final URL url;
	private final Method method;
	
	private Transcoder<T> transcoder;
	private T object;
	
	private final Map<String, String> headers = new HashMap<String, String>();
	
	private int timeout = -1;
	private int chunkSize = HttpClient.DEFAULT_CHUNK_SIZE;
	
	/**
	 * Crea un task con l'identit&agrave; assegnata e un URL parametrizzato.
	 * 
	 * La parametrizzazione dell'URL viene effettuata usando il formato supportato 
	 * da {@link Formatter} e {@link String#format(String, Object...)}.
	 * 
	 * @param identity l'identit&agrave; utilizzata, o {@code null} per effettuare una richiesta non autenticata
	 * @param method il metodo HTTP da utilizzare, es. {@code GET}, {@code POST} o {@code PUT}
	 * @param url un URL parametrizzabile
	 * @param args gli argomenti usati nella parametrizzazione
	 */
	public HttpTask(URL url, Method method, Transcoder<T> transcoder, T object) {
		this.url = url;
		this.method = method;
		this.transcoder = transcoder;
		this.object = object;
	}

	
	/**
	 * Imposta un header HTTP per la richiesta.
	 * 
	 * @param key il nome dell'header
	 * @param value il valore da assegnare
	 */
	public void setHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * Esegue la richiesta HTTP.
	 * 
	 * @throws Exception in caso di errori durante la richiesta
	 * @throws HttpStatusException nel caso in cui la risposta abbia uno status code di tipo 4xx o 5xx
	 * @throws SocketTimeoutException nel caso in cui la richiesta vada in timeout
	 */
	@Override
	public T call() throws Exception {
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) url.openConnection();

			if(timeout != -1) {
				urlConnection.setConnectTimeout(timeout);
			}
			
			urlConnection.setUseCaches(true);
			urlConnection.setRequestMethod(method.toString());
			
			Log.d("HttpTask", method + " " + url);
			
			for(Map.Entry<String, String> e : headers.entrySet())
				urlConnection.setRequestProperty(e.getKey(), e.getValue());
					
			if(method.hasBody() && object != null) {
				urlConnection.setDoOutput(true);
			    urlConnection.setChunkedStreamingMode(chunkSize);
			    
			    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream(), chunkSize);
			    try {
			    	transcoder.write(object, out);
			    } finally {
			    	out.flush();
			    	out.close();
			    }
			    
			}
			
			int status = urlConnection.getResponseCode();
			if(status >= 400)
				throw new HttpStatusException(status, method + " " + url);

		    InputStream in = new BufferedInputStream(urlConnection.getInputStream(), chunkSize);
		    
		    try {
		    	return transcoder.read(in);
		    } finally {
		    	in.close();
		    }
		} finally {
			if(urlConnection != null)
				urlConnection.disconnect();
		}
	}
}
