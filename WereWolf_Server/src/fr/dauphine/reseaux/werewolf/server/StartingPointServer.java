package fr.dauphine.reseaux.werewolf.server;

import java.io.IOException;

/**
 * 
 * Lancement du Serveur
 *
 */
public class StartingPointServer {

	public static void main(String[] args) throws IOException, ClassNotFoundException {

		Server server = new Server(2620);
		server.waitingForClients();

	}
}
