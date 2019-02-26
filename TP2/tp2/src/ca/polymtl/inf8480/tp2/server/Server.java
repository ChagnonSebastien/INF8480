/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP2 - INF8480
 */

package ca.polymtl.inf8480.tp2.server;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp2.shared.BalancerInterface;
import ca.polymtl.inf8480.tp2.shared.DirectoryInterface;
import ca.polymtl.inf8480.tp2.shared.ServerInterface;

public class Server extends RemoteServer implements ServerInterface {

	private static final long serialVersionUID = 4680914689425721831L;
	
	// Attributs
	String address = ""; // adresse du serveur
	int port = 0; // numero du port
	double falseAnswerRatio = 0.0; // taux de reponse erronnee (serveur en mode securise : 0, non securise : 0.01 à 1)
	int q = 0; // capacite du serveur (nombre d'operations mathematique)
	
	private DirectoryInterface directoryStub = null;
	
	public static void main(String[] args) {
		String hostname = "";
		String directoryHostname = "";
		
		if (args.length > 0) {
			hostname = args[0];
			directoryHostname = args[1];
		} else {
			hostname = "127.0.0.1";
			directoryHostname = "127.0.0.1";
		}
		
		Server server = new Server(hostname, directoryHostname);
		server.run();
	}
	
	public Server(String hostname, String directoryHostname) {
		super();
		this.address = hostname;
		this.directoryStub = loadDirectoryStub("127.0.0.1");
	}
	
	private DirectoryInterface loadDirectoryStub(String directoryHostname) {
		DirectoryInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(directoryHostname, 5000);
			System.out.println("REGISTRY OK");
			stub = (DirectoryInterface) registry.lookup("directory");
			System.out.println("DirectoryStub ok");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
		
		return stub;
	}
	
	private void logToDirectory() {
		if (this.directoryStub != null) {
			try {
				JsonObject response = new JsonParser().parse(directoryStub.logServer(this.address)).getAsJsonObject();
				
				if (!response.get("result").getAsBoolean()) {
					System.out.println(response.get("value").getAsString());
				} else {
					JsonObject serverConfig = response.get("value").getAsJsonObject();
							
					this.port = serverConfig.get("port").getAsInt();
					this.falseAnswerRatio = serverConfig.get("falseAnswerRatio").getAsDouble();
					this.q = serverConfig.get("q").getAsInt();
					
					System.out.println(this.address + "," + this.port + "," + this.falseAnswerRatio + "," + this.q);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Veuillez démarrer le service de répertoire de noms avant un serveur.");
			System.exit(0);
		}
	}
	
	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		this.logToDirectory();
		
		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, this.port);
			Registry registry = LocateRegistry.getRegistry(5000);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

}
