package ca.polymtl.inf8480.tp1.exo2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

public interface ServerInterface extends Remote {

	public String openSession(String login, String password) throws RemoteException;
	
	public String getGroupList(long checksum) throws RemoteException;
	
}
