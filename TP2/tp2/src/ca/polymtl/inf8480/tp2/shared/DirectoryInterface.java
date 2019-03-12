/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP2 - INF8480
 */

package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DirectoryInterface extends Remote {

	public String logServer(String hostname) throws RemoteException;
	
	public String getServers() throws RemoteException;

	public boolean authenticateBalancer(String login, String password) throws RemoteException;
	
}
