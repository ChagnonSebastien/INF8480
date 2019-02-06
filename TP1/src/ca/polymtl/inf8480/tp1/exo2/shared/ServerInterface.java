package ca.polymtl.inf8480.tp1.exo2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {

	public String openSession(String login, String password) throws RemoteException;
	
	public String getGroupList(String checksum, String login) throws RemoteException;

	public String pushGroupList(String groupsDef, String login) throws RemoteException;

	public String lockGroupList(String login) throws RemoteException;
	
	public String sendMail(String email) throws RemoteException;
	
	public String listMails(boolean justUnread, String login) throws RemoteException;
	
	public String readMail(int id, String login) throws RemoteException;
	
	public String deleteMail(int id, String login) throws RemoteException;
	
}
