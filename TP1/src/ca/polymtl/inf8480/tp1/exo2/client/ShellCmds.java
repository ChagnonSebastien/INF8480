package ca.polymtl.inf8480.tp1.exo2.client;

import java.rmi.RemoteException;
import java.util.List;

import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public enum ShellCmds {
	GET_GROUP_LIST ("get-group-list") {
		@Override
		public void execute(String login, ServerInterface server, List<String> args) throws RemoteException {
			server.getGroupList(Long.MAX_VALUE);
			System.out.println("OK!");
		}
	},
	COMMAND_2 ("command2") {
		@Override
		public void execute(String login, ServerInterface server, List<String> args) throws RemoteException {
			// TODO Auto-generated method stub
			
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
