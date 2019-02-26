package ca.polymtl.inf8480.tp2.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.google.gson.JsonArray;

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
	private String clientDir;

	public Client(String distantBalancerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		if (distantBalancerHostname != null) {
			balancerStub = loadBalancerStub(distantBalancerHostname);
		} else {
			balancerStub = loadBalancerStub("127.0.0.1"); // serveur local
		}

		clientDir = Paths.get(System.getProperty("user.dir"), "1734636-1804702-client").toString();
		new File(clientDir).mkdir();
	}

	private BalancerInterface loadBalancerStub(String hostname) {
		BalancerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname, 5001);
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
				
				
				
				
				System.out.println(balancerStub.computeOperations(""));
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private JsonArray parseOperations(String operationFileName) {
		File operationsFile = new File(operationFileName);
		if (!operationsFile.exists()) {
			return null;
		}
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(operationsFile));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
		
		
		
		
		
		
	}
	
	

}
