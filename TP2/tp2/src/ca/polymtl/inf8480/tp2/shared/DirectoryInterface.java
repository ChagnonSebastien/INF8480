package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DirectoryInterface extends Remote {

	public String logServer(String hostname) throws RemoteException;
	
}
