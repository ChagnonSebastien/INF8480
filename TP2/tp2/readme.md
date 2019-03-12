#INF8480 - Systèmes répartis et infonuagique 
##TP2 - Services distribués et gestion des pannes
<span> 
1734636 - Pierre To
<br />
1804702 - Sébastien Chagnon
</span>

### A) Configurations des serveurs
1. Ouvrir le fichier server-config.json sur le poste du service de répertoire de noms
2. Pour chaque serveur nécessaire
	2i. Sur une console du laboratoire, tapez 'ssh L4712-XX' où XX représente un poste du laboratoire
	2ii. Une fois sur le poste, tapez 'ifconfig' pour trouver l'adresse IP du poste (ex. 132.207.12.34)
	2iii. Mettre à jour l'adresse IP d'un serveur sur fichier server-config.json
	Exemple de mise à jour :
	"132.207.12.34": {
		"port": 5003,
		"falseAnswerRatio": 0.0,
		"q": 5
	}
	où la clé est l'adresse IP d'un serveur, port est le numéro du port, falseAnswerRatio est le mode de fonctionnement du serveur (0.0 : serveur de bonne foi, 1.0 : serveur toujours malicieux), q est la capacité du serveur de calcul
	2iv. Attribuer un port différent entre 5002 à 5048 pour ce serveur

### B) Démarrage du service de répertoire de noms
1. Aller dans le dossier tp2/
2. Faire 'ant' pour compiler
3. Sur une autre console, aller dans le dossier tp2/bin/
4. Partir le registre rmi avec 'rmiregistry 5000'
5. Sur une autre console, aller dans le dossier tp2/
6. Tapez 'ifconfig' pour trouver l'adresse IP du poste courant
7. Partir le service de répertoire de noms avec './directory.sh x.x.x.x' où x.x.x.x représente l'adresse IP du service de répertoire de noms

### C) Démarrage du répartiteur
1. Sur le même poste que le service de répertoire de noms, aller dans le dossier tp2/ (déjà compilé et registre rmi déjà parti)
2. Tapez 'ifconfig' pour trouver l'adresse IP du poste courant (devrait être la même que le service de répertoire de noms)
3. Partir le répartiteur de noms avec './balancer.sh z x.x.x.x y.y.y.y' où z représente le mode (sécurisé : true, non sécurisé : false), x.x.x.x représente l'adresse IP du répartiteur (trouvé à l'étape C2) et y.y.y.y représente le service de répertoire de noms (trouvé à l'étape B6)

### D) Démarrage d'un serveur de calcul (reproduire ces étapes pour chaque serveur de calcul)
1. Sur le poste d'un serveur de calcul (parti avec ssh)
2. Aller dans le dossier tp2/
3. Faire 'ant' pour compiler
4. Sur une autre console, aller dans le dossier tp2/bin/
5. Partir le registre rmi avec 'rmiregistry 5000'
6. Tapez 'ifconfig' pour trouver l'adresse IP du poste courant
7. Partir le serveur de calcul avec './server.sh x.x.x.x y.y.y.y' où x.x.x.x représente l'adresse IP du serveur (trouvé à l'étape D6) et y.y.y.y représente le service de répertoire de noms (trouvé à l'étape B6)

### E) Démarrage du client
1. Sur le même poste que le répartiteur, aller dans le dossier tp2/ (déjà compilé et registre rmi déjà parti)
2. Partir le client de noms avec './client.sh x y.y.y.y' où x représente le nom du fichier d'opérations (ex. operations-216) et y.y.y.y représente l'adresse IP du répartiteur (trouvé à l'étape C2)