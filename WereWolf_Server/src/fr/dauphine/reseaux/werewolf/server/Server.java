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

	// DurÃ©e du tour des loup-garous (ms)
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
	/**
	 * Mapping between room name and Room (list of users in the room)
	 */
	private Map<String, Room> rooms;

	// Game state

	/**
	 * Mapping between room name and the role per player in this room (represented with a map)
	 */
	private Map<String,Map<String, Role>> roleMap;
	/**
	 * Mapping between room name and the vote per player in this room (represented with a map)
	 */
	private Map<String, Map<String, String>> voteMap;
	/**
	 * Mapping between room name and the role which is playing in this room
	 */
	private Map<String,Role> roleTurn;

	private Map<String,Boolean> playerSaved = new HashMap<>();
	private Map<String,Boolean> seerPower = new HashMap<>(); // seer = voyante ; // gerer si la voyante a utilisé son pouvoir
	private Map<String,Boolean> witchPower = new HashMap<>(); // witch = sorciere ; // gerer si la sorcière a utilisé son pouvoir

	private Map<String,Boolean> witchSavePower = new HashMap<>();
	private Map<String,Boolean> witchKillPower = new HashMap<>();
	/**
	 * Mapping between room name and the player the witch want to kill
	 */
	private Map<String,String> playerWitchToKill = new HashMap<>();

	private Map<String,ArrayList<String>> playersAlive = new HashMap<>();
	private Map<String,LinkedList<String>> playersDead = new HashMap<>();

	// constructor
	public Server(int port) throws IOException {
		// Simple Gui for Server
		serverGui = new JFrame("Server");
		serverGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		serverGui.setSize(500, 500);
		displayWindow = new JTextArea();
		serverGui.add(new JScrollPane(displayWindow), BorderLayout.CENTER);
		serverGui.setVisible(true);

		outputStreams = new Hashtable<>();
		clients = new Hashtable<>();
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
		try(ObjectOutputStream tempOutput = clients.get(username)) {
			tempOutput.writeObject(message);
			tempOutput.flush();
			
			tempOutput.close();
		}
		
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
	public void removeConnection(Socket socket, String username) {

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

		String roomName = location.getName();
		
		seerPower.put(location.getName(), true);
		witchPower.put(location.getName(), true); 
		try {
			playersDead.put(roomName, new LinkedList<String>());
			
			InitiateRole(roomName);
			initiateListPlayerAlive(roomName);
			
			sendToRoom(location, "@Narrator;" + DEBUT_DU_JEU);
			
			Thread.sleep(2000);

			Map<String, Integer> playersVotedTurn = new HashMap<>();

			boolean first_turn = true;

			while (!gameFinished(roomName)) {
				if (!first_turn) {
					sendToRoom(location, "@Narrator;"
							+ "De suite les villageois se concertent et decident de voter pour dÃ©signer un coupable ('/vote PSEUDO' pour voter contre la cible)");
					Thread.sleep(DUREE_TOUR);

				}
				
				roleTurn.put(roomName, Role.WOLF);

				sendToRoom(location, "@Narrator;"
						+ "Les loups-garous se reveillent et choisissent leur cible ('/vote PSEUDO' pour voter contre la cible)");
				
				Thread.sleep(DUREE_TOUR);

				String eliminatedPlayer = eliminate(roomName,playersVotedTurn);

				if (location.getUsers().size() > 3) {
					roleTurn.put(roomName, Role.WITCH);
					sendToRoom(location, "@Narrator;" + "La sorciere se reveille");
					
					//Envoie un MP pour lui dire qui est mort
					eliminatedPlayer = sendDeadPlayerToWitch(roomName);

					witchKillManagement(roomName);

					sendToRoom(location, "@Narrator;" + "La sorciere retourne dormir");
				}

				if (location.getUsers().size() > 4) {

					roleTurn.put(roomName, Role.SEER);

					sendToRoom(location, "@Narrator;" + "La Voyante se reveille");
					sendToRoom(location, "@Narrator;" + "Voyante choisissez le joueur dont vous voulez voir la carte");
					//TODO VOYANTE à implementer
					
				}

				Thread.sleep(1);
				
				roleMap.remove(eliminatedPlayer);
				sendToRoom(location,
						"@Narrator;" + "Le jour se leve: les villageois se reveillent et decouvrent avec effroi que "
								+ eliminatedPlayer + " est mort... !");

				System.out.println("Alive " + playersAlive);
				System.out.println("Dead " + playersDead);
				voteMap.clear();

			}

			if (winner(roomName).equals(Role.WOLF)) {
				sendToRoom(location, "@Narrator;Les loups-garous ont gagne !");
			} else {
				sendToRoom(location, "@Narrator;Les villageois ont gagne ! Vive le village de Thiercelieux");
			}
		} catch (IOException e) {
			e.printStackTrace();
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

	public void menu()  {
		//nothing
	}

	private boolean gameFinished(String roomName) {
		Role winner = winner(roomName);
		if (winner == null) {
			return false;
		}
			return true;
	}
	

	private Role winner(String roomName) {
		boolean werewolfWins = true;
		boolean villajoisWins = true;

		for (Role role : roleMap.get(roomName).values()) {
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

	private void InitiateRole(String roomName) throws IOException {
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
			roleMap.put(roomName, new HashMap<String,Role>());
			addRole(roomName,player,roles.getFirst());
		}

		// send to all player their role
		for (String player : clients.keySet()) {
			sendPrivately(player, "@Role;" + roleMap.get(player).toString());
		}
	}

	public void addRole(String roomName, String userName, Role role) {
		roleMap.get(roomName).put(userName, role);
	}

	public Role getRole(String roomName, String userName) {
		return roleMap.get(roomName).get(userName);
	}

	public void vote(String roomName, String usernameVoter, String playerVoted) throws IOException {
		if (playersAlive.get(roomName).contains(playerVoted)) {
			voteMap.get(roomName).put(usernameVoter, playerVoted);
		} else {
			sendPrivately(usernameVoter, "@Narrator;" + "Player already dead ! Choose another player.");
		}

	}

	public void initiateListPlayerAlive(String roomName) {
		playersAlive = new HashMap<>();

		for (String player : roleMap.keySet()) {
			playersAlive.put(roomName, new ArrayList<String>());
			playersAlive.get(roomName).add(player);
		}
	}

	public String eliminate(String roomName, Map<String, Integer> playersVotedTurn) {

		for (String name : voteMap.get(roomName).values()) {

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
		playersDead.get(roomName).add(eliminated);
		return eliminated;
	}

	public String sendDeadPlayerToWitch(String roomName) throws IOException, InterruptedException {
		String deadPlayer = "";
		for (String player : roleMap.keySet()) {
			if (roleMap.get(roomName).get(player).equals(Role.WITCH) && witchSavePower.get(roomName)) {
				sendPrivately(player, "@Narrator;" + "This player is dead. Do you want to save someone ?");
				sendPrivately(player, "@Narrator;" + playersDead.get(roomName).getLast());
				Thread.sleep(DUREE_TOUR - 15000);
				if (playerSaved.get(roomName)) {
					sendPrivately(player, "@Narrator;" + "Player saved");
				} else {
					sendPrivately(player, "@Narrator;" + "Player killed");
					deadPlayer = playersDead.get(roomName).getLast();
				}

			}

		}
		return deadPlayer;

	}

	public void resultWitchSave(String roomName, String vote) {
		playerSaved.put(roomName, false);
		if (vote.equals("yes")) {
			playersDead.remove(playersDead.get(roomName).getLast());
			playerSaved.put(roomName, true);
			witchSavePower.put(roomName, false);
		} else if (vote.equals("no")) {
			playersAlive.remove(playersDead.get(roomName).getLast());
			playerSaved.put(roomName, false) ;
		}
	}

	public void witchKillManagement(String roomName) throws IOException, InterruptedException {
		for (String player : roleMap.keySet()) {
			if (roleMap.get(roomName).get(player).equals(Role.WITCH) && witchKillPower.get(roomName)) {
				sendPrivately(player, "@Narrator;"
						+ "You still have your killing power. Do you kill to save someone ? (Write yes or no, if yes, write the player name after a space");
				Thread.sleep(DUREE_TOUR - 15000);
				if (!playerWitchToKill.isEmpty() && playersAlive.get(roomName).contains(playerWitchToKill.get(roomName))) {
					playersAlive.remove(playerWitchToKill.get(roomName));
					sendPrivately(player, "@Narrator;" + "Player killed");
				} else {
					sendPrivately(player, "@Narrator;" + "Player isn't alive");
				}

			}
		}
	}

	public void resultWitchKill(String roomName,String vote, String playername) {
		if (vote.equals("yes")) {
			playerWitchToKill.put(roomName, playername);
			witchKillPower.put(roomName, false);
		} else if (vote.equals("no")) {
			//nothing
		}
	}

	/**
	 * Join the room entered in parameter
	 * 
	 * @param roomName
	 * @throws IOException
	 */
	public void joinRoom(String roomName,String userName) throws IOException {
		rooms.get(roomName).addUsers(userName);
		roomSelection.remove(userName);
		sendToRoom(rooms.get(roomName), rooms.get(roomName).userKey());
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
