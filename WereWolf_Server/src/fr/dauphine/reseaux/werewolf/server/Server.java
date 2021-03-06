package fr.dauphine.reseaux.werewolf.server;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import fr.dauphine.reseaux.werewolf.server.gameObjects.Role;

public class Server {

	private static final String DEBUT_DU_JEU = "Bienvenue sur le jeu du Loup-Garou! Le jeu peut commencer. \n Nous entrons dans une nuit noire  Thiercelieux, les villageois dorment profondemment ...\n\n"
			+ "-------------------------------------------------------\n";

	// TIMING

	/**
	 * Duree du tour des loup-garous (ms)
	 */
	private static final long DUREE_TOUR = 30000;

	/**
	 * Duree d'attente entre les evenements du jeu
	 */
	private static final long DUREE_WAIT = 2000;

	// NETWORK

	JFrame serverGui;
	JTextArea displayWindow;
	private ServerSocket serverSocket;
	private Socket socket;

	public Hashtable<Socket, ObjectOutputStream> outputStreams;

	/*
	 * Contain all users connected to the server
	 */
	public Hashtable<String, ObjectOutputStream> clients;

	// GAME

	/*
	 * Contain users in the roomSelection
	 */
	public Set<String> roomSelection;
	/**
	 * Mapping between room name and Room (list of users in the room and all game
	 * objects)
	 */
	private Map<String, Room> rooms;

	// Constructor

	public Server(int port) throws IOException {
		// Simple Gui for Server
		serverGui = new JFrame("Server");
		serverGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		serverGui.setSize(500, 500);
		displayWindow = new JTextArea();
		serverGui.add(new JScrollPane(displayWindow), BorderLayout.CENTER);
		serverGui.setVisible(true);

		// initiate network variables
		outputStreams = new Hashtable<>();
		clients = new Hashtable<>();
		serverSocket = new ServerSocket(port);

		// initiate game variables
		roomSelection = new HashSet<>();
		rooms = new HashMap<>();

		showMessage("Waiting for clients at " + serverSocket);
	}

	/*
	 * 
	 * PARTIE 1 | MOTEUR DE JEU *
	 * 
	 */

	public void startGame(Room location) throws InterruptedException {
		Set<String> displayUsers = new HashSet<>(location.getUsers());
		location.setSeerPower(true);
		location.setWitchPower(true);
		try {
			location.setPlayersDead(new LinkedList<String>());

			InitiateRole(location);
			initiateListPlayerAlive(location);

			sendToRoom(location, "@Narrator;" + DEBUT_DU_JEU);

			Thread.sleep(DUREE_WAIT);

			boolean first_turn = true;

			while (!gameFinished(location)) {
				try {
					/*
					 * 
					 * VILLAGER turn
					 * 
					 * 
					 */
					if (!first_turn) {
						sendToRoom(location, "@ROLETURN;VILLAGER");
						location.setRoleTurn(Role.VILLAGER);

						sendToRoom(location, "@Timing;" + "Villagers turn");

						String userKilledByVillage = villagersVote(location);

						if (!userKilledByVillage.equals("")) {
							displayUsers.remove(userKilledByVillage);
							displayUsers.add(userKilledByVillage + " <Dead>");
							sendToRoom(location, "!" + displayUsers);
						}
						if (gameFinished(location)) {
							break;
						}
					}

					first_turn = false;

					/*
					 * 
					 * 
					 * WEREWOLF TURN
					 * 
					 * 
					 */
					location.setRoleTurn(Role.WOLF);
					sendToRoom(location, "@ROLETURN;WOLF");

					sendToRoom(location, "@Narrator;"
							+ "Les loups-garous se reveillent et choisissent leur cible ('/vote PSEUDO' pour voter contre la cible)");
					sendToRoom(location, "@Timing;" + "Wolf turn");
					Thread.sleep(DUREE_TOUR);

					String eliminatedPlayerWolf = eliminate(location, false);

					sendToRoom(location, "@Narrator;" + "Les loups-garous se rendorment.");
					Thread.sleep(DUREE_WAIT);

					String eliminatedPlayerWitch = "";

					/*
					 * 
					 * WITCH turn
					 * 
					 * 
					 */

					if (location.getUsers().size() > 3) {
						location.setRoleTurn(Role.WITCH);
						if (witch_Alive(location)) {
							sendToRoom(location, "@Narrator;" + "La sorciere se reveille");

							/*
							 * 
							 * Send a private message to the witch to inform him which player has been
							 * killed by the werewolves The dead player is added again to the list of
							 * players alive if he is resurrected
							 */

							if (!"".equals(eliminatedPlayerWolf)) {
								sendToRoom(location, "@ROLETURN;WITCH_SAVE");
								eliminatedPlayerWolf = sendDeadPlayerToWitch(location);
							}
							if (!gameFinished(location)) {
								sendToRoom(location, "@ROLETURN;WITCH_KILL");
								eliminatedPlayerWitch = witchKillManagement(location);
							}

							Thread.sleep(DUREE_WAIT);

							sendToRoom(location, "@Narrator;" + "La sorciere retourne dormir");
						} else {
							sendToRoom(location,
									"@Narrator;" + "La sorciere n'est plus de ce monde, elle ne se reveille donc pas.");
						}
						Thread.sleep(DUREE_WAIT);
					}

					// SEND MESSAGE DEAD PERSON TO VILLAGE
					if (!"".equals(eliminatedPlayerWolf) && !"".equals(eliminatedPlayerWitch)) {
						sendToRoom(location,
								"@Narrator;"
										+ "Le jour se leve: les villageois se reveillent et decouvrent avec effroi que "
										+ eliminatedPlayerWolf + " [" + location.getRoleMap().get(eliminatedPlayerWolf)
										+ "] et " + eliminatedPlayerWitch + " ["
										+ location.getRoleMap().get(eliminatedPlayerWitch) + "]" + " sont morts ... !");

					} else if ("".equals(eliminatedPlayerWolf) && "".equals(eliminatedPlayerWitch)) {

						sendToRoom(location, "@Narrator;"
								+ "Le jour se leve: les villageois se reveillent et decouvrent avec bonheur que personne n'est mort !");
					} else if (!"".equals(eliminatedPlayerWolf)) {
						sendToRoom(location,
								"@Narrator;"
										+ "Le jour se leve: les villageois se reveillent et decouvrent avec effroi que "
										+ eliminatedPlayerWolf + " [" + location.getRoleMap().get(eliminatedPlayerWolf)
										+ "]" + " est mort... !");
					} else if (!"".equals(eliminatedPlayerWitch)) {
						sendToRoom(location,
								"@Narrator;"
										+ "Le jour se leve: les villageois se reveillent et decouvrent avec effroi que "
										+ eliminatedPlayerWitch + " ["
										+ location.getRoleMap().get(eliminatedPlayerWitch) + "]" + " est mort... !");
					}

					// If not saved, remove from RoleMap the killed user
					if (!"".equals(eliminatedPlayerWolf)) {
						location.getRoleMap().remove(eliminatedPlayerWolf);
					}
					if (!"".equals(eliminatedPlayerWitch)) {
						location.getRoleMap().remove(eliminatedPlayerWitch);
					}

					Thread.sleep(DUREE_WAIT);

					/*
					 * 
					 * Update user list of the room to display to the room with dead people
					 * 
					 */
					if (!eliminatedPlayerWolf.equals("")) {
						displayUsers.remove(eliminatedPlayerWolf);
						displayUsers.add(eliminatedPlayerWolf + " <Dead>");
						sendToRoom(location, "!" + displayUsers);
					}
					if (!eliminatedPlayerWitch.equals("")) {
						displayUsers.remove(eliminatedPlayerWitch);
						displayUsers.add(eliminatedPlayerWitch + " <Dead>");
						sendToRoom(location, "!" + displayUsers);
					}

					location.getVoteMap().clear();
				} catch (SocketException e) {
					break;
				}
			}

			Thread.sleep(DUREE_WAIT);
			/*
			 * Display the message of the winner
			 */
			if (winner(location).equals(Role.WOLF)) {
				sendToRoom(location, "@Narrator;Les loups-garous ont gagne !");

			} else {
				sendToRoom(location, "@Narrator;Les villageois ont gagne ! Vive le village de Thiercelieux");
			}

			if (winner(location) != null) {
				System.out.println("le jeu prend fin");

				sendToRoom(location, "@ROLETURN;SELECTION");
				sendToRoom(location, "@END " + winner(location));
				for (String user : location.getUsers()) {
					this.roomSelection.add(user);

				}
				sendToSelectionRoom("ROOM" + rooms.keySet().toString());

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * 
	 * PARTIE 2 | GAME METHODS
	 * 
	 */

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

	private boolean gameFinished(Room roomName) {
		Role winner = winner(roomName);
		if (winner == null) {
			return false;
		}
		return true;
	}

	/*
	 * Returns the winner of the room
	 */
	private Role winner(Room room) {
		boolean werewolfWins = true;
		boolean villajoisWins = true;

		for (Role role : room.getRoleMap().values()) {
			if (!role.equals(Role.WOLF)) {
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

	/**
	 * this function give a role to the players
	 * 
	 * @param room
	 * @throws IOException
	 */
	private void InitiateRole(Room room) throws IOException {
		int nbPlayer = room.getUsers().size();
		Set<String> players = room.getUsers();

		LinkedList<Role> roles = new LinkedList<>();

		if (nbPlayer == 1) {
			roles.add(Role.WOLF);
		}
		if (nbPlayer == 2) {

			roles.add(Role.WOLF);
			roles.add(Role.VILLAGER);
		}
		if (nbPlayer == 3) {

			roles.add(Role.WOLF);
			roles.add(Role.WOLF);
			roles.add(Role.VILLAGER);
			// roles.add(Role.VILLAGER);
		}

		if (nbPlayer == 4) {

			int nbVillageois = nbPlayer - 2;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			roles.add(Role.WOLF);
			roles.add(Role.WITCH);
		}

		if (nbPlayer == 5) {

			int nbVillageois = 3;
			int nbLoup = 1;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			for (int i = 0; i < nbLoup; i++) {
				roles.add(Role.WOLF);
			}
			roles.add(Role.WITCH);

		}
		if (nbPlayer == 6) {

			int nbVillageois = 3;
			int nbLoup = 2;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			for (int i = 0; i < nbLoup; i++) {
				roles.add(Role.WOLF);
			}
			roles.add(Role.WITCH);

		}
		if (nbPlayer == 7) {

			int nbVillageois = 5;
			int nbLoup = 1;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			for (int i = 0; i < nbLoup; i++) {
				roles.add(Role.WOLF);
			}
			roles.add(Role.WITCH);

		}
		if (nbPlayer == 8) {

			int nbVillageois = 5;
			int nbLoup = 2;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			for (int i = 0; i < nbLoup; i++) {
				roles.add(Role.WOLF);
			}
			roles.add(Role.WITCH);

		}
		if (nbPlayer > 8) {

			int nbLoup = (int) (nbPlayer * (0.25));
			int nbVillageois = nbPlayer - nbLoup - 1;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			for (int i = 0; i < nbLoup; i++) {
				roles.add(Role.WOLF);
			}
			roles.add(Role.WITCH);

		}
		Collections.shuffle(roles);
		room.setRoleMap(new HashMap<String, Role>());

		for (String player : players) {
			room.getRoleMap().put(player, roles.removeFirst());
		}

		// send to the players their role
		for (String player : room.getUsers()) {
			sendPrivately(player, "@Role;" + room.getRoleMap().get(player).toString());
		}
	}

	/*
	 * Vote management
	 */
	public void vote(Room room, String usernameVoter, String playerVoted) throws IOException {
		if (room.getPlayersAlive().contains(playerVoted)) {
			room.getVoteMap().put(usernameVoter, playerVoted);

			System.out.println(usernameVoter + " à vote pour " + room.getVoteMap().get(usernameVoter));
			System.out.println("size " + room.getVoteMap());
			sendPrivately(usernameVoter, "@Game;Vous avez vote " + playerVoted);

		} else {

			sendPrivately(usernameVoter, "@Game;" + "Le joueur est deja mort, choisissez un autre !");

		}

	}

	/**
	 * initlilize the list of player who are alive at the beginning
	 * 
	 * @param room
	 */
	public void initiateListPlayerAlive(Room room) {
		room.setPlayersAlive(new ArrayList<String>());

		for (String player : room.getRoleMap().keySet()) {
			room.getPlayersAlive().add(player);
		}
	}

	/**
	 * this function is used for werewolves and villagers to choose an player and
	 * kill him
	 * 
	 * @param room
	 * @param villageVote : true if it is the village vote (one killed mandatory,
	 *                    random if nobody votes)
	 * @return the name of killed player
	 */
	public String eliminate(Room room, boolean villageVote) {
		Map<String, Integer> playersVotedTurn = new HashMap<>();
		System.out.println("eliminate function");
		System.out.println(room.getVoteMap().size());
		for (String name : room.getVoteMap().values()) {

			if (playersVotedTurn.containsKey(name)) {
				playersVotedTurn.put(name, playersVotedTurn.get(name) + 1);
			} else {
				playersVotedTurn.put(name, 1);
			}
			System.out.println(playersVotedTurn.values() + "comptages des votes ");

		}

		Random rand = new Random();

		String eliminated = "";
		if (villageVote) {
			eliminated = room.getPlayersAlive().get(rand.nextInt(room.getPlayersAlive().size()));
		}
		int max = -1;

		for (String name : playersVotedTurn.keySet()) {
			if (playersVotedTurn.get(name) > max) {
				max = playersVotedTurn.get(name);
				eliminated = name;
			}
		}

		killPlayer(room, eliminated);

		return eliminated;
	}

	/**
	 * for villagers turn they vote clean the vote map at the beginning
	 * 
	 * @param location
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public String villagersVote(Room location) throws InterruptedException, IOException {
		sendToRoom(location, "@Narrator;"
				+ "De suite les villageois se concertent et decident de voter pour designer un coupable ('/vote PSEUDO' pour voter contre la cible)");
		location.getVoteMap().clear();
		Thread.sleep(DUREE_TOUR);
		String userKilledByVillage = eliminate(location, true);

		if (!"".equals(userKilledByVillage)) {

			sendToRoom(location, "@Narrator;" + userKilledByVillage + " a ete tue par le village et c'etait un(e) "

					+ location.getRoleMap().get(userKilledByVillage));
			location.getRoleMap().remove(userKilledByVillage);

		}

		Thread.sleep(DUREE_WAIT);

		location.getVoteMap().clear();

		return userKilledByVillage;
	}

	/*
	 * Update the lists of players alive and the dead players
	 */
	public void killPlayer(Room room, String player) {
		room.getPlayersAlive().remove(player);
		room.getPlayersDead().addLast(player);

	}

	/*
	 * Send to the witch which player was killed by the werewolves
	 */
	public String sendDeadPlayerToWitch(Room room) throws IOException, InterruptedException {
		String eliminated = room.getPlayersDead().getLast();
		for (String player : room.getRoleMap().keySet()) {
			if (room.getRoleMap().get(player).equals(Role.WITCH) && room.getWitchSavePower()) {
				sendPrivately(player, "@Narrator;" + room.getPlayersDead().getLast()
						+ " is dead. Do you want to save him ? (/witch_save ${yes or no}");
				sendToRoom(room, "@Timing;" + "Witch turn");
				Thread.sleep(DUREE_TOUR - 0);
				if (room.getPlayerSaved()) {
					sendPrivately(player, "@Game;" + "Player " + room.getPlayersDead().getLast() + " is saved.");
					room.setWitchSavePower(false);
					room.getPlayersAlive().add(room.getPlayersDead().getLast());
					room.getPlayersDead().removeLast();
					eliminated = "";
					room.setPlayerSaved(false);
				} else {
					sendPrivately(player, "@Game;" + "Player is dead.");
				}

			}

		}
		return eliminated;

	}

	/*
	 * Returns the result of whether the witch has decided to save or not the killed
	 * player
	 */
	public void resultWitchSave(Room room, String witchName, String vote) throws IOException {
		room.setPlayerSaved(false);
		if (vote.equals("yes")) {
			room.setPlayerSaved(true);
			sendPrivately(witchName, "@Game;Vous avez decide de sauver le joueur tue par les loups !");
		} else if (vote.equals("no")) {
			room.setPlayerSaved(false);
			sendPrivately(witchName, "@Game;Vous avez decide de ne pas sauver le joueur tue par les loups !");
		}
	}

	/*
	 * Witch killing potion management
	 */
	public String witchKillManagement(Room room) throws IOException, InterruptedException {
		for (String player : room.getRoleMap().keySet()) {
			if (room.getRoleMap().get(player).equals(Role.WITCH) && room.getWitchKillPower()) {
				sendPrivately(player, "@Narrator;"
						+ "You still have your killing power. Do you kill to save someone ? (To kill someone, write /witch_kill ${the player name you want to kill}. Else, just wait.  ");
				sendToRoom(room, "@Timing;" + "Witch turn");
				Thread.sleep(DUREE_TOUR - 0);

				String toKill = room.getPlayerWitchToKill();
				if (!toKill.isEmpty() && room.getPlayersAlive().contains(toKill)) {
					room.getPlayersAlive().remove(toKill);
					room.getPlayersDead().add(toKill);
					sendPrivately(player, "@Game;" + "Player " + toKill + " killed");

					room.setPlayerWitchToKill("");

					return toKill;
				}

				return "";

			}
		}
		return "";

	}

	/*
	 * Result of the witch killing potion
	 */
	public void resultWitchKill(Room room, String witchUserName, String playername) throws IOException {
		if (room.getPlayersAlive().contains(playername)) {
			room.setPlayerWitchToKill(playername);
			room.setWitchKillPower(false);
			sendPrivately(witchUserName, "@Game;" + "Vous decidez de tuer " + playername + ".");
		} else if (room.getPlayersDead().contains(playername)) {
			sendPrivately(witchUserName, "@Game;" + "Player " + playername + " isn't alive, you cannot kill him.");
		} else {
			sendPrivately(witchUserName, "@Game;" + "Player " + playername + " doesn't exist, you cannot kill him.");
		}

	}

	/*
	 * To know if the witch is alive or not
	 */
	private boolean witch_Alive(Room room) {
		for (String player : room.getPlayersAlive()) {
			if (Role.WITCH.equals(room.getRoleMap().get(player))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Join the room entered in parameter
	 * 
	 * @param roomName
	 * @throws IOException
	 */
	public void joinRoom(String roomName, String userName) throws IOException {
		rooms.get(roomName).addUsers(userName);
		roomSelection.remove(userName);
		sendToRoom(rooms.get(roomName), rooms.get(roomName).userKey());
	}

	/*
	 * 
	 * PARTIE 3 | COMMUNICATION CLIENT-SERVEUR *
	 * 
	 */

	// Waiting for clients to connect
	public void waitingForClients() throws IOException, ClassNotFoundException {

		while (true) {
			socket = serverSocket.accept();

			new ServerThread(this, socket);
		}
	}

	/**
	 * sending a message to all available clients in the selectionRoom
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void sendToSelectionRoom(String data) throws IOException {
		String cryptedData = AES.encrypt(data);
		for (String userName : roomSelection) {
			synchronized (roomSelection) {
				ObjectOutputStream tempOutput = clients.get(userName);
				tempOutput.writeObject(cryptedData);
				tempOutput.flush();

			}
		}
	}

	/**
	 * send the data to every user in the Room room
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void sendToRoom(Room room, String data) throws IOException {
		String cryptedData = AES.encrypt(data);

		for (String userName : room.getUsers()) {
			synchronized (clients) {
				ObjectOutputStream tempOutput = clients.get(userName);
				if (tempOutput != null) {
					tempOutput.writeObject(cryptedData);
					tempOutput.flush();
				}

			}
		}
	}

	/**
	 * send the data to all user in the Room room
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void sendToDeadRoom(Room room, String data) throws IOException {
		String cryptedData = AES.encrypt(data);

		for (String userName : room.getUsers()) {
			if (room.getPlayersDead().contains(userName)) {

				synchronized (clients) {
					ObjectOutputStream tempOutput = clients.get(userName);
					if (tempOutput != null) {
						tempOutput.writeObject(cryptedData);
						tempOutput.flush();
					}

				}
			}
		}
	}

	// Sending a private Message to the user with the Username
	public void sendPrivately(String username, String message) throws IOException {
		String cryptedMessage = AES.encrypt(message);
		ObjectOutputStream tempOutput = clients.get(username);
		tempOutput.writeObject(cryptedMessage);
		tempOutput.flush();

	}

	// Allows wolves to chat with other wolves and to not be seen by other roles
	public void sendToWolves(Room room, ArrayList<String> usersAlive, String message, String myUsername)
			throws IOException {
		for (String user : usersAlive) {
			if (room.getRoleMap().get(user).equals(Role.WOLF)) {
				sendPrivately(user, "@Wolf;" + myUsername + ";" + message);
			}
		}

	}

	// Removing the client from the client hash table
	public void removeClient(Room room, String username) throws IOException {

		synchronized (clients) {
			if (room != null) {

				room.getPlayersAlive().remove(username);
				room.getPlayersDead().remove(username);
				room.getRoleMap().remove(username);
				room.getUsers().remove(username);
			}
			clients.remove(username);
			if (room != null) {
				if (room.getUsers().isEmpty()) {
					rooms.remove(room.getName());
					sendToSelectionRoom("ROOM" + rooms.keySet().toString());
				} else {
					if (room.getHost().equals(username)) {
						String newHost = "";
						for (String user : room.getUsers()) {
							newHost = user;
							break;
						}

						room.setHost(newHost);
						sendPrivately(newHost, "@Game;Host have left the game. You are the new host !");
					}
					sendToRoom(room, "!" + room.getUsers());

				}
			}
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

	/*
	 * Display the name of the room to the user
	 */
	public void sendRoomNameToUser(Room room, String username) throws IOException {
		sendPrivately(username, "@Game;" + "Vous etes dans la Room " + room.getName());
	}

	/*
	 * 
	 * PARTIE 4 | GUI *
	 * 
	 */

	// displaying message on Server GUI
	public void showMessage(final String message) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				displayWindow.append(message);
			}

		});
	}

	/*
	 * 
	 * PARTIE 5 | GETTERS/SETTERS
	 * 
	 */
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
	 * @param roomSelection the roomSelection to set
	 */
	public void setRoomSelection(Set<String> roomSelection) {
		this.roomSelection = roomSelection;
	}

	/**
	 * @param rooms the rooms to set
	 */
	public void setRooms(Map<String, Room> rooms) {
		this.rooms = rooms;
	}

}
