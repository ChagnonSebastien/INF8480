package ca.polymtl.inf8480.tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BalancerInterface extends Remote {

	public String computeOperations(String operations) throws RemoteException;
}
