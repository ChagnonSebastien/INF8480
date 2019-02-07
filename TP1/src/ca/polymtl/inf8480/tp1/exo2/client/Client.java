/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP1 - INF8480
 */

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ca.polymtl.inf8480.tp1.exo2.shared.ServerInterface;

public class Client {
	public static void main(String[] args) {
		String distantHostname = null;

		if (args.length > 0) {
			distantHostname = args[0];
		}

		Client client = new Client(distantHostname);
		
		// Prévention de la fermeture non desiree du client
		Runtime.getRuntime().addShutdownHook(
			new Thread() {
				@Override
				public void run() {
					if (client.serverStub != null && client.login != null) {
						try {
							client.serverStub.disconnectSession(client.login);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
			}
		);
		
		client.run();
	}

	// Attributs
	private ServerInterface serverStub = null;
	private String login = null;
	private String clientDir = null; // repertoire du client sur son ordinateur
	private String userDir = null; // repertoire de l'utilisateur (courriel)

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		if (distantServerHostname != null) {
			serverStub = loadServerStub(distantServerHostname);
		} else {
			serverStub = loadServerStub("127.0.0.1"); // serveur local
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

	/*
	 * Appel a distance avec Java RMI
	 */
	private void appelRMI() {
		try (Scanner scanner = new Scanner(System.in)) {
			do {
				// Ouverture de session
				this.login = openSession(scanner);
				
				this.userDir = Paths.get(clientDir, login).toString();
				new File(userDir).mkdir();
				
				ShellCmds.GET_GROUP_LIST.execute(login, serverStub, this.userDir, null, null);
				
				// Envoi de requêtes
				this.programLoop(scanner);
			} while(true);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Ouverture d'une session d'un utilisateur
	 */
	private String openSession(Scanner scanner) throws RemoteException {
		String login;
		JsonObject result = null;
		boolean isLoggedIn = false;

		do {
			System.out.println("Veuillez entrer votre nom d'utilisateur:");
			login = scanner.nextLine();
			if (login.isEmpty())
				continue;
			
			System.out.println("Veuillez entrer votre mot de passe:");
			String password = scanner.nextLine();

			result = new JsonParser().parse(serverStub.openSession(login, password)).getAsJsonObject();
			isLoggedIn = result.get("result").getAsBoolean();
			System.out.println(result.get("content").getAsString());
		} while (!isLoggedIn);

		return result.get("login").getAsString();
	}

	/*
	 * Envoi de requêtes au serveur
	 */
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
				System.out.printf("bash: %s: commande inexistante. Vos commandes doivent tous commencer par \"./client\".\n", args.get(0));
				continue;
			}
			
			// Afficher l'aide
			if (args.size() == 1 || args.get(1).equals("help")) {
				System.out.println("Commandes du systeme \"./client\" de courriel :");
				System.out.println("disconnect : deconnecte l'utilisateur actif");
				System.out.println("get-group-list : recupere la liste de groupes globale du serveur");
				System.out.println("lock-group-list : verrouille la liste de groupe globale pour mise-a-jour");
				System.out.println("publish-group-list : met a jour la liste de groupes globale du serveur");
				System.out.println("create-group abc@xyz.co : ajoute l'adresse de multidiffusion abc@xyz.co sans utilisateur");
				System.out.println("join-group abc@xyz.co -u 123@poly.ca : ajoute l'adresse 123@poly.ca au groupe de multidiffusion abc@xyz.co");
				System.out.println("send -s \"SUJET\" abc@xyz.co : envoie un courriel avec le sujet SUJET a abc@xyz.co");
				System.out.println("list : affiche la liste de tous les courriels");
				System.out.println("list -u : affiche la liste de courriels non lus");
				System.out.println("read : lire le contenu d'un courriel avec son identifiant");
				System.out.println("delete : supprime le courriel avec son identifiant");
				System.out.println("search mot1 mot2 : affiche les courriels dont le contenu contient les mots mot1 et mot2");
				continue;
			}
			
			// Deconnexion du client
			if (args.get(1).equals("disconnect")) {
				try {
					System.out.println(serverStub.disconnectSession(login));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				this.login = null;
				break;
			}
			
			// Commande du terminal a envoyer au serveur
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
	}

}
