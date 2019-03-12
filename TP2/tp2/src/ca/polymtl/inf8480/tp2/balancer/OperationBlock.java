/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP2 - INF8480
 */

package ca.polymtl.inf8480.tp2.balancer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp2.shared.ServerInterface;

// Represente une tache pour le serveur de calcul (quelques operations a calculer)
public class OperationBlock extends Thread {

	// Attributs
	JsonArray operations;
	Map<String, Integer> serversCalled;

	boolean authenticationError = false;
	boolean capacityError = false;
	boolean serverError = false;

	Entry<String, ServerInterface> toCall;
	private String login;
	private String password;

	public OperationBlock(JsonArray operations, String login, String password, String name) {
		this.operations = operations;
		this.login = login;
		this.password = password;
		this.serversCalled = new HashMap<>();
		this.setName(name);
	}

	// Indique si le serveur a l'adresse ip a deja ete appelle
	public boolean hasServerBeenCalled(String hostname) {
		return this.serversCalled.containsKey(hostname);
	}

	@Override
	public void run() {
		authenticationError = false;
		capacityError = false;
		serverError = false;

		// Requete de calcul d'operations vers un serveur
		JsonObject request = new JsonObject();
		request.addProperty("login", this.login);
		request.addProperty("password", this.password);
		request.add("operations", this.operations);

		try {
			// Envoi de la tache au serveur de calcul
			JsonObject operationResponse = new JsonParser().parse(toCall.getValue().compute(request.toString()))
					.getAsJsonObject();
			
			boolean authenticated = operationResponse.get("authenticated").getAsBoolean();
			if (!authenticated) {
				// Si le repartiteur n'a pas ete authentifie correctement
				this.authenticationError = true;
				return;
			}

			boolean hasEnoughCapacity = operationResponse.get("enoughCapacity").getAsBoolean();

			if (hasEnoughCapacity) {
				// serveur a assez de capacite
				int result = operationResponse.get("result").getAsInt();
				serversCalled.put(toCall.getKey(), result);
				capacityError = false;
			} else {
				// serveur n'a pas assez de capacite
				capacityError = true;
			}
		} catch (RemoteException e) {
			System.out.println("Le block d'operations " + this.getName() + " n'a pas pu avoir une reponse du serveur." );
			this.serverError = true;
		}
	}

	// Retourne le resultat du serveur de calcul
	public int getResult(boolean secured) throws Exception {
		if (secured) {
			// Mode securise
			if (serversCalled.size() > 0) {
				return serversCalled.values().toArray(new Integer[serversCalled.size()])[0];
			} else {
				throw new Exception("Le serveur n'a pas encore ete contacte.");
			}
		} else {
			// Mode non securise
			if (serversCalled.size() < 2)
				throw new Exception("Deux serveurs n'ont pas encore ete contactes.");

			// Verification du resultat de deux serveurs de calculs
			List<Integer> results = new ArrayList<Integer>(serversCalled.values());
			if (results.get(0).intValue() == results.get(1).intValue()) {
				return results.get(0);
			}

			if (serversCalled.size() < 3)
				throw new Exception(
						"Les deux premiers serveurs appeles n'ont pas le meme resultat. Un troixieme serveur n'a pas encre ete contacte.");

			// Le resultat est bon pour deux serveurs de calcul en presence d'un serveur malicieux
			if (results.get(0).intValue() == results.get(1).intValue()) {
				return results.get(0);
			} else if (results.get(0).intValue() == results.get(2).intValue()) {
				return results.get(2);
			} else {
				return results.get(1);
			}
		}
	}
	
	// Retourne un bloc d'operation clone a partir du premier
	public OperationBlock clone() {
		OperationBlock newBlock = new OperationBlock(this.operations, this.login, this.password, this.getName());
		newBlock.serversCalled = this.serversCalled;
		return newBlock;
	}
}