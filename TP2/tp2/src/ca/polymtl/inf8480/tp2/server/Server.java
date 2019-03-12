/*
 * @authors : Sebastien Chagnon (1804702), Pierre To (1734636)
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
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp2.shared.DirectoryInterface;
import ca.polymtl.inf8480.tp2.shared.ServerInterface;

// Execute les operations transmis par le repartiteur
// Le serveur peut etre configurer pour accepter un nombre maximale de requetes ainsi que son taux de reponses malicieuses
public class Server extends RemoteServer implements ServerInterface {

	private static final long serialVersionUID = 4680914689425721831L;
	
	// Attributs
	String address = ""; // adresse du serveur
	int port = 0; // numero du port
	double falseAnswerRatio = 0.0; // taux de reponse erronnee (serveur en mode securise : 0, non securise : 0.01 à 1)
	int capacity = 0; // capacite du serveur (nombre d'operations mathematique)
	
	static Integer operationsCountAccepted = 0; // Nombre d'operations dans la queue du serveur
	
	private DirectoryInterface directoryStub = null; // Stub du service de repertoire de noms
	
	public static void main(String[] args) {
		String hostname = "";
		String directoryHostname = "";
		
		// IP du Serveur
		if (args.length > 0) {
			hostname = args[0];
		} else {
			hostname = "127.0.0.1";
		}
		
		//IP du service de repertoire de noms
		if (args.length > 1) {
			directoryHostname = args[1];
		} else {
			directoryHostname = "127.0.0.1";
		}

		Server server = new Server(hostname, directoryHostname);
		server.run();
	}
	
	public Server(String hostname, String directoryHostname) {
		super();
		this.address = hostname;
		this.directoryStub = loadDirectoryStub(directoryHostname);
	}
	
	// Initialise le stub du service de repertoire de noms
	private DirectoryInterface loadDirectoryStub(String directoryHostname) {
		DirectoryInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(directoryHostname, 5000);
			stub = (DirectoryInterface) registry.lookup("directory");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
		
		return stub;
	}
	
	// Averti le service de repertoire de nom de son existence
	private void logToDirectory() {
		if (this.directoryStub != null) {
			try {
				JsonObject response = new JsonParser().parse(directoryStub.logServer(this.address)).getAsJsonObject();
				
				if (!response.get("result").getAsBoolean()) {
					
					// si le repertoire de noms ne reconnait pas le serveur
					System.out.println(response.get("value").getAsString());
					System.out.println("Exiting...");
					System.exit(0);
				
				} else {
					
					// Initialise les parametres du serveur
					JsonObject serverConfig = response.get("value").getAsJsonObject();
							
					this.port = serverConfig.get("port").getAsInt();
					this.falseAnswerRatio = serverConfig.get("falseAnswerRatio").getAsDouble();
					this.capacity = serverConfig.get("q").getAsInt();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Veuillez demarrer le service de repertoire de noms avant un serveur.");
			System.exit(0);
		}
	}
	
	// Methode qui est appelee lors du demarrage du serveur
	private void run() {
		this.logToDirectory();
		
		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, this.port);
			Registry registry = LocateRegistry.getRegistry(5000);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lance ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}
	
	// Verifie si le serveur est apte a accepter une nouvelle serie d'operations
	private boolean checkCapacity(int operationSize) {
		
		// Mutex pour lire et editer la variable globale
		synchronized (Server.operationsCountAccepted) {
			int operationsCount = operationSize + Server.operationsCountAccepted;
			double rejectionRatio = (operationsCount - this.capacity) / (5.0 * this.capacity);
			double currentRatio = Math.random();
			
			// les operations sont acceptees si on respecte le ratio de rejet (plus grand que)
			boolean isAccepted = currentRatio > rejectionRatio;
			
			// Modification du nombre d'operations dans la pile
			if (isAccepted) {
				Server.operationsCountAccepted += operationSize;
			}
			
			return isAccepted;
		}
	}

	// Fonction appelee par le repartiteur pour calculer un bloc d'operations
	@Override
	public String compute(String request) throws RemoteException {
		System.out.println("Nouvelle requete de calcul...");
		JsonObject requestJson = new JsonParser().parse(request).getAsJsonObject();
		
		String login = requestJson.get("login").getAsString();
		String password = requestJson.get("password").getAsString();
		JsonArray operations = requestJson.get("operations").getAsJsonArray();
		
		JsonObject response = new JsonObject();
		
		try {
			// Verification des identifiants du repartiteur 
			if (directoryStub.authenticateBalancer(login, password)) {
				System.out.println("Authentification reussi");
				response.addProperty("authenticated", true);

				// serveur verifie s'il n'est pas surcharge
				boolean enoughCapacity = checkCapacity(operations.size());
				response.addProperty("enoughCapacity", enoughCapacity);
				if (!enoughCapacity) {
					return response.toString();
				}
				
				System.out.println("Capacite sufisante");
				int result = 0;
				
				// le serveur malicieux retourne une reponse aleatoire
				if (this.falseAnswerRatio > Math.random()) {
					System.out.println("<*.*>");
					result = new Random().nextInt(5000);
				}
				
				// execution des operations du bloc
				for (JsonElement op : operations) {
					String operation = op.getAsJsonObject().get("operation").getAsString();
					int operande = op.getAsJsonObject().get("operande").getAsInt();
					
					if (operation.equals("pell")) {
						result = (result + Operations.pell(operande)) % 5000;
					}
					else if (operation.equals("prime")) {
						result = (result + Operations.prime(operande)) % 5000;
					}
					
				}
				
				System.out.println("Resultat intermediaire : " + result);
				response.addProperty("result", result);
				
				// Mutex pour editer la variable globale
				synchronized (Server.operationsCountAccepted) {
					Server.operationsCountAccepted -= operations.size();
				}
			}
			else {
				// L'authentification a echouee
				System.out.println("Authentification echouee");
				response.addProperty("authenticated", false);
			}
		}
		catch(RemoteException e) {
			e.printStackTrace();
		}
		
		return response.toString();
	}

}
