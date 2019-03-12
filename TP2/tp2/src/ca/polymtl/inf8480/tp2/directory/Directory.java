package ca.polymtl.inf8480.tp2.directory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp2.shared.DirectoryInterface;

public class Directory extends RemoteServer implements DirectoryInterface {

	private static final long serialVersionUID = 5906690575291221944L;
	
	// Attributs
	File serverConfigFile = null;
	File balancerFile = null;
	JsonObject servers = null;
	
	public static void main(String[] args) {
		Directory directory = new Directory();
		directory.run();
	}
	
	public Directory() {
		super();
		
		this.serverConfigFile = new File("server-config.json");
		if (!this.serverConfigFile.exists()) {
			System.out.println("Le fichier de configuration n'existe pas. Veuillez l'ajouter.");
			System.exit(0);
		}
		
		this.balancerFile = new File("authorized-balancers.json");
		if (!this.balancerFile.exists()) {
			System.out.println("Le fichier des repartiteurs autorises n'existe pas. Veuillez l'ajouter.");
			System.exit(0);
		}
		
		this.servers = new JsonObject();
	}

	private void run() {
		try {
			DirectoryInterface stub = (DirectoryInterface) UnicastRemoteObject.exportObject(this, 5050);
			Registry registry = LocateRegistry.getRegistry(5000);
			registry.rebind("directory", stub);
			System.out.println("Directory ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lance ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
	}

	@Override
	public String logServer(String hostname) throws RemoteException {
		JsonObject response = new JsonObject();
		System.out.println("Le serveur " + hostname + " tente de se connecter au service de repertoire de noms.");
		
		try {
			JsonObject configs = new JsonParser().parse(new FileReader(this.serverConfigFile)).getAsJsonObject();
			
			if (!configs.has(hostname)) {
				System.out.println("L'addresse " + hostname + " n'existe pas dans le fichier de configuration.");
				response.addProperty("result", false);
				response.addProperty("value", "L'adresse du serveur n'existe pas dans le fichier de configuration.\nVeuillez l'ajouter dans server-config.json");
				return response.toString();
			}
			
			JsonObject serverConfig = configs.get(hostname).getAsJsonObject();
						
			response.addProperty("result", true);
			response.add("value", serverConfig);
			
			// Se souvenir de la capacite de chaque serveur
			this.servers.addProperty(hostname, serverConfig.get("q").getAsInt());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return response.toString();
	}

	@Override
	public String getServers() throws RemoteException {
		return this.servers.toString();
	}
	
	@Override
	public boolean authenticateBalancer(String login, String password) throws RemoteException {
		try {
			JsonObject balancers = new JsonParser().parse(new FileReader(this.balancerFile)).getAsJsonObject();
			
			if (!balancers.has(login)) {
				return false;
			}
			
			return balancers.get(login).getAsString().equals(password);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	

}
