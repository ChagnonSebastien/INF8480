package ca.polymtl.inf8480.tp1.exo2.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp1.exo2.shared.Hash;
import ca.polymtl.inf8480.tp1.exo2.shared.JsonUtils;
import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public class Server extends RemoteServer implements ServerInterface {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3520052702176224119L;

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	private String lockedUser = null;
	private File groupListFile;
	
	private JsonObject users;
	private JsonObject groups;
	
	private HashSet<String> loggedUsers;
	
	String serverDirPath = Paths.get(System.getProperty("user.dir"), "1734636-1804702-server").toString();
	final String emailsPath = Paths.get(serverDirPath, "emails").toString();

	public Server() {
		super();
		
		try {
			new File(serverDirPath).mkdir();
			new File(emailsPath).mkdir();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		this.getUsers();
		this.groupListFile = this.getGroupList();
		loggedUsers = new HashSet<String>();
	}

	private void getUsers() {
		File usersFile = new File(serverDirPath, "users.json");

		if (!usersFile.exists()) {
			try {
				usersFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Populer la liste des utilisateurs dans le fichier
			JsonObject users = new JsonObject();
			users.addProperty("seb@polymtl.ca", "seb");
			users.addProperty("pierre@polymtl.ca", "pierre");
			
			JsonUtils.writeToFile(users.toString(), usersFile);
		}
		
		// Lire les utilisateurs dans le fichier
		try {
			FileReader reader = new FileReader(usersFile);
			this.users = (JsonObject) new JsonParser().parse(reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private File getGroupList() {
		File groupListFile = new File(serverDirPath, "grouplist.json");

		if (!groupListFile.exists()) {
			try {
				groupListFile.createNewFile();
				
				// populate group list
				JsonObject group = new JsonObject();
				
				JsonArray users = new JsonArray();
				users.add("seb@polymtl.ca");
				users.add("pierre@polymtl.ca");
				group.add("inf8480@polymtl.ca", users);
				
				JsonUtils.writeToFile(group.toString(), groupListFile);
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		// Lire les groupes de diffusion dans le fichier
		try {
			FileReader reader = new FileReader(groupListFile);
			this.groups = new JsonParser().parse(reader).getAsJsonObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return groupListFile;
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
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancÃ© ?");
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
		
		if (this.users.get(login) == null || !this.users.get(login).getAsString().equals(password))
			return "";
		
		loggedUsers.add(login);
		return login;
	}

	@Override
	public String getGroupList(String checksum) throws RemoteException {
		String actualChecksum = Hash.MD5.checksum(groupListFile);
		if (actualChecksum.equals(checksum)) {
			return null;
		} else {
			return this.groups.toString();
		}
	}

	@Override
	public String pushGroupList(String groupsDef, String login) throws RemoteException {

		if (lockedUser == null)
			return"Impossible de publier les changements: Vous devez verrouiller la liste de groupes globale";
		
		if (!lockedUser.equals(login)) 
			return "Impossible de publier les changements: La liste de groupes globale est deja verrouillee par " + this.lockedUser;
		
		JsonUtils.writeToFile(groupsDef, this.groupListFile);
		this.groups = new JsonParser().parse(groupsDef).getAsJsonObject();
		
		this.lockedUser = null;
		return "Les modifications apportees a la liste de groupes globale sont publiees avec succes";
	}

	@Override
	public String lockGroupList(String login) throws RemoteException {
		if (lockedUser != null)
			return "La liste de groupe globale est déja verrouilee par " + this.lockedUser;
		
		long timeout = 60000;
		Server server = this;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					server.lockedUser = null;
				}
			}
		}).start();
		
		this.lockedUser = login;
		return "La liste de groupes globale est verrouillee avec succes. Votre lock expirera dans " + timeout / 1000 + "s";
	}

	
	

}
