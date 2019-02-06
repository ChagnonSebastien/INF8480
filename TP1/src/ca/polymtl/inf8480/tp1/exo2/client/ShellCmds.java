package ca.polymtl.inf8480.tp1.exo2.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			if (groupListFile.exists()) {
				checksum = Hash.MD5.checksum(groupListFile);
			}
			
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

}
