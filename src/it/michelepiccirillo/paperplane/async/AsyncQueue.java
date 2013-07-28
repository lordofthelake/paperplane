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
package it.michelepiccirillo.paperplane.async;


import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.os.AsyncTask;
import android.os.Looper;


/**
 * Classe che permette l'esecuzione in background di uno o pi&ugrave; task contemporaneamente, permettendo la notifica asincrona
 * dell'esito dell'esecuzione ad un listener, i cui metodi sono eseguiti nel thread principale dell'applicazione (anche conosciuto 
 * come <em>main thread</em> o <em>UI thread</em>).
 * 
 * <p>I task sono implementazioni dell'interfaccia {@link java.util.concurrent.Callable} e vengono eseguiti da un pool di thread.
 * Il numero massimo di thread che possono essere attivi contemporaneamente &egrave; fissato in fase di creazione. I task vengono
 * messi in coda finch&eacute; non c'&egrave; un thread libero che possa eseguire il task. Quando i thread sono inutilizzati per un
 * tempo superiore a quello configurato (tempo di Keep-Alive) essi vengono terminati automaticamente per liberare risorse, qualora
 * la piattaforma lo supporti (API level 9 e superiori).</p>
 * 
 * <p>&Egrave; necessario richiamare il metodo {@link AsyncQueue#shutdown()} al termine dell'utilizzo per fermare tutti i thread in
 * esecuzione e liberare risorse. L'esecuzione dei task in coda pu&ograve; essere messa in pausa tramite il metodo {@link #pause()} e 
 * riavviata tramite {@link #resume()}.</p>
 * 
 * <p>Rispetto all'uso di {@link AsyncTask}, che &egrave; la soluzione "standard" per la piattaforma Android per eseguire task in 
 * background che hanno effetti sulla UI, questa implementazione fornisce un controllo migliore sull'utilizzo delle risorse: oltre
 * alla possibilit&agrave; di mettere in pausa e riprendere l'esecuzione dei task (tipicamente in corrispondenza agli eventi {@code onPause()}
 * e {@code onResume()} delle Activity), il controllo sul numero di thread contemporanei ha un'importante impatto sulla performance:
 * laddove con l'approccio tradizionale della piattaforma il numero di thread &egrave; fissato ad uno (singolo {@code AsyncTask} che esegue una coda 
 * di lavori) o incontrollato (creazione di tanti {@code AsyncTask} quanti sono i lavori in background ad eseguire), l'AsyncQueue riesce
 * a rispondere in modo elastico alla domanda. Il riciclo dei thread gi&agrave; esistenti per eseguire nuovi task e la politica di 
 * Keep-Alive contribuiscono inoltre ad abbassare il costo di avvio di un nuovo task.</p>
 * 
 * <p>Un esempio tipico, che ha portato allo sviluppo di questa soluzione, &egrave; la visualizzazione contemporanea sul display di molte 
 * icone che devono essere scaricate asincronamente dalla rete: l'approccio tradizionale comporterebbe o una visualizzazione lenta 
 * (download in coda) o un'esplosione di thread che contemporaneamente scaricano e decodificano tutte le immagini, con un impatto non 
 * negligibile sull'utilizzo delle risorse di networking e computazionali.</p>
 * 
 * @see ListenableFuture
 * @see http://developer.android.com/resources/articles/painless-threading.html
 * @author Michele Piccirillo <michele.piccirillo@gmail.com>
 */
public class AsyncQueue {
	
	/**
	 * Executor con un pool di thread di dimensioni massime fissate e possibilit&agrave; di mettere in 
	 * pausa i task.
	 * 
	 * Per diminuire il costo di creazione di nuovi thread, se sono liberi da task, i vecchi vengono
	 * riciclati. Thread senza task in esecuzione vengono mantenuti attivi per un certo tempo configurabile (Keep-Alive)
	 * prima di essere terminati (dove la piattaforma lo supporti).
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor
	 */
	protected static class PausableExecutor extends ThreadPoolExecutor {
		private boolean isPaused;
		private ReentrantLock pauseLock = new ReentrantLock();
		private Condition unpaused = pauseLock.newCondition();

		public PausableExecutor(int poolSize, int keepAlive) { 
			super(poolSize, poolSize, keepAlive, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()); 
			if(keepAlive > 0) {
				try {
					// Disponibile da API >= 9
					getClass().getMethod("allowCoreThreadTimeOut").invoke(this, true);
				} catch (Exception e) {}
			}
			
			// XXX La priorita' dei thread potrebbe essere abbassata per lasciare piu' risorse alla UI
		}

		protected void beforeExecute(Thread t, Runnable r) {
			super.beforeExecute(t, r);
			pauseLock.lock();
			try {
				while (isPaused) unpaused.await();
			} catch (InterruptedException ie) {
				t.interrupt();
			} finally {
				pauseLock.unlock();
			}
		}

		public void pause() {
			pauseLock.lock();
			try {
				isPaused = true;
			} finally {
				pauseLock.unlock();
			}
		}

		public void resume() {
			pauseLock.lock();
			try {
				isPaused = false;
				unpaused.signalAll();
			} finally {
				pauseLock.unlock();
			}
		}
	}

	private PausableExecutor pool;
	private Looper looper;
	
	/**
	 * Crea una nuova istanza con il numero massimo di thread e il tempo di Keep-Alive indicati.
	 * 
	 * &Egrave; possibile utilizzare il Keep-Alive solo per le API 9 o superiore. Per versioni precedenti, il 
	 * parametro viene ignorato e sar&agrave; comunque necessaria una chiamata di {@link #shutdown()} per terminare i thread
	 * avviati.
	 * 
	 * @param poolSize il numero massimo di thread che possono essere attivi contemporaneamente
	 * @param keepAlive numero di secondi dopo il quale i thread inutilizzati saranno terminati automaticamente
	 */
	public AsyncQueue(int poolSize, int keepAlive) {
		this.pool = new PausableExecutor(poolSize, keepAlive);
		this.looper = Looper.myLooper();
	}
	
	/**
	 * Crea una nuova istanza con il numero massimo di thread indicato.
	 * 
	 * I thread, una volta avviati, non vengono terminati fino alla chiamata di {@link #shutdown()}.
	 * @param poolSize il numero massimo di thread che possono essere attivi contemporaneamente
	 */
	public AsyncQueue(int poolSize) {
		this(poolSize, 0);
	}
	
	/**
	 * Crea una nuova istanza con al massimo un solo thread in esecuzione.
	 * 
	 * I nuovi task vengono accodati finch&eacute; il thread non si libera da quello precedente, creando una coda batch.
	 * Per terminare il thread &egrave; necessario richiamare {@link #shutdown()}.
	 */
	public AsyncQueue() {
		this(1, 0);
	}
	
	/**
	 * Accoda un task da eseguire.
	 * 
	 * @param task il task da eseguire in background
	 */
	public <V> void exec(ListenableFuture<V> task) {
		pool.execute(task);
	}
	
	/**
	 * Accoda un nuovo task da eseguire, senza un listener.
	 * 
	 * @param action il task da eseguire in background
	 * @return un {@link ListenableFuture} associato al task, che ne permette il controllo
	 */
	public <V> ListenableFuture<V> exec(Callable<V> action) {
		return exec(action, null);
	}
	
	/**
	 * Accoda un nuovo task da eseguire, con il listener associato.
	 * 
	 * @param action il task da eseguire in background
	 * @param listener il listener che verr&agrave; notificato degli esiti dell'esecuzione
	 * @return un {@link ListenableFuture} associato al task, che ne permette il controllo
	 */
	public <V> ListenableFuture<V> exec(final Callable<V> action, FutureListener<V> listener) {
		ListenableFuture<V> task = new ListenableFuture<V>(action, listener, looper);
		this.exec(task);
		return task;
	}
	
	/**
	 * Mette in pausa l'esecuzione di nuovi task.
	 * 
	 * Mentre i task gi&agrave; in esecuzione non saranno interrotti, non ne verranno avviati altri fino alla chiamata di {@see #resume()}.
	 */
	public void pause() {
		pool.pause();
	}
	
	/**
	 * Riavvia l'esecuzione di nuovi task.
	 * 
	 * Tipicamente utilizzato in seguito ad una chiamata a {@link #pause()}
	 */
	public void resume() {
		pool.resume();
	}
	
	/**
	 * Termina tutti i thread attualmente in esecuzione.
	 * 
	 * Nel caso in cui ci fossero dei task ancora in esecuzione, verr&agrave; tentato di interromperli.
	 */
	public void shutdown() {
		pool.shutdownNow();
	}
}
