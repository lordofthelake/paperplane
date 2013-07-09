package it.michelepiccirillo.paperplane.client;

import java.io.InputStream;
import java.io.OutputStream;

public interface Transcoder<T> {
	/**
	 * In caso di risposta favorevole (codice 2xx) legge il contenuto inviato dal server
	 * e lo converte in un oggetto opportuno.
	 * 
	 * @param in lo stream da cui leggere i contenuti della risposta
	 * @return l'oggetto risultato della conversione
	 * @throws Exception in caso di errori durante la lettura o la conversione
	 */
	T read(InputStream in) throws Exception;
	
	/**
	 * Per metodi che richiedono l'invio di dati (es. {@code POST} o {@code PUT}), scrive i dati
	 * necessari sull'OutputStream associato alla richiesta.
	 * 
	 * L'implementazione di default non fa niente, pertanto le sottoclassi ne dovrebbero
	 * fare l'override nel caso in cui volessero fare l'invio effettivo di dati.
	 * 
	 * @param out lo stream su cui scrivere i dati
	 * @throws Exception in caso di errori durante la scrittura
	 */
	void write(T object, OutputStream out) throws Exception;
	
	boolean isReadonly();
}
