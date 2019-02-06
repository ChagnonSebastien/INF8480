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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp1.exo2.shared.Hash;
import ca.polymtl.inf8480.tp1.exo2.shared.JsonUtils;
import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public class Server extends RemoteServer implements ServerInterface {
	
	private static final long serialVersionUID = 3520052702176224119L;

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	private String lockedUser = null;
	private File groupListFile;
	private File usersFile;
	
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
		this.usersFile = new File(serverDirPath, "users.json");

		if (!usersFile.exists()) {
			try {
				usersFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Populer la liste des utilisateurs dans le fichier
			JsonObject users = new JsonObject();
			
			JsonObject user1 = new JsonObject();
			user1.addProperty("password", "seb");
			user1.addProperty("mailCount", 0);
			users.add("seb@polymtl.ca", user1);
			
			JsonObject user2 = new JsonObject();
			user2.addProperty("password", "pierre");
			user2.addProperty("mailCount", 0);
			users.add("pierre@polymtl.ca", user2);
			
			JsonUtils.writeToFile(users.toString(), this.usersFile);
		}
		
		// Lire les utilisateurs dans le fichier
		try {
			FileReader reader = new FileReader(this.usersFile);
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
		
		// Lire les groupes de multidiffusion dans le fichier
		try {
			FileReader reader = new FileReader(groupListFile);
			this.groups = new JsonParser().parse(reader).getAsJsonObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return groupListFile;
	}
	
	private boolean userExists(String user) {
		return this.users.get(user) != null;
	}
	
	private boolean userIsLogged(String user) {
		return this.loggedUsers.contains(user);
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
	// TODO : un client a la fois peut s'authentifier
	@Override
	public String openSession(String login, String password) throws RemoteException {
		
		if (this.users.get(login) == null ||
				!this.users.get(login).getAsJsonObject().get("password").getAsString().equals(password) ||
				this.loggedUsers.contains(login))
			return "";
		
		loggedUsers.add(login);
		return login;
	}

	@Override
	public String getGroupList(String checksum, String login) throws RemoteException {
		JsonObject response = new JsonObject();
		
		// Check if user is logged
		if (!userIsLogged(login)) {
			response.addProperty("result", false);
			response.addProperty("content", "Vous n'etes pas authentifie. Que faites-vous ici?");
			return response.toString();
		}
		
		String actualChecksum = Hash.MD5.checksum(groupListFile);
		if (actualChecksum.equals(checksum)) {
			response.addProperty("result", false);
			response.addProperty("content", "Vous avez deja la derniere version de la liste de groupes.");
		} else {
			response.addProperty("result", true);
			response.addProperty("content", this.groups.toString());
			
		}
		
		return response.toString();
	}

	@Override
	public String pushGroupList(String groupsDef, String login) throws RemoteException {
		// Check if user is logged
		if (!userIsLogged(login)) {
			return "Vous n'etes pas authentifie. Que faites-vous ici?";
		}
		
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
		// Check if user is logged
		if (!userIsLogged(login)) {
			return "Vous n'etes pas authentifie. Que faites-vous ici?";
		}
		
		if (lockedUser != null)
			return "La liste de groupe globale est deja verrouilee par " + this.lockedUser;
		
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

	@Override
	public String sendMail(String email) throws RemoteException {
		JsonObject emailJson = new JsonParser().parse(email).getAsJsonObject();
		String from = emailJson.get("from").getAsString();
		
		// Check if user is logged
		if (!userIsLogged(from)) {
			return "Vous n'etes pas authentifie. Que faites-vous ici?";
		}
		
		String to = emailJson.get("to").getAsString();
		
		String result = "";
		
		List<String> dests = new ArrayList<String>();
		
		JsonElement elem = this.groups.get(to); 
		if (elem != null) {
			// Courriel envoye a un groupe
			JsonArray groupList = elem.getAsJsonArray();
			
			if (groupList.size() == 0) {
				result += "L'adresse de multidiffusion ne contient aucun utilisateur.\n";
			}
			
			for (JsonElement dest : groupList) {
				if (userExists(dest.getAsString())) {
					dests.add(dest.getAsString());
				}
				else {
					result += "L'utilisateur " + dest.getAsString() + " dans le groupe de multidiffusion " + to + " n'existe pas.\n";
				}
			}
		}
		else {
			// Courriel envoye a un utilisateur unique
			if (userExists(to)) {
				dests.add(to);
			}
			else {
				result += "L'utilisateur " + to + " n'existe pas.\n";
			}
		}
		
		// Envoi de courriel(s)
		for (String dest : dests) {
			String destFolder = Paths.get(this.emailsPath, dest).toString();
			new File(destFolder).mkdir();
			
			SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm");
			emailJson.addProperty("date", format.format(new Date()));
			
			emailJson.addProperty("read", false);
			
			int mailId = this.users.get(dest).getAsJsonObject().get("mailCount").getAsInt() + 1;
			emailJson.addProperty("id", mailId);
			this.users.get(dest).getAsJsonObject().addProperty("mailCount", mailId);
			
			// Update users file
			JsonUtils.writeToFile(this.users.toString(), this.usersFile);
			
			// Write email
			File emailFile = new File(destFolder, mailId + ".json");
			try {
				emailFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			JsonUtils.writeToFile(emailJson.toString(), emailFile);
		}
		
		if (dests.size() > 0) {
			result += "Courriel envoye avec succes a " + (dests.size() > 1 ? ("la liste de multidiffusion " + to) : to);
		}
		else {
			result += "Courriel non envoye";
		}
		
		return result;
	}
	
	@Override
	public String listMails(boolean justUnread, String login) throws RemoteException {
		JsonObject response = new JsonObject();
		
		// Check if user is logged
		if (!userIsLogged(login)) {
			response.addProperty("result", false);
			response.addProperty("content", "Vous n'etes pas authentifie. Que faites-vous ici?");
			return response.toString();
		}
		
		String destFolderPath = Paths.get(this.emailsPath, login).toString();
		File destFolder = new File(destFolderPath);
		destFolder.mkdir();
		
		File[] listOfFiles = destFolder.listFiles();
		List<JsonObject> messages = new ArrayList<>();
		
		for (File messageFile : listOfFiles) {
			try (FileReader reader = new FileReader(messageFile)) {
				JsonObject message = new JsonParser().parse(reader).getAsJsonObject();
				if (justUnread ? !message.get("read").getAsBoolean() : true)
					messages.add(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		messages.sort(new Comparator<JsonObject>() {
			@Override
			public int compare(JsonObject o1, JsonObject o2) {
				return o1.get("id").getAsInt() - o2.get("id").getAsInt();
			}
		});
		
		JsonArray responseContent = new JsonArray();
		for (JsonObject message : messages)
			responseContent.add(message);

		response.addProperty("result", true);
		response.addProperty("content", responseContent.toString());
		response.addProperty("mailCount", listOfFiles.length);
		return response.toString();
	}
	
	public String readMail(int id, String login) throws RemoteException {
		JsonObject response = new JsonObject();
		
		// Check if user is logged
		if (!userIsLogged(login)) {
			response.addProperty("result", false);
			response.addProperty("content", "Vous n'etes pas authentifie. Que faites-vous ici?");
			return response.toString();
		}
		
		String destFolderPath = Paths.get(this.emailsPath, login).toString();
		File destFolder = new File(destFolderPath);
		destFolder.mkdir();
		
		File[] listOfFiles = destFolder.listFiles();
		JsonObject message = null;
		
		for (File messageFile : listOfFiles) {
			try (FileReader reader = new FileReader(messageFile)) {
				JsonObject m = new JsonParser().parse(reader).getAsJsonObject();
				if (m.get("id").getAsInt() == id) {
					message = m;
					m.addProperty("read", true);
					JsonUtils.writeToFile(m.toString(), messageFile);
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (message == null) {
			response.addProperty("result", false);
			response.addProperty("content", "L'id du courriel n'existe pas.");
			return response.toString();
		}
		
		response.addProperty("result", true);
		response.addProperty("content", message.get("content").getAsString());
		return response.toString();
	}

	@Override
	public String deleteMail(int id, String login) throws RemoteException {		
		// Check if user is logged
		if (!userIsLogged(login)) {
			return "Vous n'etes pas authentifie. Que faites-vous ici?";
		}
		
		String destFolderPath = Paths.get(this.emailsPath, login).toString();
		File destFolder = new File(destFolderPath);
		destFolder.mkdir();
		
		File[] listOfFiles = destFolder.listFiles();
		for (File messageFile : listOfFiles) {
			try (FileReader reader = new FileReader(messageFile)) {
				JsonObject m = new JsonParser().parse(reader).getAsJsonObject();
				if (m.get("id").getAsInt() == id) {
					reader.close();
					messageFile.delete();
					return "Le courriel avec l'id " + id + " a ete supprime avec succes." ;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return "L'id du courriel n'existe pas.";
	}

	@Override
	public String findMail(String args, String login) throws RemoteException {		
		JsonObject response = new JsonObject();
		
		// Check if user is logged
		if (!userIsLogged(login)) {
			response.addProperty("result", false);
			response.addProperty("content", "Vous n'etes pas authentifie. Que faites-vous ici?");
			return response.toString();
		}
		
		String destFolderPath = Paths.get(this.emailsPath, login).toString();
		File destFolder = new File(destFolderPath);
		destFolder.mkdir();
		
		File[] listOfFiles = destFolder.listFiles();
		List<JsonObject> messages = new ArrayList<>();
		
		for (File messageFile : listOfFiles) {
			try (FileReader reader = new FileReader(messageFile)) {
				JsonObject m = new JsonParser().parse(reader).getAsJsonObject();
				if (containsWords(m.get("content").getAsString(), args.split(" "))) {
					messages.add(m);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		messages.sort(new Comparator<JsonObject>() {
			@Override
			public int compare(JsonObject o1, JsonObject o2) {
				return o1.get("id").getAsInt() - o2.get("id").getAsInt();
			}
		});
		
		JsonArray responseContent = new JsonArray();
		for (JsonObject message : messages)
			responseContent.add(message);

		response.addProperty("result", true);
		response.addProperty("content", responseContent.toString());
		response.addProperty("mailCount", messages.size());
		return response.toString();
	}
	
	private static boolean containsWords(String message, String[] words) {
		int wordsFound = 0;
		
		for (String word : words) {
			if (message.contains(word)) {
				wordsFound++;
			}
		}
		
		return wordsFound == words.length;
	}
	
}
