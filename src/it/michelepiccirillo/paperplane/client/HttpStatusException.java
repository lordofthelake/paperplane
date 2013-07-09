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

/**
 * Eccezione sollevata nel caso in cui una richiesta HTTP si completi con uno Status Code
 * di tipo 4xx o 5xx.
 * 
 * @author Michele Piccirillo <michele.piccirillo@gmail.com>
 */
public class HttpStatusException extends Exception {
	private static final long serialVersionUID = 1L;
	
	private int code;
	
	/**
	 * Crea una nuova eccezione con il codice HTTP e il messaggio indicato.
	 * 
	 * @param code lo status code restituito dall'interazione remota
	 * @param message un messaggio esplicativo dell'errore
	 */
	public HttpStatusException(int code, String message) {
		super("[" + code + "]" + message);
		this.code = code;
	}
	
	/**
	 * Crea una nuova eccezione con il codice HTTP indicato.
	 *  
	 * @param code lo status code restituito dall'interazione remota
	 */
	public HttpStatusException(int code) {
		super("[" + code + "]");
		this.code = code;
	}
	
	/**
	 * Restituisce lo status code associato a questa eccezione.
	 * 
	 * @return lo status code HTTP
	 */
	public int getStatusCode() {
		return code;
	}
}
