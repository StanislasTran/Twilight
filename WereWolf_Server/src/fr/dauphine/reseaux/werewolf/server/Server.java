package fr.dauphine.reseaux.werewolf.server;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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

	private static final String DEBUT_DU_JEU = "Bienvenue sur le jeu du Loup-Garou! Le jeu peut commencer. \n Nous entrons dans une nuit noire  Thiercelieux, les villageois dorment profondemment ...\n\n"
			+ "-------------------------------------------------------\n";

	// Durée du tour des loup-garous (ms)
	private static final long DUREE_TOUR = 30000;

	JFrame serverGui;
	JTextArea displayWindow;
	private ServerSocket serverSocket;
	private Socket socket;

	public Hashtable<Socket, ObjectOutputStream> outputStreams;

	// contain all users connected to the server
	public Hashtable<String, ObjectOutputStream> clients;

	// contain users in the roomSelection
	public Set<String> roomSelection;
	private Map<String, Room> rooms;

	// Game state

	private Map<String, Role> roleMap;
	private Map<String, String> voteMap;
	private Role roleTurn;

	private boolean playerSaved = false;
	private boolean seerPower = false; // seer = voyante
	private boolean witchPower = false; // witch = sorciere

	private boolean witchSavePower = true;
	private boolean witchKillPower = true;
	private String playerWitchToKill = "";

	private ArrayList<String> playersAlive;
	private LinkedList<String> playersDead;
	// private ArrayList<String> playersDead;

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
		roomSelection = new HashSet<>();

		roleMap = new HashMap<>();
		voteMap = new HashMap<>();
		rooms = new HashMap<>();
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

	/*
	 *
	 *
	 * sending message methods
	 *
	 *
	 */

	/**
	 * sending a message to all available clients in the selectionRoom
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void sendToSelectionRoom(Object data) throws IOException {
		for (String userName : roomSelection) {
			synchronized (roomSelection) {
				ObjectOutputStream tempOutput = clients.get(userName);
				tempOutput.writeObject(data);
				tempOutput.flush();

			}
		}
	}

	// Sending a message to all the available clients
	public void adminSendsToPlayer(String username, String message) throws IOException {

		clients.get(username).writeObject(message);
	}

	/**
	 * send the data to all user in the Room room
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void sendToRoom(Room room, Object data) throws IOException {
		for (String userName : room.getUsers()) {
			synchronized (room) {
				ObjectOutputStream tempOutput = clients.get(userName);
				tempOutput.writeObject(data);
				tempOutput.flush();

			}
		}
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
				System.out.println("msg send" + data.toString());

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

	/**
	 * @param location
	 *            ******************
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

	public void startGame(Room location) throws InterruptedException {

		seerPower = true;// gerer si la voyante a utilisé son pouvoir
		witchPower = true;// gerer si la voyante a utilisé ses pouvoirs
		try {
			playersDead = new LinkedList<>();
			InitiateRole();
			initiateListPlayerAlive();
			sendToRoom(location, "@Narrator;" + DEBUT_DU_JEU);
			Thread.sleep(2000);

			boolean first_turn = true;
			Map<String, Integer> playersVotedTurn = new HashMap<>();

			while (!gameFinished()) {
				if (!first_turn) {
					sendToRoom(location, "@Narrator;"
							+ "De suite les villageois se concertent et décident de voter pour désigner un coupable ('/vote PSEUDO' pour voter contre la cible)");
					Thread.sleep(DUREE_TOUR);

				}
				this.roleTurn = Role.WOLF;

				sendToRoom(location, "@Narrator;"
						+ "Les loups-garous se reveillent et choisissent leur cible ('/vote PSEUDO' pour voter contre la cible)");
				Thread.sleep(DUREE_TOUR);

				String eliminatedPlayer = eliminate(playersVotedTurn);

				if (location.getUsers().size() > 3) {
					this.roleTurn = Role.WITCH;
					sendToRoom(location, "@Narrator;" + "La sorcière se réveille");
					// envoyer un MP pour lui dire qui est mort

					eliminatedPlayer = sendDeadPlayerToWitch();

					witchKillManagement();

					sendToRoom(location, "@Narrator;" + "La sorciere retourne dormir");
				}

				if (location.getUsers().size() > 4) {

					this.roleTurn = Role.SEER;

					sendToRoom(location, "@Narrator;" + "La Voyante se réveille");

					sendToRoom(location, "@Narrator;" + "Voyante choisissez le joueur dont vous voulez voir la carte");
					// à implémenter aussi
				}

				Thread.sleep(1);
				roleMap.remove(eliminatedPlayer);
				sendToRoom(location,
						"@Narrator;" + "Le jour se lève: les vilageois se réveillent et découvrent avec effroi que"
								+ eliminatedPlayer + " est mort :( !");

				System.out.println("Alive " + playersAlive);
				System.out.println("Dead " + playersDead);
				voteMap.clear();

			}

			if (winner().equals(Role.WOLF)) {
				sendToRoom(location, "@Narrator;Les loups-garous ont gagné !");
			} else {
				sendToRoom(location, "@Narrator;Les villageois ont gagné !");
			}
		} catch (IOException e) {

		}
	}

	/****
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 	
	 *  */

	// ROOM METHOD

	/**
	 * create a room
	 * 
	 * @param name
	 * @param user
	 * @param maxSize
	 * @throws IOException
	 */
	public void createRoom(String name, String user, String maxSize) throws IOException {

		this.rooms.put(name, new Room(name, user, Integer.parseInt(maxSize)));
		System.out.println(rooms.keySet().toString());
		roomSelection.remove(user);

		sendToSelectionRoom("ROOM" + rooms.keySet().toString());
		sendToRoom(rooms.get(name), rooms.get(name).userKey());

		System.out.println("ok");

	}

	public void menu() throws InterruptedException {

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
			if (role.equals(Role.VILLAGER)) {
				werewolfWins = false;
			} else if (role.equals(Role.WOLF)) {
				villajoisWins = false;
			}
		}
		if (werewolfWins)
			return Role.WOLF;
		else if (villajoisWins)
			return Role.VILLAGER;
		else
			return null;
	}

	private void InitiateRole() throws IOException {
		int nbPlayer = clients.size();
		Set<String> players = clients.keySet();

		LinkedList<Role> roles = new LinkedList<>();

		if (nbPlayer == 2) {

			roles.add(Role.WOLF);
			roles.add(Role.VILLAGER);
		}
		if (nbPlayer == 3) {

			roles.add(Role.WOLF);
			roles.add(Role.VILLAGER);
			roles.add(Role.VILLAGER);
		}

		if (nbPlayer >= 4 && nbPlayer < 7) {

			int nbVillageois = nbPlayer - 3;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			roles.add(Role.WOLF);
			roles.add(Role.WITCH);
			roles.add(Role.VILLAGER);

		}

		if (nbPlayer >= 7 && nbPlayer <= 11) {

			int nbVillageois = nbPlayer - 4;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			roles.add(Role.WOLF);
			roles.add(Role.VILLAGER);
			roles.add(Role.WITCH);
			roles.add(Role.VILLAGER);

		}

		Collections.sort(roles);

		for (String player : players) {
			System.out.println(roles.getFirst());
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

	public void vote(String usernameVoter, String playerVoted) throws IOException {
		if (playersAlive.contains(playerVoted)) {
			voteMap.put(usernameVoter, playerVoted);
		} else {
			sendPrivately(usernameVoter, "@Narrator;" + "Player already dead ! Choose another player.");
		}

	}

	public void initiateListPlayerAlive() {
		playersAlive = new ArrayList<String>();
		for (String player : roleMap.keySet()) {
			playersAlive.add(player);
		}
	}

	public String eliminate(Map<String, Integer> playersVotedTurn) {

		for (String name : voteMap.values()) {

			if (playersVotedTurn.containsKey(name)) {
				playersVotedTurn.put(name, playersVotedTurn.get(name) + 1);
			} else {
				playersVotedTurn.put(name, 1);
			}

		}

		int max = 0;
		String eliminated = "";

		for (String name : playersVotedTurn.keySet()) {
			if (playersVotedTurn.get(name) > max) {
				max = playersVotedTurn.get(name);
				eliminated = name;
			}
		}
		playersDead.add(eliminated);
		return eliminated;
	}

	public String sendDeadPlayerToWitch() throws IOException, InterruptedException {
		String deadPlayer = "";
		for (String player : roleMap.keySet()) {
			if (roleMap.get(player).equals(Role.WITCH) && witchSavePower) {
				sendPrivately(player, "@Narrator;" + "This player is dead. Do you want to save someone ?");
				sendPrivately(player, "@Narrator;" + playersDead.getLast());
				Thread.sleep(DUREE_TOUR - 15000);
				if (playerSaved) {
					sendPrivately(player, "@Narrator;" + "Player saved");
				} else {
					sendPrivately(player, "@Narrator;" + "Player killed");
					deadPlayer = playersDead.getLast();
				}

			}

		}
		return deadPlayer;

	}

	public void resultWitchSave(String vote) {
		playerSaved = false;
		if (vote.equals("yes")) {
			playersDead.remove(playersDead.getLast());
			playerSaved = true;
			witchSavePower = false;
		} else if (vote.equals("no")) {
			playersAlive.remove(playersDead.getLast());
			playerSaved = false;
		}
	}

	public void witchKillManagement() throws IOException, InterruptedException {
		for (String player : roleMap.keySet()) {
			if (roleMap.get(player).equals(Role.WITCH) && witchKillPower) {
				sendPrivately(player, "@Narrator;"
						+ "You still have your killing power. Do you kill to save someone ? (Write yes or no, if yes, write the player name after a space");
				Thread.sleep(DUREE_TOUR - 15000);
				if (!playerWitchToKill.isEmpty() && playersAlive.contains(playerWitchToKill)) {
					sendPrivately(player, "@Narrator;" + "Player killed");
				} else {
					sendPrivately(player, "@Narrator;" + "Player isn't alive");
				}

			}
		}
	}

	public void resultWitchKill(String vote, String playername) {
		if (vote.equals("yes")) {
			playerWitchToKill = playername;
			playersAlive.remove(playerWitchToKill);
			witchKillPower = false;
		} else if (vote.equals("no")) {
		}
	}

	/**
	 * Join the room entered in parameter
	 * 
	 * @param roomName
	 * @throws IOException
	 */
	public void joinRoom(String userName, String roomName) throws IOException {
		System.out.println("entre dans le join");
		rooms.get(roomName).addUsers(userName);
		roomSelection.remove(userName);
		sendToRoom(rooms.get(roomName), rooms.get(roomName).userKey());

		System.out.println("sort du join");
	}

	/**
	 * @return the roomSelection
	 */
	public Set<String> getRoomSelection() {
		return roomSelection;
	}

	/**
	 * @return the rooms
	 */
	public Map<String, Room> getRooms() {
		return rooms;
	}

	/**
	 * @param roomSelection
	 *            the roomSelection to set
	 */
	public void setRoomSelection(Set<String> roomSelection) {
		this.roomSelection = roomSelection;
	}

	/**
	 * @param rooms
	 *            the rooms to set
	 */
	public void setRooms(Map<String, Room> rooms) {
		this.rooms = rooms;
	}

}
