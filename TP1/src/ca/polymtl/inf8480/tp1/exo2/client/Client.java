package ca.polymtl.inf8480.tp1.exo2.client;

import java.io.File;
import java.nio.file.Paths;
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
	private String clientDir;
	private String userDir;

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
		
		clientDir = Paths.get(System.getProperty("user.dir"), "1734636-1804702-client").toString();
		new File(clientDir).mkdir();
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
			
			this.userDir = Paths.get(clientDir, login).toString();
			new File(userDir).mkdir();
			
			ShellCmds.GET_GROUP_LIST.execute(login, serverStub, this.userDir, null, null);
			
			// Envoi de requêtes
			this.programLoop(scanner);

		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private String openSession(Scanner scanner) throws RemoteException {
		String login;
		String result = null;
		boolean isLoggedIn = false;

		do {
			System.out.println("Veuillez entrer votre nom d'utilisateur:");
			login = scanner.nextLine();
			if (login.isEmpty())
				continue;
			
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

	private void programLoop(Scanner scanner) {
		boolean end = false;
		
		do {

			System.out.printf("\n%s$ ", this.login);
			String request = scanner.nextLine();
			List<String> args = new ArrayList<String>(Arrays.asList(request.split(" ", 3)));
			args.removeAll(Arrays.asList(""));
			
			if (args.size() == 0) {
				continue;
			}
			
			if (!args.get(0).equals("./client")) {
				System.out.printf("bash: %s: commande inexistante\n", args.get(0));
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
				System.out.printf("client: ne peut pas executer '%s': Commande n'existe pas\n", args.get(1));
				continue;
			}
			
			try {
				command.execute(this.login, serverStub, userDir, args.size() > 2 ? args.get(2) : null, scanner);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			

		} while (!end);
		
		scanner.close();
	}

}
