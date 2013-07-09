package it.michelepiccirillo.paperplane.client;

import it.michelepiccirillo.paperplane.client.HttpTask.Method;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpClient {
	public static final int DEFAULT_TIMEOUT = 30 * 1000; // 30s
	public static final int DEFAULT_CHUNK_SIZE = 1024; // 1 KiB
	
	private InetSocketAddress host;
	private int timeout = DEFAULT_TIMEOUT;
	private int chunkSize = DEFAULT_CHUNK_SIZE;
	
	
	public HttpClient(InetSocketAddress host) {
		this.host = host;
	}
	
	protected <T> HttpTask<T> create(String path, Method method, Transcoder<T> transcoder, T object) {
		try {
			URL url = new URL("http", host.getHostName(), host.getPort(), path);
			HttpTask<T> task = new HttpTask<T>(url, method, transcoder, object);
			
			task.setTimeout(timeout);
			task.setChunkSize(chunkSize);
			
			return task;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public <T> HttpTask<T> get(String path, Transcoder<T> transcoder) {
		return create(path, Method.GET, transcoder, null);
	}
	
	public <T> HttpTask<T> post(String path, Transcoder<T> transcoder, T object) {
		return create(path, Method.POST, transcoder, object);
	}
	
	public <T> HttpTask<T> put(String path, Transcoder<T> transcoder, T object) {
		return create(path, Method.PUT, transcoder, object);
	}
	
	public <T> HttpTask<T> delete(String path, Transcoder<T> transcoder) {
		return create(path, Method.DELETE, transcoder, null);
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	
}
