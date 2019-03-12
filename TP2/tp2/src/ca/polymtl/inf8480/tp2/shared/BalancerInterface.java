/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP2 - INF8480
 */

package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

//Point d'entree du systeme au niveau du serveur
//Repartit les operations parmi les serveurs de calcul
public interface BalancerInterface extends Remote {
	public String computeOperations(String operations) throws RemoteException;
}
