package ca.polymtl.inf8480.tp2.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp2.shared.BalancerInterface;

public class Client {

	public static void main(String[] args) {
		String distantHostname = null;

		if (args.length == 0) {
			System.out.println("Veuillez inclure le nom du fichier d'operations");
			System.exit(0);
		}
		
		if (args.length > 1) {
			distantHostname = args[1];
		}

		Client client = new Client(distantHostname);
		client.run(args[0]);
	}

	// Attributs
	private BalancerInterface balancerStub = null;

	public Client(String distantBalancerHostname) {
		super();
		
		if (distantBalancerHostname != null) {
			balancerStub = loadBalancerStub(distantBalancerHostname);
		} else {
			balancerStub = loadBalancerStub("127.0.0.1"); // serveur local
		}
	}

	private BalancerInterface loadBalancerStub(String hostname) {
		BalancerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname, 5000);
			stub = (BalancerInterface) registry.lookup("balancer");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private void run(String operationFileName) {
		if (balancerStub != null) {
			try {
				
				JsonArray operations = parseOperations(operationFileName);
				if (operations == null) {
					System.out.println("Ce fichier d'operations n'existe pas.");
				}
				
				JsonObject response = new JsonParser().parse(balancerStub.computeOperations(operations.toString())).getAsJsonObject();
				
				boolean result = response.get("result").getAsBoolean();
				
				if (!result) {
					System.out.println("Erreur : " + response.get("value"));
				}
				else {
					System.out.println("Le resultat des operations du fichier " + operationFileName + " est " + response.get("value"));
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private JsonArray parseOperations(String operationFileName) {
		File operationsFile = new File(operationFileName);
		if (!operationsFile.exists()) {
			return null;
		}
		
		JsonArray operations = new JsonArray();
		
		try {
			Scanner scanner = new Scanner(new FileReader(operationsFile));
			
			while (scanner.hasNextLine()) {
				String[] line = scanner.nextLine().split(" ");
				String operation = line[0];
				String operande = line[1];
				
				JsonObject op = new JsonObject();
				op.addProperty("operation", operation);
				op.addProperty("operande", operande);
				operations.add(op);
			}
			
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return operations;
	}

}
