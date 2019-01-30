package ca.polymtl.inf8480.tp1.exo2.server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public class Server extends RemoteServer implements ServerInterface {
	
	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}
	
	
	private Map<String, String> users;
	private HashSet<String> loggedUsers;

	public Server() {
		super();
		this.createUsers();
		loggedUsers = new HashSet<String>();
	}

	private void createUsers() {
		// TODO : Lire le fichier pour populer les utilisateurs
		this.users = new HashMap<String, String>();
		this.users.put("seb", "seb");
		this.users.put("pierre", "pierre");
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/*
	 * Methode pour s'authentifier et ouvrir une session au niveau serveur de messagerie
	 */
	@Override
	public String openSession(String login, String password) throws RemoteException {
		
		if (!this.users.containsKey(login) || !this.users.get(login).equals(password))
			return "";
		
		loggedUsers.add(login);
		return login;
	}

}
