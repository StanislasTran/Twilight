package fr.dauphine.reseaux.werewolf.server;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import fr.dauphine.reseaux.werewolf.server.gameObjects.Role;

public class Server {

	private static final String DEBUT_DU_JEU = "Bienvenue sur le jeu du Loup-Garou! Le jeu peut commencer. Nous entrons dans une nuit noire � Thiercelieux, les villageois dorment profondemment ...\n\n"
			+ "-------------------------------------------------------\n";

	// Dur�e du tour des loup-garous (ms)
	private static final long DUREE_TOUR = 30000;

	JFrame serverGui;
	JTextArea displayWindow;
	private ServerSocket serverSocket;
	private Socket socket;
	public Hashtable<Socket, ObjectOutputStream> outputStreams;
	public Hashtable<String, ObjectOutputStream> clients;

	// Game state

	private Map<String, Role> roleMap;
	private Map<String, String> voteMap;

	// constructor
	public Server(int port) throws IOException {
		// Simple Gui for Server
		serverGui = new JFrame("Server");
		serverGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		serverGui.setSize(500, 500);
		displayWindow = new JTextArea();
		serverGui.add(new JScrollPane(displayWindow), BorderLayout.CENTER);
		serverGui.setVisible(true);

		outputStreams = new Hashtable<Socket, ObjectOutputStream>();
		clients = new Hashtable<String, ObjectOutputStream>();

		roleMap = new HashMap<>();
		voteMap = new HashMap<>();

		serverSocket = new ServerSocket(port);
		showMessage("Waiting for clients at " + serverSocket);
	}

	// Waiting for clients to connect
	public void waitingForClients() throws IOException, ClassNotFoundException {

		while (true) {
			socket = serverSocket.accept();

			new ServerThread(this, socket);
		}
	}

	// displaying message on Server Gui
	public void showMessage(final String message) {
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				displayWindow.append(message);
			}

		});
	}

	// Sending a message to all the available clients
	public void sendTo(String username) throws IOException {

		clients.get(username).writeObject(roleMap.get(username));
	}

	// Sending a message to all the available clients
	public void sendToAll(Object data) throws IOException {

		for (Enumeration<ObjectOutputStream> e = getOutputStreams(); e.hasMoreElements();) {
			// since we don't want server to remove one client and at the same time sending
			// message to it
			synchronized (outputStreams) {
				ObjectOutputStream tempOutput = e.nextElement();
				tempOutput.writeObject(data);
				tempOutput.flush();
			}
		}
	}

	// To get Output Stream of the available clients from the hash table
	private Enumeration<ObjectOutputStream> getOutputStreams() {
		// TODO Auto-generated method stub
		return outputStreams.elements();
	}

	// Sending private message
	public void sendPrivately(String username, String message) throws IOException {
		// TODO Auto-generated method stub
		ObjectOutputStream tempOutput = clients.get(username);
		tempOutput.writeObject(message);
		tempOutput.flush();
	}

	// Removing the client from the client hash table
	public void removeClient(String username) throws IOException {

		synchronized (clients) {
			clients.remove(username);
			sendToAll("!" + clients.keySet());
		}
	}

	// Removing a connection from the outputStreams hash table and closing the
	// socket
	public void removeConnection(Socket socket, String username) throws IOException {

		synchronized (outputStreams) {
			outputStreams.remove(socket);
		}

		// Printing out the client along with the IP offline in the format of
		// ReetAwwsum(123, 12, 21, 21) is offline
		showMessage("\n" + username + "(" + socket.getInetAddress().getHostAddress() + ") is offline");

	}

	/********************
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */

	// GAME METHODS

	public void START_GAME() throws InterruptedException {
		try {
			Initiate_Role();
			sendToAll("@Narrator;" + DEBUT_DU_JEU);
			Thread.sleep(2000);

			while (!gameFinished()) {
				sendToAll("@Narrator;"
						+ "Les loups-garous se r�veillent et choisissent leur cible ('/vote PSEUDO' pour voter contre la cible)");
				Thread.sleep(DUREE_TOUR);

				String eliminatedPlayer = eliminatedPlayer();
				roleMap.remove(eliminatedPlayer);
				sendToAll("@Narrator;" + "Le jour se l�ve: " + eliminatedPlayer + " est mort :( !");
			}

			if (winner().equals(Role.LOUPGAROU)) {
				sendToAll("@Narrator;Les loups-garous ont gagn� !");
			} else {
				sendToAll("@Narrator;Les villageois ont gagn� !");
			}
		} catch (IOException e) {

		}
	}

	private boolean gameFinished() {
		Role winner = winner();
		if (winner == null)
			return false;
		else
			return true;
	}

	private Role winner() {
		boolean werewolfWins = true;
		boolean villajoisWins = true;

		for (Role role : roleMap.values()) {
			if (role.equals(Role.VILLAGEOIS)) {
				werewolfWins = false;
			} else if (role.equals(Role.LOUPGAROU)) {
				villajoisWins = false;
			}
		}
		if (werewolfWins)
			return Role.LOUPGAROU;
		else if (villajoisWins)
			return Role.VILLAGEOIS;
		else
			return null;
	}

	private void Initiate_Role() throws IOException {
		int nbPlayer = clients.size();
		Set<String> players = clients.keySet();

		LinkedList<Role> roles = new LinkedList<>();

		switch (nbPlayer) {
		case 2:
			roles.add(Role.LOUPGAROU);
			roles.add(Role.VILLAGEOIS);
			break;
		case 3:
			roles.add(Role.LOUPGAROU);
			roles.add(Role.VILLAGEOIS);
			roles.add(Role.VILLAGEOIS);
			break;
		}

		Collections.sort(roles);

		for (String player : players) {
			roleMap.put(player, roles.removeFirst());
		}

		// send to all player their role
		for (String player : clients.keySet()) {
			sendPrivately(player, "@Role;" + roleMap.get(player).toString());
		}
	}

	public void addRole(String userName, Role role) {
		roleMap.put(userName, role);
	}

	public Role getRole(String userName) {
		return roleMap.get(userName);
	}

	public void vote(String username, String vote) {
		voteMap.put(username, vote);

	}

	public String eliminatedPlayer() {
		Map<String, Integer> vote = new HashMap<>();

		for (String name : voteMap.values()) {
			if (vote.containsKey(name)) {
				vote.put(name, vote.get(name) + 1);
			} else {
				vote.put(name, 1);
			}
		}

		int max = 0;
		String eliminated = "";

		for (String name : vote.keySet()) {
			if (vote.get(name) > max) {
				max = vote.get(name);
				eliminated = name;
			}
		}
		return eliminated;
	}

}
