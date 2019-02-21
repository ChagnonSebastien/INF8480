package ca.polymtl.inf8480.tp2.balancer;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;

import ca.polymtl.inf8480.tp2.shared.BalancerInterface;

public class Balancer extends RemoteServer implements BalancerInterface {
	
	private static final long serialVersionUID = -3221999619303495281L;

	public static void main(String[] args) {
		Balancer balancer = new Balancer();
		balancer.run();
	}

	public Balancer() {
		super();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			BalancerInterface stub = (BalancerInterface) UnicastRemoteObject.exportObject(this, 5000);
			Registry registry = LocateRegistry.getRegistry(5001);
			registry.rebind("balancer", stub);
			System.out.println("Balancer ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lance ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
		
	}

	@Override
	public String computeOperations(String ops) throws RemoteException {
		return "weeeeee";
	}

}
