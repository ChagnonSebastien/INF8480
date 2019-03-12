package ca.polymtl.inf8480.tp2.balancer;

import java.net.ConnectException;
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

public class OperationBlock extends Thread {

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

	public boolean hasServerBeenCalled(String hostname) {
		return this.serversCalled.containsKey(hostname);
	}

	@Override
	public void run() {
		authenticationError = false;
		capacityError = false;
		serverError = false;

		// Requete de calcul d'operation vers un serveur
		JsonObject request = new JsonObject();
		request.addProperty("login", this.login);
		request.addProperty("password", this.password);
		request.add("operations", this.operations);

		try {
			JsonObject operationResponse = new JsonParser().parse(toCall.getValue().compute(request.toString()))
					.getAsJsonObject();
			boolean authenticated = operationResponse.get("authenticated").getAsBoolean();

			if (!authenticated) {
				this.authenticationError = true;
				return;
			}

			boolean hasEnoughCapacity = operationResponse.get("enoughCapacity").getAsBoolean();

			if (hasEnoughCapacity) {
				int result = operationResponse.get("result").getAsInt();
				serversCalled.put(toCall.getKey(), result);
				capacityError = false;
			} else {
				capacityError = true;
			}

		} catch (RemoteException e) {
			System.out.println("Le block d'operations " + this.getName() + " n'a pas pu avoir une reponse du serveur." );
			this.serverError = true;
		}
	}

	public int getResult(boolean secured) throws Exception {
		if (secured) {
			if (serversCalled.size() > 0) {
				return serversCalled.values().toArray(new Integer[serversCalled.size()])[0];
			} else {
				throw new Exception("Le serveur n'a pas encore ete contacte.");
			}

		} else {
			if (serversCalled.size() < 2)
				throw new Exception("Deux serveurs n'ont pas encore ete contactes.");

			List<Integer> results = new ArrayList<Integer>(serversCalled.values());
			if (results.get(0) == results.get(1))
				return results.get(0);

			if (serversCalled.size() < 3)
				throw new Exception(
						"Les deux premiers serveurs appeles n'ont pas le meme resultat. Un troixieme serveur n'a pas encre ete contacte.");

			// Il y a au maximum un serveur qui est malicieux donc le troisieme serveur
			// contacte est assurement fiable.
			return results.get(2);
		}
	}
	
	public OperationBlock clone() {
		OperationBlock newBlock = new OperationBlock(this.operations, this.login, this.password, this.getName());
		newBlock.serversCalled = this.serversCalled;
		return newBlock;
	}
}