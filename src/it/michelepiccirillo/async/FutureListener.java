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

/**
 * Oggetto che viene notificato nel momento in cui &egrave; terminata una richiesta in background.
 * 
 * Nel momento in cui i dati siano stati correttamente caricati (per esempio dalla rete o da un DB), 
 * viene richiamato il metodo {@link #onSuccess(V)}. In caso di errori, viene notificato 
 * {@link #onError(Throwable)}.
 * 
 * @author Michele Piccirillo <michele.piccirillo@gmail.com>
 *
 * @param <V> Il tipo di dato caricato.
 */
public interface FutureListener<V> {
	
	/**
	 * Metodo richiamato nel momento in cui il task &egrave; stato completato in modo corretto.
	 * 
	 * @param object Il risultato dell'operazione
	 */
	void onSuccess(V object);
	
	/**
	 * Richiamato quando si &egrave verificato un errore nel caricamento dei dati.
	 * 
	 * @param e Eccezione contenente informazioni circa l'errore verificatosi.
	 */
	void onError(Throwable e);
}
