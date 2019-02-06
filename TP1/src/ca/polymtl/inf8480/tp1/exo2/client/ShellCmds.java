package ca.polymtl.inf8480.tp1.exo2.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp1.exo2.shared.Hash;
import ca.polymtl.inf8480.tp1.exo2.shared.JsonUtils;
import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public enum ShellCmds {
	GET_GROUP_LIST ("get-group-list") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			File groupListFile = new File(userDir, "grouplist.json");

			String checksum = "";
			if (groupListFile.exists())
				checksum = Hash.MD5.checksum(groupListFile);
			
			JsonObject response = new JsonParser().parse(server.getGroupList(checksum, login)).getAsJsonObject();
			boolean result = response.get("result").getAsBoolean();
			String content = response.get("content").getAsString();
			
			if (!result) {
				System.out.println(content);
				return;
			}
			
			if (!groupListFile.exists()) {
				try {
					groupListFile.createNewFile();
				}
				catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
			
			JsonUtils.writeToFile(content, groupListFile);
			System.out.println("La liste des groupes a ete mise a jour");
			
		}
	},
	PUSH_GROUP_LIST ("publish-group-list") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			File groupListFile = new File(userDir, "grouplist.json");

			if (!groupListFile.exists())
				try {
					throw new Exception("grouplist.json n'existe pas");
				} catch (java.lang.Exception e) {
					e.printStackTrace();
				}
			
			FileReader reader;
			try {
				reader = new FileReader(groupListFile);
				JsonObject groups = new JsonParser().parse(reader).getAsJsonObject();
				System.out.println(server.pushGroupList(groups.toString(), login));
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	},
	LOCK_GROUP_LIST ("lock-group-list") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			System.out.println(server.lockGroupList(login));
		}
	},
	CREATE_GROUP ("create-group") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			if (args == null) {
				System.out.println("Votre requete doit contenir l'adresse de multidiffusion.");
				return;
			}
			
			// Obtention de l'adresse de multidiffusion
			String addr = "";
			String emailRegex = "\\w+@[a-zA-Z_]+?\\.[a-zA-Z]{2,3}";
			Pattern regex = Pattern.compile(emailRegex);
			Matcher m = regex.matcher(args);
			if (m.find()) {
				addr = m.group();
			} else {
				System.out.println("Votre requete doit contenir l'adresse de multidiffusion (ex. abc@xyz.com).");
				return;
			}
			
			File groupListFile = new File(userDir, "grouplist.json");

			if (!groupListFile.exists()) {
				System.out.println("Veuillez obtenir la groupe de multidiffusion avec \"./client get-group-list\".");
				return;
			}
			
			FileReader reader;
			try {
				reader = new FileReader(groupListFile);
				JsonObject groups = new JsonParser().parse(reader).getAsJsonObject();
				groups.add(addr,  new JsonArray());
				JsonUtils.writeToFile(groups.toString(), groupListFile);
				System.out.println("Le groupe " + addr + " est cree avec succes.");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	},
	SEND_MAIL ("send") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			if (args == null) {
				System.out.println("Votre courriel doit contenir un sujet et un destinataire.");
				return;
			}
			
			// Obtention du sujet
			String subject = "";
			String subjectRegex = "-s \".*\"";
			Pattern regex = Pattern.compile(subjectRegex);
			Matcher m = regex.matcher(args);
			if (m.find()) {
				subject = m.group().substring(4, m.group().length() - 1);
			} else {
				System.out.println("Votre courriel doit contenir un sujet avec la syntaxe suivante : -s \"VOTRE_SUJET\"");
				return;
			}
			
			// Obtention du destinataire
			String dest = "";
			String tempArgs = String.join(" ", args.split(subjectRegex));
			String emailRegex = "\\w+@[a-zA-Z_]+?\\.[a-zA-Z]{2,3}";
			regex = Pattern.compile(emailRegex);
			m = regex.matcher(tempArgs);
			if (m.find()) {
				dest = m.group();
			} else {
				System.out.println("Votre courriel doit contenir un destinataire.");
				return;
			}
			
			// Écriture du courriel
			System.out.println("Sujet : " + subject);
			System.out.println("Destinataire : " + dest);
			System.out.println("Veuillez rédiger votre courriel. Pour envoyer, tapez \"/send\".");
			String content = "";
			
			do {
				String line = scanner.nextLine();
				
				if (line.equals("/send")) {
					break;
				}
				
				content += line + "\n";
				
			} while (true);
			
			// Envoyer le courriel
			JsonObject email = new JsonObject();
			email.addProperty("from", login);
			email.addProperty("to", dest);
			email.addProperty("subject", subject);
			email.addProperty("content", content);
			System.out.println(server.sendMail(email.toString()));
		}
	},
	LIST_MAIL ("list") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			boolean justUnread = args != null ? args.contains("-u") : false;
			listMails(login, server, justUnread);
		}
	},
	READ_MAIL ("read") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			boolean justUnread = false;
			listMails(login, server, justUnread);
			
			do {
				System.out.print("Lire le courriel no : ");
				try {
					if (scanner.hasNextLine()) {
						int id = Integer.parseInt(scanner.nextLine());
					    JsonObject response = new JsonParser().parse(server.readMail(id, login)).getAsJsonObject();
						
						if (!response.get("result").getAsBoolean()) {
							System.out.println(response.get("content").getAsString());
							return;
						}
						
						System.out.println(response.get("content").getAsString());
						break;
					}
				} catch(Exception e) {
				}
			} while (true);
		}
	},
	DELETE_MAIL ("delete") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			boolean justUnread = false;
			listMails(login, server, justUnread);
			
			do {
				System.out.print("Supprimer le courriel no : ");
				try {
					if (scanner.hasNextLine()) {
						int id = Integer.parseInt(scanner.nextLine());
						System.out.println(server.deleteMail(id, login));
						return;
					}
				} catch(Exception e) {
				}
			} while (true);
		}
	},
	SEARCH_MAIL ("search") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, String args, Scanner scanner) throws RemoteException {
			if (args == null) {
				System.out.println("Veuillez ajouter au moins un mots-cle a votre recherche.");
				return;
			}
			
			JsonObject response = new JsonParser().parse(server.findMail(args, login)).getAsJsonObject();
			
			boolean result = response.get("result").getAsBoolean();
			String content = response.get("content").getAsString();
			
			if (!result) {
				System.out.println(content);
				return;
			}
			
			JsonArray messages = new JsonParser().parse(content).getAsJsonArray();
			
			int mailCount = response.get("mailCount").getAsInt();
			System.out.println(mailCount + " courriels qui correspondent a votre recherche sont trouves.");
			
			for (JsonElement messageJson : messages) {
				JsonObject message = messageJson.getAsJsonObject();
				
				int id = message.get("id").getAsInt();
				String from = message.get("from").getAsString();
				String date = message.get("date").getAsString();
				String subject = message.get("subject").getAsString();
				
				System.out.println(id + "\t" + from + "\t" + date + "\t" + subject);
			}
			
			if (mailCount > 0) {
				do {
					System.out.print("Lire le courriel no : ");
					try {
						if (scanner.hasNextLine()) {
							int id = Integer.parseInt(scanner.nextLine());
						    response = new JsonParser().parse(server.readMail(id, login)).getAsJsonObject();
							
							if (!response.get("result").getAsBoolean()) {
								System.out.println(response.get("content").getAsString());
								return;
							}
							
							System.out.println(response.get("content").getAsString());
							break;
						}
					} catch(Exception e) {
					}
				} while (true);
			}
		}
	};
	
	private String commandName;

	private ShellCmds(String commandName) {
		this.commandName = commandName;
		
	}
	
	public abstract void execute(String login, ServerInterface server, String userDir, String request, Scanner scanner) throws RemoteException;
	
	public static ShellCmds getByName(String cmd) throws IllegalArgumentException {
		for (ShellCmds c : ShellCmds.values()) {
			if (c.commandName.equalsIgnoreCase(cmd))
				return c;
		}
		
		throw new IllegalArgumentException(cmd + " is not a valid command");
	}
	
	private static void listMails(String login, ServerInterface server, boolean justUnread) throws RemoteException {
		JsonObject response = new JsonParser().parse(server.listMails(justUnread, login)).getAsJsonObject();
		
		boolean result = response.get("result").getAsBoolean();
		String content = response.get("content").getAsString();
		
		if (!result) {
			System.out.println(content);
			return;
		}
		
		JsonArray messages = new JsonParser().parse(content).getAsJsonArray();
		int unreadMessages = 0;
		for (JsonElement messageJson : messages) {
			JsonObject message = messageJson.getAsJsonObject();
			
			boolean read = message.get("read").getAsBoolean();
			if (!read)
				unreadMessages++;
		}
		System.out.println(response.get("mailCount").getAsInt() + " courriels dont " + unreadMessages + " sont non-lus.");
		
		for (JsonElement messageJson : messages) {
			JsonObject message = messageJson.getAsJsonObject();
			
			int id = message.get("id").getAsInt();
			boolean read = message.get("read").getAsBoolean();
			String from = message.get("from").getAsString();
			String date = message.get("date").getAsString();
			String subject = message.get("subject").getAsString();
			
			System.out.println(id + "\t" + (read ? "-" : "N") + "\t" + from + "\t" + date + "\t" + subject);
		}
	}

}
