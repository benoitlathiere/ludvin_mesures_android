package com.example;

import java.util.Calendar;
import java.util.Date;

public class Borne {

	private String nom;
	private String mac;
	private Double frequence;
	private int signal;
	private Date date;
	private Calendar calendrier;

	public Borne(String nom, String mac, Double frequence, int signal) {
		calendrier = Calendar.getInstance();
		this.nom = nom;
		this.mac=mac;
		this.frequence=frequence;	//Fréquence en GHz
		this.signal=signal;			//en dBm
		this.date=calendrier.getTime();
	}

	
	/**
	 * Retourne le nom (SSIF) de la borne.
	 * @return Nom de la borne
	 */
	public String getNom() {
		return this.nom;
	}
	

	/**
	 * Retourne la force du signal de la borne.
	 * @return Force du signal en dBm
	 */
	public int getSignal() {
		return this.signal;
	}
	
	
	/**
	 * Retourne l'adresse MAC de la borne.
	 * @retun	Adresse MAC
	 */
	public String getMac() {
		return this.mac;
	}
	
	
	/**
	 * Retourne la fréquence d'emission de la borne.
	 * @retun	Fréquence en GHz
	 */
	public Double getFrequence() {
		return this.frequence;
	}	

	
	/**
	 * Retourne la date/heure où le signal a été enregistré.
	 * @retun	Date/heure
	 */
	public String getDate() {
		return this.date.toLocaleString();
	}	
	

	
	/**
	 * Retourne la date/heure au format ISO où le signal a été enregistré.
	 * @retun	Date/heure au format YYYY-MM-DD HH:MM:SS
	 */
	public String getDateISO() {
		return this.date.getYear()+"-"+this.date.getMonth()+"-"+this.date.getDay()+" "+this.date.getHours()+":"+this.date.getMinutes()+":"+this.date.getSeconds();
	}	
	
	
	/**
	 * Retourne un texte contenant les éléments pertinents de la borne.
	 * @return Texte formaté des éléments
	 */
	public String toString() {
		return this.nom+" ("+this.mac+")"+this.signal+" dBm (à "+this.date+")";
	}

}
