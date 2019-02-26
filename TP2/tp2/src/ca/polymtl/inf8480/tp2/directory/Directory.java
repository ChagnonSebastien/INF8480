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
	
	File serverConfigFile = null;

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
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

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
		
		try {
			JsonObject configs = new JsonParser().parse(new FileReader(this.serverConfigFile)).getAsJsonObject();
			
			if (!configs.has(hostname)) {
				response.addProperty("result", false);
				response.addProperty("value", "L'adresse du serveur n'existe pas dans le fichier de configuration.");
				return response.toString();
			}
			
			JsonObject serverConfig = configs.get(hostname).getAsJsonObject();
						
			response.addProperty("result", true);
			response.addProperty("value", serverConfig.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return response.toString();
	}

}
