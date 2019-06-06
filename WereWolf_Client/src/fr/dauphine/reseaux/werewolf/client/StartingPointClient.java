package fr.dauphine.reseaux.werewolf.client;

/**
 * 
 * Lancement du Client
 *
 */
public class StartingPointClient {
	public static void main(String[] args) {
		Client.BuildMainWindow();
		Client.Initialize();
		Client.BuildLogInWindow();
	}
}
