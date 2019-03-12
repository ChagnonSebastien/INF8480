/*
 * @authors : Sebastien Chagnon (1804702), Pierre To (1734636)
 * TP2 - INF8480
 */

package ca.polymtl.inf8480.tp2.balancer;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp2.shared.BalancerInterface;
import ca.polymtl.inf8480.tp2.shared.DirectoryInterface;
import ca.polymtl.inf8480.tp2.shared.ServerInterface;

// Point d'entree du systeme au niveau du serveur
// Repartit les operations parmi les serveurs de calcul
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
		boolean secureMode = true;

		if (args.length > 0) {
			secureMode = args[0].equals("true");
		} else {
			System.out.println("Vous devez fournir un mode de securite");
			System.exit(0);
		}

		if (args.length > 2) {
			directoryHostname = args[2];
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

	// Retourne l'interface du service de repertoire de noms
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

	// Retourne les interfaces associees a chaque serveur de calcul
	private Map<String, ServerInterface> loadServerStubs() {
		Map<String, ServerInterface> serverStubs = new HashMap<>();

		System.out.println("Loading server stubs...");
		for (String hostname : this.servers.keySet()) {
			System.out.println("Loading server stub " + hostname + "...");
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

	// Retourne un sous-tableau du tableau fourni selon un debut et une fin
	private JsonArray getSubArray(JsonArray array, int start, int end) {
		JsonArray subArray = new JsonArray();

		for (int i = start; i < end; i++) {
			subArray.add(array.get(i));
		}

		return subArray;
	}

	@Override
	public String computeOperations(String ops) throws RemoteException {
		// Aller chercher les adresses ip des serveurs de calcul du service de repertoire de noms
		System.out.println("Nouvelle requete de calcul...");
		this.servers = new JsonParser().parse(directoryStub.getServers()).getAsJsonObject();

		// Get server stubs
		Map<String, ServerInterface> serverStubs = loadServerStubs();

		JsonObject response = new JsonObject();

		// S'il n'y a aucun serveur enregistre dans le service de repertoire de noms
		if (this.servers.keySet().size() == 0) {
			System.out.println("Il n'y a pas de serveurs enregistres");
			response.addProperty("result", false);
			response.addProperty("value", "Il n'y a aucun serveur disponible.");
			return response.toString();
		}

		JsonArray operations = new JsonParser().parse(ops).getAsJsonArray();
		int value = 0;

		// On cree des blocs d'operation ayant pour taille la capacite minimale parmi
		// tous les serveurs (repartition des operations)
		int qMin = Integer.MAX_VALUE;
		for (String ip : servers.keySet()) {
			int q = servers.get(ip).getAsInt();
			if (q < qMin)
				qMin = q;
		}
		System.out.println("Capacite minimale des serveurs de calculs : " + qMin);

		int opsToProcess = operations.size();
		int start = 0;
		int end = qMin;

		// Creation des threads qui vont executer des blocs d'operations
		List<OperationBlock> blocks = new ArrayList<>();
		System.out.println("Creation des blocs d'operations...");
		while (opsToProcess > 0) {
			if (end > operations.size()) {
				end = operations.size();
			}

			blocks.add(new OperationBlock(this.getSubArray(operations, start, end), this.login, this.password, start + " to " + end));

			opsToProcess -= (end - start);
			start += qMin;
			end += qMin;
		}

		// Boucle d'execution
		int index = 0;
		System.out.println("Debut des requetes");
		while (blocks.size() > 0) {

			OperationBlock block = blocks.get(index);
			// Si le Thread a termine sa precedente execution
			if (!block.isAlive()) {

				// Retourne l'erreur au client si l'authentification du repartiteur echoue
				if (block.authenticationError) {
					response.addProperty("result", false);
					response.addProperty("value",
							"Le repartiteur ne peut pas s'authentifier aupres du service de repertoire de noms.");
					return response.toString();
				}

				// Retourne l'erreur au client s'il n'y a plus aucun serveur disponible
				// Gestion de pannes intempestives d'un serveur de calcul
				if (block.serverError) {
					String hostname = block.toCall.getKey();

					if (this.servers.has(hostname)) {
						this.servers.remove(hostname);
					}

					if (serverStubs.containsKey(hostname)) {
						serverStubs.remove(hostname);
					}

					if (serverStubs.size() == 0) {
						response.addProperty("result", false);
						response.addProperty("value", "Il n'y a plus de serveur disponible");
						return response.toString();
					}
				}
				
				try {
					int result = block.getResult(this.secureMode); // ceci peut retourner 3 exceptions qui indiquent que le resultat n'a pas encore ete calcule ou que le resultat n'est pas encore verifie (voir OperationBlock)
					value += result;
					value %= 5000;
					blocks.remove(index);
				} catch (Exception e) {
					// En attente d'une reponse fiable (gestion de serveurs malicieux)
					// Trouver un stub disponible pour l'execution du thead
					List<Entry<String, ServerInterface>> potentialStubs = new ArrayList<>();
					for (Entry<String, ServerInterface> server : serverStubs.entrySet()) {
						String hostname = server.getKey();
						if (!block.hasServerBeenCalled(hostname)) {
							potentialStubs.add(server);
						}
					}

					// Repartition de la tache
					if (potentialStubs.size() > 0) {
						OperationBlock newBlock = block.clone();
						newBlock.toCall = potentialStubs.get(new Random().nextInt(potentialStubs.size()));
						newBlock.start();
						blocks.set(index, newBlock);
					}
				}
			}

			// On passe au prochain Thread
			if (++index >= blocks.size())
				index = 0;
		}

		response.addProperty("result", true);
		response.addProperty("value", value);
		return response.toString();
	}

}
