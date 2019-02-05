package ca.polymtl.inf8480.tp1.exo2.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.List;

import ca.polymtl.inf8480.tp1.exo2.shared.Hash;
import ca.polymtl.inf8480.tp1.exo2.shared.JsonUtils;
import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public enum ShellCmds {
	GET_GROUP_LIST ("get-group-list") {
		@Override
		public void execute(String login, ServerInterface server, List<String> args) throws RemoteException {
			File groupListFile = new File(System.getProperty("user.home"), "grouplist.json");

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
		public void execute(String login, ServerInterface server, List<String> args) throws RemoteException {
			
			
		}
	};
	
	private String commandName;

	private ShellCmds(String commandName) {
		this.commandName = commandName;
		
	}
	
	public abstract void execute(String login, ServerInterface server, List<String> args) throws RemoteException;
	
	public static ShellCmds getByName(String cmd) throws IllegalArgumentException {
		for (ShellCmds c : ShellCmds.values()) {
			if (c.commandName.equalsIgnoreCase(cmd))
				return c;
		}
		
		throw new IllegalArgumentException(cmd + " is not a valid command");
	}

}
