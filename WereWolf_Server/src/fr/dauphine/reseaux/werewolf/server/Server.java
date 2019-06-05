package fr.dauphine.reseaux.werewolf.server;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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

	// Duree du tour des loup-garous (ms)
	private static final long DUREE_TOUR = 30000;
	private static final long DUREE_WAIT = 2000;

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
	 * Mapping between room name and Room (list of users in the room and all game
	 * objects)
	 */
	private Map<String, Room> rooms;

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
	 * send the data to all user in the Room room
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

	// Sending a message to all the available clients
	// public void sendToAll(String data) throws IOException {
	// String cryptedData =AES.encrypt(data);
	//
	// for (Enumeration<ObjectOutputStream> e = getOutputStreams();
	// e.hasMoreElements();) {
	// // since we don't want server to remove one client and at the same time
	// sending
	// // message to it
	// synchronized (outputStreams) {
	// ObjectOutputStream tempOutput = e.nextElement();
	// tempOutput.writeObject(cryptedData);
	// tempOutput.flush();
	// System.out.println("msg send" + cryptedData.toString());
	//
	// }
	// }
	// }

	// To get Output Stream of the available clients from the hash table
	private Enumeration<ObjectOutputStream> getOutputStreams() {
		// TODO Auto-generated method stub
		return outputStreams.elements();
	}

	// Sending private message
	public void sendPrivately(String username, String message) throws IOException {
		String cryptedMessage = AES.encrypt(message);
		// TODO Auto-generated method stub
		ObjectOutputStream tempOutput = clients.get(username);
		tempOutput.writeObject(cryptedMessage);
		tempOutput.flush();

	}

	// Wolves chat
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
	 * 
	 * 
	 * MAIN LOOP FOR THEGAME ENGINE !
	 * 
	 * 
	 */
	// GAME METHODS

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

					// if (location.getUsers().size() <= 3) {
					// if (!"".equals(eliminatedPlayerWolf)) {
					// location.getRoleMap().remove(eliminatedPlayerWolf);
					// }
					// }

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

							// Envoie un MP pour lui dire qui est mort;le joueur est rajoute e la liste
							// des
							// alives s'il est ressuscite
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

					/*
					 * if (location.getUsers().size() > 4) {
					 * 
					 * location.setRoleTurn(Role.SEER);
					 * 
					 * sendToRoom(location, "@Narrator;" + "La Voyante se reveille");
					 * sendToRoom(location, "@Timing;" + "Seer turn"); sendToRoom(location,
					 * "@Narrator;" +
					 * "Voyante choisissez le joueur dont vous voulez voir la carte");
					 * 
					 * // TODO VOYANTE e implementer
					 * 
					 * }
					 */

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
					 * Update user list display to the room with dead people
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

			if (winner(location).equals(Role.WOLF)) {
				sendToRoom(location, "@Narrator;Les loups-garous ont gagne !");

			} else {
				sendToRoom(location, "@Narrator;Les villageois ont gagne ! Vive le village de Thiercelieux");
			}

			if (winner(location) != null) {
				System.out.println("le jeu prend fin");

				sendToRoom(location, "@ROLETURN;");
				sendToRoom(location, "@END " + winner(location));
				for (String user : location.getUsers()) {
					this.roomSelection.add(user);

				}
				sendToSelectionRoom("ROOM" + rooms.keySet().toString());
//				this.rooms.remove(location.getName());
//
//				location.setHost(null);
//				location = null;

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRoomNameToUser(Room room, String username) throws IOException {
		sendPrivately(username, "@Game;" + "Vous etes dans la Room " + room.getName());
	}

	private boolean witch_Alive(Room room) {
		for (String player : room.getPlayersAlive()) {
			if (Role.WITCH.equals(room.getRoleMap().get(player))) {
				return true;
			}
		}
		return false;
	}

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

	public void menu() {
		// nothing
	}

	private boolean gameFinished(Room roomName) {
		Role winner = winner(roomName);
		if (winner == null) {
			return false;
		}
		return true;
	}

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

		if (nbPlayer >= 10) {

			int nbVillageois = (int) (nbPlayer * (0.75));
			int nbLoup = nbPlayer - nbVillageois - 1;

			for (int i = 0; i < nbVillageois; i++) {
				roles.add(Role.VILLAGER);

			}
			for (int i = 0; i < nbLoup; i++) {
				roles.add(Role.WOLF);
			}
			roles.add(Role.WITCH);

		}

		Collections.sort(roles);
		room.setRoleMap(new HashMap<String, Role>());

		for (String player : players) {
			room.getRoleMap().put(player, roles.removeFirst());
		}

		// send to all player their role
		for (String player : room.getUsers()) {
			sendPrivately(player, "@Role;" + room.getRoleMap().get(player).toString());
		}
	}

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
	 * this function is used for werewolf and villagers to choose an player and kill
	 * him
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

	public void killPlayer(Room room, String player) {
		room.getPlayersAlive().remove(player);
		room.getPlayersDead().addLast(player);

	}

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
