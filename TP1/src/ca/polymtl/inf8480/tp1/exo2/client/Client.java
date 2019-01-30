package ca.polymtl.inf8480.tp1.exo2.client;

import java.io.BufferedReader;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.util.Scanner;

import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public class Client {
	public static void main(String[] args) {
		String distantHostname = null;

		if (args.length > 0) {
			// TODO remettre
			//		distantHostname = args[0];
		}

		Client client = new Client(distantHostname);
		client.run();
	}

	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServerStub = loadServerStub("127.0.0.1");

		/*if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}*/
	}

	private void run() {

		if (localServerStub != null) {
			appelRMILocal();
		}

		/*if (distantServerStub != null) {
			appelRMIDistant();
		}*/
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private void appelRMILocal() {
		try (
				Scanner scanner = new Scanner(System.in);
			)
		{
			String login = openSession(scanner);
			
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void appelRMIDistant() {

	}
	
	private String openSession(Scanner scanner) throws RemoteException {
		String login;
		boolean isConnected = false;
		do {
			System.out.println("Veuillez entrer votre nom d'utilisateur:");
			login = scanner.nextLine();
			System.out.println("Veuillez entrer votre mot de passe:");
			String password = scanner.nextLine();
			
			isConnected = localServerStub.openSession(login, password);
			
			if (isConnected) {
				System.out.println("Bienvenue dans votre boite à courriel " + login);
			} else {
				System.out.println("Erreur lors de la connection");
			}
		
		} while (!isConnected);

		return login;
	}
}
