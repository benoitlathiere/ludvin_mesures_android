package com.example;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


public class WiFiDemo extends Activity implements OnClickListener {
	private static final String TAG = "WiFiDemo";	//pour Log.d()
	WifiManager wifi;
	BroadcastReceiver receiver;

	//éléments UI
	TextView textSortie;
	TextView textMesures;
	Button buttonScan;
	Button buttonScanNew;	//BL scan bornes alentours
	Button buttonVideCache;
	Button btnVoirMesures;
	Button btnMesuresCSV;		//export vers fichier formaté
	CheckBox CBautoscan;
	TextView TxtNomRepere;

	TableLayout Tableau;
	
	Calendar c;

	//timer
	TimerTask scanTask;
	final Handler handler = new Handler();
	Timer t = new Timer();
	int secondes = 3;
	
	//divers
	DecimalFormat f = new DecimalFormat();
	
	//cache des valeurs
	ArrayList<Borne> Bornes = new ArrayList<Borne>();
	private Hashtable<String,ArrayList<Integer>> statsbornesuniques = new Hashtable<String,ArrayList<Integer>>();;	//tableau associatif des bornes listées [nom],[signaux[]]
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceMesuree) {
		super.onCreate(savedInstanceMesuree);
		setContentView(R.layout.main);

		Log.d(TAG,"----------");
		
		// Setup UI
		textSortie = (TextView) findViewById(R.id.textSortie);
		textMesures = (TextView) findViewById(R.id.textMesures);
		buttonScan = (Button) findViewById(R.id.buttonScan);
		buttonScan.setOnClickListener(this);

		//ajouts BL :
		buttonScanNew = (Button) findViewById(R.id.buttonScanNew);
		buttonScanNew.setOnClickListener(this);
		btnVoirMesures = (Button) findViewById(R.id.btnVoirMesures);
		btnVoirMesures.setOnClickListener(this);
		buttonVideCache = (Button) findViewById(R.id.buttonVideCache);
		buttonVideCache.setOnClickListener(this);
		btnMesuresCSV = (Button) findViewById(R.id.btnMesuresCSV);
		btnMesuresCSV.setOnClickListener(this);
		TxtNomRepere = (TextView) findViewById(R.id.TxtNomRepere);
		Tableau = (TableLayout) findViewById(R.id.Tableau);
		
		
		CBautoscan = (CheckBox) findViewById(R.id.CBautoscan);
		CBautoscan.setOnClickListener(this);
		CBautoscan.setText("Scan auto des bornes toutes les "+secondes+"s.");
		
		//afichage décimales
		f.setMaximumFractionDigits(1);
		
		// Setup WiFi
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifi.isWifiEnabled()==false)
			{Toast toast = Toast.makeText(this, "Activation WIFI", Toast.LENGTH_SHORT);toast.show();} 
		wifi.setWifiEnabled(true);
		wifi.startScan();

		// Get WiFi status
		WifiInfo info = wifi.getConnectionInfo();
		textSortie.append("\n\nWiFi Mesures: " + info.toString()+"\nNiveau borne courante : "+info.getRssi()+" dBm");
		// List available networks
		List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
		for (WifiConfiguration config : configs) {
			textSortie.append("\n\n" + config.toString());
		}		
		
		// Register Broadcast Receiver
		if (receiver == null)
			receiver = new WiFiScanReceiver(this);
		registerReceiver(receiver, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		Log.d(TAG, "onCreate()");
	}



	public void onClick(View view) {
		//bornes déjà configurées
		if (view.getId() == R.id.buttonScan) {	
			//Log.d(TAG, "onClick() wifi.startScan()");	//debug
			WifiInfo info = wifi.getConnectionInfo();
			wifi.startScan();
			textSortie.append("\n\nWiFi Mesureus: " + info.toString()+"\nNiveau borne courante : "+info.getRssi()+" dBm\n\n");
			// List available networks
			List<WifiConfiguration> configs = wifi.getConfiguredNetworks();
			for (WifiConfiguration config : configs) {
				textSortie.append("\n\n" + config.toString());
			}	
		}
		
		//BL - nouveaux réseaux scannés
		else if (view.getId() == R.id.buttonScanNew) {
			//Log.d(TAG, "onClick() Bornes alentours");	//debug
			scanner_alentours();
		}
		
		//case à cocher Autoscan
		else if (view.getId() == R.id.CBautoscan) {
			autoscan();
		}
		
		//voir stats
		else if (view.getId() == R.id.btnVoirMesures) {
			voirMesures();
		}
		
		//vider cache
		else if (view.getId() == R.id.buttonVideCache) {
			Bornes.clear();
			textSortie.setText("- pas de mesure -");
			Toast toast = Toast.makeText(this, "Mesures vidées", Toast.LENGTH_SHORT);toast.show();	
		}
		
		//export mesures vers fichier CSV
		else if (view.getId() == R.id.btnMesuresCSV){
			exportCSV();
		}
	}

	
	
	/**
	 * Scanne les bornes Wifi alentours
	 */
	private void scanner_alentours() {
		//Toast toast = Toast.makeText(this, "Nouveau scan des bornes alentours", Toast.LENGTH_SHORT);	toast.show();
		Log.d(TAG, "Scan des bornes alentours");
		c = Calendar.getInstance();
		Date heure = c.getTime();
		wifi.startScan();
		List<ScanResult> scannes = wifi.getScanResults();	//BL
		textSortie.setText("Scan de bornes à "+heure.getHours()+":"+heure.getMinutes()+":"+heure.getSeconds()+"\nNombre de bornes alentours : "+String.valueOf(scannes.size()));
		for (ScanResult scanne : scannes) {
			textSortie.append("\n\nNom  : "+scanne.SSID);
			textSortie.append("\nMAC  : "+scanne.BSSID);
			textSortie.append("\nFréq  : "+convertFreq(scanne.frequency)+" MHz");
			textSortie.append("\nForce  : "+scanne.level+" dBm");
			Bornes.add( new Borne(scanne.SSID, scanne.BSSID, convertFreq(scanne.frequency), scanne.level) );
		}
		textSortie.append("\r\n\r\nINFO : "+Bornes.size()+" mesures dans le cache.");
	}




	/**
	 * Change l'état du scan automatique des bornes Wi-Fi alentours selon la case à cocher
	 */
	private void autoscan() {
		boolean actif=CBautoscan.isChecked();
		String msg="";
		if (actif) {
			scanTask = new TimerTask() {
		        public void run() {
		                handler.post(new Runnable() {
		                        public void run() {
		                        	scanner_alentours();
		                        	Log.d("TIMER", "Timer set off");
		                        }
		               });
		        }};
		    t.schedule(scanTask, 1000, (secondes*1000));
		    msg="activé";
		} else {
			scanTask.cancel();
			msg="désactivé";
		}	
		Toast toast = Toast.makeText(this, "AUTOSCAN toutes les "+secondes+" secondes "+msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	
	
	/**
	 * Convertie une fréquence MHz en GHz
	 * @param freq	Fréquence en MHz
	 * @return Fréquence en GHz avec décimales
	 */
	@SuppressLint("DefaultLocale")
	private double convertFreq(int freq) {
		return (double) (freq/1000.0);
	}
	
	
	/**
	 * Affiche les stats courantes (min, max, moy) de chaque borne
	 */
	private void voirMesures() {
		//1) on parcourt Bornes
		//2) on compulse les infos de chaque borne
		//3) puis on parcourt les éléments de chaque borne pour afficher stats
		Log.d(TAG,"voirMesures()");
		String texte="";
		
		if (Bornes.size()>0) {
			this.statsbornesuniques.clear();
			Borne bornetmp=null;	//borne temporaire durant le parcours
			
			//on parcourt chaque ligne du log "Bornes" pour agréger les signaux de chaque borne
			for (int a=0; a<Bornes.size(); a++) {
				bornetmp = Bornes.get(a); //on rŽcupre la borne
				Log.d(TAG, "borne " + bornetmp.getNom() + " MAC " + bornetmp.getMac());
				if (this.statsbornesuniques.get(bornetmp.getMac()) == null) {
					//Log.d(TAG, "nouvelle borne");
					this.statsbornesuniques.put(bornetmp.getMac(), new ArrayList<Integer>());
				} else {
					//Log.d(TAG, "borne déjà présente dans la liste");
					//Log.d(TAG, "nb de signaux associés : " + this.statsbornesuniques.get(bornetmp.getMac()).size());
				}
				this.statsbornesuniques.get(bornetmp.getMac()).add(bornetmp.getSignal());
				//Log.d(TAG, "nb de signaux associés : " + this.statsbornesuniques.get(bornetmp.getMac()).size());
			}
			
			Log.d(TAG,"fin première boucle --");
			//textMesures.setText("STATS : \r\n");
			texte = "STATS : \r\n";
			//for (int a=0 ; a<statsbornesuniques.size() ; a++) {
			Enumeration<String> e = statsbornesuniques.keys();
			ArrayList<Integer> maborne = new ArrayList<Integer>(); 
			Log.d(TAG,"while");
			String MAC="";
			ArrayList<Integer> tmptab = new ArrayList<Integer>(); 
			int sum=0;
			int min=0;
			int max=0;
			double avg=0;
			
			while (e.hasMoreElements()) {
				MAC = e.nextElement().toString();
				Log.d(TAG,"-----dans boucle----"+MAC);
				texte += MAC ;	//+ "  "+statsbornesuniques.get( MAC ).toString() + " "  ;
				int a=0;	//indice tmp
				tmptab = statsbornesuniques.get( MAC );
				for (a=0 ; a<tmptab.size() ; a++) {
					sum += tmptab.get(a);
					if (a==0) {
						min = tmptab.get(a);
						max = tmptab.get(a);
					}
					if (tmptab.get(a)>max)
						max = tmptab.get(a);
					if (tmptab.get(a)<min);
						min=tmptab.get(a);
				}
				avg = (sum/(a+1.0)) ;
				
				texte += " min: "+min+" / moy: "+ this.f.format(avg) +" / max: "+max+" ("+(a+1)+" mesures)\r\n";
			}//fin while
			
			textSortie.setText(texte);

			/* tableaux
			TableRow ligne = new TableRow(this);
			Tableau.addView(ligne);
			*/
			
		} else {
			textSortie.setText("les stats sont vides !?");
		}
	}
	
	
	
	/**
	 * Export des stats collectées vers un fichier CSV
	 * @return	True/False Etat de l'export vers le fichier
	 */
	public boolean exportCSV() {
		//nom et chemin du fichier
		String FichierExport = "sdcard/WiFiDemo-repere"+"-"+TxtNomRepere.getText()+".csv";
		//nom de l'utilisateur du mobile
		AccountManager accountManager = AccountManager.get(this);
		Account[] accounts = accountManager.getAccountsByType("com.google");
		String utilisateur = accounts[0].name;
		//on boucle sur toutes les lignes enregistrées
		if (Bornes.size()>0) {
			String texte = null;
			Borne borne = null;
			texte = "MAC;Nom;Fréquence;Signal;Date;Mobile;Utilisateur;Repère\r\n";
			for (int a=0; a < Bornes.size(); a++) {
				borne = Bornes.get(a);
				texte += borne.getMac()+";"+borne.getNom()+";"+borne.getFrequence()+";"+borne.getSignal()+";"+borne.getDateISO()+";"+getDeviceName()+";"+utilisateur+";"+TxtNomRepere.getText()+"\r\n";
			}
			Log.d(TAG,texte);
			if (EcrireFichierTexte(texte, FichierExport)) {
				Toast toast = Toast.makeText(this, "Les mesures ont été exportées vers "+FichierExport, Toast.LENGTH_SHORT);
				toast.show();
				return true;
			} else {
				Toast toast = Toast.makeText(this, "ERREUR : Les mesures n'ont pas été exportées", Toast.LENGTH_LONG);toast.show();
				return false;
			}
		} else {
			Toast toast = Toast.makeText(this, "Pas de stat à exporter !", Toast.LENGTH_LONG);toast.show();
			return false;
		}
	}
	
	
	
	
	
	/**
	 * Ecrire un fichier texte sur la SDCard
	 * source : http://stackoverflow.com/questions/1756296/android-writing-logs-to-text-file
	 * @param text Texte à écrire dans le fichier
	 * @param fichier Chemin et nom du fichier, relatifs à / (ex : sdcard/toto.txt)
	 * @return True/False Etat de l'enregistrement du fichier
	 */
	public boolean EcrireFichierTexte(String text, String fichier)
	{       
	   File logFile = new File(fichier);
	   if (!logFile.exists())
	   {
		   //on créé le fichier
	      try {
	         logFile.createNewFile();
	      } catch (IOException e) {
	         e.printStackTrace();
	         return false;
	      }
	      //on écrit dans le fichier
	      try {
		      //BufferedWriter for performance, true to set append to file flag
		      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
		      buf.append(text);
		      buf.newLine();
		      buf.close();
		   } catch (IOException e) {
		      e.printStackTrace();
		      return false;
		   }
	   } else {
		   Toast toast = Toast.makeText(this, "Le fichier existe déjà ! Changez de nom !", Toast.LENGTH_LONG);toast.show();
		   return false;
	   }
 	   return true;
	}
	
	
	
	/**
	 * Retourne le modèle du mobile
	 * Source : http://stackoverflow.com/questions/14030223/android-device-name
	 * @return	Modèle du mobile
	 */
	public String getDeviceName() {
		  String manufacturer = Build.MANUFACTURER;
		  String model = Build.MODEL;
		  if (model.startsWith(manufacturer)) {
		    return model;
		  } else {
		    return manufacturer + " " + model;
		  }
		}
	
	
	
	@Override
	public void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}
	
}