package ca.polymtl.inf8480.tp1.exo2.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp1.exo2.shared.Hash;
import ca.polymtl.inf8480.tp1.exo2.shared.JsonUtils;
import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public enum ShellCmds {
	GET_GROUP_LIST ("get-group-list") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, List<String> args) throws RemoteException {
			File groupListFile = new File(userDir, "grouplist.json");

			String checksum = "";
			if (groupListFile.exists()) {
				checksum = Hash.MD5.checksum(groupListFile);
			}
			
			String groups = server.getGroupList(checksum);
			if (groups == null) {
				System.out.println("Vous avez deja la derniere version de la liste de groupes");
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
			
			JsonUtils.writeToFile(groups, groupListFile);
			System.out.println("La liste des groupes a ete mise a jour");
			
		}
	},
	PUSH_GROUP_LIST ("publish-group-list") {
		@Override
		public void execute(String login, ServerInterface server, String userDir, List<String> args) throws RemoteException {
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
		public void execute(String login, ServerInterface server, String userDir, List<String> args) throws RemoteException {
			System.out.println(server.lockGroupList(login));
		}
	};
	
	private String commandName;

	private ShellCmds(String commandName) {
		this.commandName = commandName;
		
	}
	
	public abstract void execute(String login, ServerInterface server, String userDir, List<String> args) throws RemoteException;
	
	public static ShellCmds getByName(String cmd) throws IllegalArgumentException {
		for (ShellCmds c : ShellCmds.values()) {
			if (c.commandName.equalsIgnoreCase(cmd))
				return c;
		}
		
		throw new IllegalArgumentException(cmd + " is not a valid command");
	}

}
