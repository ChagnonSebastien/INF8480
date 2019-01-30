package ca.polymtl.inf8480.tp1.exo2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

public interface ServerInterface extends Remote {

	
	public boolean openSession(String login, String password) throws RemoteException;
	
}
