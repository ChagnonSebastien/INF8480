package ca.polymtl.inf8480.tp2.balancer;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp2.shared.BalancerInterface;
import ca.polymtl.inf8480.tp2.shared.DirectoryInterface;
import ca.polymtl.inf8480.tp2.shared.ServerInterface;

public class Balancer extends RemoteServer implements BalancerInterface {
	
	private static final long serialVersionUID = -3221999619303495281L;
	
	// Attributs
	private String login = "";
	private String password = "";
	private boolean secureMode = true;
	
	private DirectoryInterface directoryStub = null;
	private JsonObject servers = null;

	public static void main(String[] args) {
		String directoryHostname = "";
		boolean secureMode  = true;
		
		if (args.length > 0) {
			secureMode = args[0].equals("true");
		}
		
		if (args.length > 1) {
			directoryHostname = args[1];
		} else {
			directoryHostname = "127.0.0.1";
		}
		
		Balancer balancer = new Balancer(directoryHostname, secureMode);
		balancer.run();
	}

	public Balancer(String directoryHostname, boolean secureMode) {
		super();
		this.login = "balancy";
		this.password = "bal1";
		this.secureMode = secureMode;
		this.directoryStub = loadDirectoryStub(directoryHostname);
		this.servers = new JsonObject();
	}
	
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
	
	private Map<String, ServerInterface> loadServerStubs() {
		Map<String, ServerInterface> serverStubs = new HashMap<>();
		
		for (String hostname : this.servers.keySet()) {
			ServerInterface stub = null;

			try {
				Registry registry = LocateRegistry.getRegistry(hostname, 5000);
				stub = (ServerInterface) registry.lookup("server");
			} catch (NotBoundException e) {
				System.out.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
			} catch (AccessException e) {
				System.out.println("Erreur: " + e.getMessage());
			} catch (RemoteException e) {
				System.out.println("Erreur: " + e.getMessage());
			}
			
			serverStubs.put(hostname, stub);
		}
		
		return serverStubs;
	}

	private void run() {
		try {
			BalancerInterface stub = (BalancerInterface) UnicastRemoteObject.exportObject(this, 5001);
			Registry registry = LocateRegistry.getRegistry(5000);
			registry.rebind("balancer", stub);
			System.out.println("Balancer ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lance ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
	}
	
	private JsonArray getSubArray(JsonArray array, int start, int end) {
		JsonArray subArray = new JsonArray();
		
		for (int i = start; i < end; i++) {
			subArray.add(array.get(i));
		}
		
		return subArray;
	}

	@Override
	public String computeOperations(String ops) throws RemoteException {
		// Get server hostnames from directory
		this.servers = new JsonParser().parse(directoryStub.getServers()).getAsJsonObject();
		
		// Get server stubs
		Map<String, ServerInterface> serverStubs = loadServerStubs();
		
		JsonObject response = new JsonObject();

		// S'il n'y a aucun serveur enregistre dans le service de repertoire de noms
		if (this.servers.keySet().size() == 0) {
			response.addProperty("result", false);
			response.addProperty("value", "Il n'y a aucun serveur disponible.");
			return response.toString();
		}
		
		JsonArray operations = new JsonParser().parse(ops).getAsJsonArray();
		int value = 0;
		
		int nbOpsATraiter = operations.size();
		int q = this.servers.get("127.0.0.1").getAsInt();
		int start = 0;
		int end = q;
		
		// TODO : verifier la capacité 
		// TODO : faire la repartition du travail
		while (nbOpsATraiter > 0) {
			if (end > operations.size()) {
				end = operations.size();
			}
			
			ServerInterface stub = serverStubs.get("127.0.0.1");
			
			// Requete de calcul d'operation vers un serveur
			JsonObject request = new JsonObject();
			request.addProperty("login", this.login);
			request.addProperty("password", this.password);
			request.add("operations", this.getSubArray(operations, start, end));
			
			JsonObject operationResponse = new JsonParser().parse(stub.compute(request.toString())).getAsJsonObject();
			boolean authenticated = operationResponse.get("authenticated").getAsBoolean();
			
			if (!authenticated) {
				response.addProperty("result", false);
				response.addProperty("value", "Le repartiteur ne peut pas s'authentifier aupres du service de repertoire de noms.");
				return response.toString();
			}
			
			boolean enoughCapacity = operationResponse.get("enoughCapacity").getAsBoolean();
			
			if (!enoughCapacity) {
				// n'a pas fonctionnee TODO : envoyer au prochain serveur ou reesayer
				System.out.println("Le serveur n'a pas assez de capacite pour traiter les ops");
			} else {
				int result = operationResponse.get("result").getAsInt();
				value = (value + result) % 5000;
			}
			
			nbOpsATraiter -= (end - start);
			start += q;
			end += q;
		}
		
		response.addProperty("result", true);
		response.addProperty("value", value);
		return response.toString();
	}

}
