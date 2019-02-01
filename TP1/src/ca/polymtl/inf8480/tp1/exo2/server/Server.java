package ca.polymtl.inf8480.tp1.exo2.server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
	
	private Map<String, String> users;
	private HashSet<String> loggedUsers;
	
	private Map<String, List<String>> groups;
	private File groupListFile;
	
	final String emailRoot = "1734636-1804702-email";
	String currentPath = System.getProperty("user.dir");
	final String basePath = Paths.get(currentPath, emailRoot).toString();

	public Server() {
		super();
		this.getUsers();
		this.groupListFile = this.getGroupList();
		loggedUsers = new HashSet<String>();
		
		try {
			this.makeDirectory(emailRoot);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private void getUsers() {
		File usersFile = new File(currentPath, "users.json");

		if (!usersFile.exists()) {
			try {
				usersFile.createNewFile();
				
				// Populer la liste des utilisateurs dans le fichier
				JsonObject users = new JsonObject();
				users.addProperty("seb@polymtl.ca", "seb");
				users.addProperty("pierre@polymtl.ca", "pierre");
				
				FileWriter writer = new FileWriter(usersFile, false);
				PrintWriter print_line = new PrintWriter(writer);
				print_line.println(users.toString());
				print_line.close();
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		// Lire les utilisateurs dans le fichier
		try {
			FileReader reader = new FileReader(usersFile);
			JsonObject users = (JsonObject) new JsonParser().parse(reader);
			
			this.users = new HashMap<String, String>();
			
			for(Entry<String, JsonElement> user : users.entrySet()) {
				String email = user.getKey();
				String password = user.getValue().getAsString();
				this.users.put(email, password);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private File getGroupList() {
		File groupListFile = new File(currentPath, "grouplist.json");

		if (!groupListFile.exists()) {
			try {
				groupListFile.createNewFile();
				
				// populate group list
				JsonObject group = new JsonObject();
				
				JsonArray users = new JsonArray();
				users.add("seb@polymtl.ca");
				users.add("pierre@polymtl.ca");
				group.add("inf8480@polymtl.ca", users);
				
				FileWriter writer = new FileWriter(groupListFile, false);
				PrintWriter print_line = new PrintWriter(writer);
				print_line.println(group.toString());
				print_line.close();
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
		// Lire les groupes de diffusion dans le fichier
		try {
			FileReader reader = new FileReader(groupListFile);
			JsonObject groups = (JsonObject) new JsonParser().parse(reader);
			
			this.groups = new HashMap<String, List<String>>();
			
			for(Entry<String, JsonElement> group : groups.entrySet()) {
				String email = group.getKey();
				List<String> users = new ArrayList<String>(); 
				for (JsonElement user : group.getValue().getAsJsonArray()) {
					users.add(user.getAsString());
				}
				this.groups.put(email, users);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return groupListFile;
	}
	
	// Crée un dossier
	private void makeDirectory(String directoryName) throws Exception {
		File directory = new File(Paths.get(currentPath, directoryName).toString());
		if (!directory.mkdir()) {
			throw new Exception("Le dossier n'a pu être créé.");
		}
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
	
	public String getGroupList(long checksum) throws RemoteException {
		return null;
	}

	
	

}
