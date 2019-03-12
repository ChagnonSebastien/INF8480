/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP2 - INF8480
 */

package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

//Execute les operations transmis par le repartiteur
//Le serveur peut �tre configurer pour accepter un nombre maximale de requ�tes ainsi que son taux de reponses malicieuses
public interface ServerInterface extends Remote {
	
	public String compute(String request) throws RemoteException;

}
