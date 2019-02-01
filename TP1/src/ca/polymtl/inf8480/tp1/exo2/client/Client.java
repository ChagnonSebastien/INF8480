package ca.polymtl.inf8480.tp1.exo2.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public class Client {
	public static void main(String[] args) {
		String distantHostname = null;

		if (args.length > 0) {
			// TODO remettre
			// distantHostname = args[0];
		}

		Client client = new Client(distantHostname);
		client.run();
	}

	private ServerInterface serverStub = null;
	private String login;

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		if (distantServerHostname != null) {
			serverStub = loadServerStub(distantServerHostname);
		} else {
			serverStub = loadServerStub("127.0.0.1");
		}
	}

	private void run() {
		if (serverStub != null) {
			appelRMI();
		}
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private void appelRMI() {
		try (Scanner scanner = new Scanner(System.in)) {
			// Ouverture de session
			this.login = openSession(scanner);

			// Envoi de requêtes
			this.programLoop();

		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private String openSession(Scanner scanner) throws RemoteException {
		String login;
		String result;
		boolean isLoggedIn = false;

		do {
			System.out.println("Veuillez entrer votre nom d'utilisateur:");
			login = scanner.nextLine();
			System.out.println("Veuillez entrer votre mot de passe:");
			String password = scanner.nextLine();

			result = serverStub.openSession(login, password);
			isLoggedIn = result.equals(login);

			if (isLoggedIn) {
				System.out.println("Bienvenue dans votre boite a courriel " + login);
			} else {
				System.out.println("Erreur lors de la connection");
			}

		} while (!isLoggedIn);

		return result;
	}

	private void programLoop() {
		Scanner scanner = new Scanner(System.in);
		boolean end = false;
		do {

			System.out.printf("\n%s$ ", this.login);
			String request = scanner.nextLine();
			List<String> args = new ArrayList<String>(Arrays.asList(request.split(" ")));
			args.removeAll(Arrays.asList(""));
			
			if (args.size() == 0) {
				continue;
			}
			
			if (!args.get(0).equals("./client")) {
				System.out.printf("bash: %s: command not found\n", args.get(0));
				continue;
			}
			
			if (args.size() == 1) {
				// Display help
				continue;
			}
			
			ShellCmds command;
			try {
				command = ShellCmds.getByName(args.get(1));
			} catch (IllegalArgumentException e) {
				System.out.printf("client: cannot execute '%s': Command dos not exist\n", args.get(1));
				continue;
			}
			
			try {
				command.execute(this.login, serverStub, args);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			

		} while (!end);
		
		scanner.close();
	}

}
