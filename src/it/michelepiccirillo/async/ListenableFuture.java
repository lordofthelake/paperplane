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
package it.michelepiccirillo.async;


import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Un'operazione asincrona interrompibile e dei cui risultati pu&ograve; essere notificato un 
 * {@link FutureListener}.
 * 
 * Le chiamate ai metodi del listener vengono effettuate di default nel <em>main thread</em> 
 * dell'applicazione, anche noto come l'<em>UI Thread</em>. Un {@link Looper} diverso  in cui
 * eseguire i metodi del listener pu&ograve; essere specificato in fase di creazione.
 *
 * @param <V> Il tipo di dato risultante dall'esecuzione
 * 
 * @see java.util.concurrent.Future
 * @see FutureListener
 * @see AsyncQueue
 * 
 * @author Michele Piccirillo <michele.piccirillo@gmail.com>
 */
public class ListenableFuture<V> extends FutureTask<V> {
	private FutureListener<V> listener = null;
	private final Handler handler;
	private final Callable<V> callable; // For debug purposes 
	
	/**
	 * Crea un nuovo {@code ListenableFuture} con il listener assegnato, i cui metodi verranno
	 * chiamati nel thread associato al Looper specificato.
	 * 
	 * @param callable il task da eseguire in background
	 * @param listener il FutureListener che verr&agrave; notificato al termine dell'esecuzione
	 * @param looper il Looper associato al thread in cui verr&agrave; notificato il listener
	 */
	public ListenableFuture(Callable<V> callable, FutureListener<V> listener, Looper looper) {
		super(callable);
		this.callable = callable;
		this.listener = listener;
		this.handler = new Handler(looper);
	}
	
	/**
	 * Crea un nuovo {@code ListenableFuture} con il listener assegnato, i cui metodi verranno
	 * chiamati nell'UI Thread.
	 * 
	 * @param callable il task da eseguire in background
	 * @param listener il FutureListener che verr&agrave; notificato al termine dell'esecuzione
	 */
	public ListenableFuture(Callable<V> callable, FutureListener<V> listener) {
		this(callable, listener, Looper.getMainLooper());
	}
	
	/**
	 * Crea un nuovo {@code ListenableFuture} senza alcun listener assegnato.
	 * 
	 * @param callable il task da eseguire in background
	 */
	public ListenableFuture(Callable<V> callable) {
		this(callable, null, null);
	}
	
	/**
	 * Imposta il FutureListener che verr&agrave; notificato al termine dell'esecuzione.
	 * 
	 * @param listener il listener da associare a questa operazione
	 */
	public synchronized void setListener(FutureListener<V> listener) {
		this.listener = listener;
		if(isDone()) notifyListener();
	}
	
	/**
	 * Restituisce il listener associato a questa operazione.
	 * 
	 * @return il listener associato, o {@code null} se non presente
	 */
	public FutureListener<V> getListener() {
		return listener;
	}
	
	/**
	 * Notifica il listener, se esistente, della conclusione dell'operazione
	 */
	private void notifyListener() {
		if(listener != null && !isCancelled()) {
			try {
				try {
					final V result = get();
					handler.post(new Runnable() {
						@Override
						public void run() {
							listener.onSuccess(result);
						}
					});
				} catch (ExecutionException xEx) {
					throw xEx.getCause();
				}
			} catch(InterruptedException iEx) {
				Thread.currentThread().interrupt();
				Log.d("ListenableFuture", "Interrupted task " + callable);
				
			} catch(final Throwable e) {

				handler.post(new Runnable() {
					@Override
					public void run() {
						listener.onError(e);
					}
				});
			}
		}
	}
	
	@Override
	protected synchronized void done() {
		notifyListener();
	}
}
