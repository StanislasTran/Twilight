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
		String cryptedData =AES.encrypt(data);
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
		String cryptedData =AES.encrypt(data);

		for (String userName : room.getUsers()) {
			synchronized (room) {
				ObjectOutputStream tempOutput = clients.get(userName);
				tempOutput.writeObject(cryptedData);
				tempOutput.flush();

			}
		}
	}

	// Sending a message to all the available clients
	public void sendToAll(String data) throws IOException {
		String cryptedData =AES.encrypt(data);

		for (Enumeration<ObjectOutputStream> e = getOutputStreams(); e.hasMoreElements();) {
			// since we don't want server to remove one client and at the same time sending
			// message to it
			synchronized (outputStreams) {
				ObjectOutputStream tempOutput = e.nextElement();
				tempOutput.writeObject(cryptedData);
				tempOutput.flush();
				System.out.println("msg send" + cryptedData.toString());

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
		String cryptedMessage=AES.encrypt(message);
		// TODO Auto-generated method stub
		ObjectOutputStream tempOutput = clients.get(username);
		tempOutput.writeObject(cryptedMessage);
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
	public void removeConnection(Socket socket, String username) {

		synchronized (outputStreams) {
			outputStreams.remove(socket);
		}

		// Printing out the client along with the IP offline in the format of
		// ReetAwwsum(123, 12, 21, 21) is offline
		showMessage("\n" + username + "(" + socket.getInetAddress().getHostAddress() + ") is offline");

	}

	/**
	 * @param location ******************
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

		location.setSeerPower(true);
		location.setWitchPower(true);
		try {
			location.setPlayersDead(new LinkedList<String>());

			InitiateRole(location);
			initiateListPlayerAlive(location);

			sendToRoom(location, "@Narrator;" + DEBUT_DU_JEU);

			Thread.sleep(2000);

			boolean first_turn = true;

			while (!gameFinished(location)) {
				if (!first_turn) {
					sendToRoom(location, "@Narrator;"
							+ "De suite les villageois se concertent et decident de voter pour désigner un coupable ('/vote PSEUDO' pour voter contre la cible)");
					// Thread.sleep(DUREE_TOUR);

					// TODO
				}
				first_turn = false;

				location.setRoleTurn(Role.WOLF);

				sendToRoom(location, "@Narrator;"
						+ "Les loups-garous se reveillent et choisissent leur cible ('/vote PSEUDO' pour voter contre la cible)");

				Thread.sleep(DUREE_TOUR);

				String eliminatedPlayerWolf = eliminate(location);
				String eliminatedPlayerWitch = "Nobody";

				if (location.getUsers().size() <= 3) {
					if (!"Nobody".equals(eliminatedPlayerWolf)) {
						location.getRoleMap().remove(eliminatedPlayerWolf);
					}
				}
				if (location.getUsers().size() > 3) {
					location.setRoleTurn(Role.WITCH);
					if (witch_Alive(location)) {
						sendToRoom(location, "@Narrator;" + "La sorciere se reveille");

						// Envoie un MP pour lui dire qui est mort;le joueur est rajout� � la liste des
						// alives s'il est ressuscit�
						if (!"Nobody".equals(eliminatedPlayerWolf)) {
							eliminatedPlayerWolf = sendDeadPlayerToWitch(location);
						}
						if (!gameFinished(location)) {
							eliminatedPlayerWitch = witchKillManagement(location);
						}

						sendToRoom(location, "@Narrator;" + "La sorciere retourne dormir");
					} else {
						sendToRoom(location,
								"@Narrator;" + "La sorciere n'est plus de ce monde, elle ne se reveille donc pas.");
						if (!"Nobody".equals(eliminatedPlayerWolf)) {
							location.getRoleMap().remove(eliminatedPlayerWolf);
						}
					}
				}

				if (location.getUsers().size() > 4) {

					location.setRoleTurn(Role.SEER);

					sendToRoom(location, "@Narrator;" + "La Voyante se reveille");
					sendToRoom(location, "@Narrator;" + "Voyante choisissez le joueur dont vous voulez voir la carte");

					// TODO�VOYANTE � implementer

				}

				Thread.sleep(1);

				// SEND MESSAGE DEAD PERSON TO VILLAGE
				if (!"Nobody".equals(eliminatedPlayerWolf) && !"Nobody".equals(eliminatedPlayerWitch)) {
					sendToRoom(location,
							"@Narrator;"
									+ "Le jour se leve: les villageois se reveillent et decouvrent avec effroi que "
									+ eliminatedPlayerWolf + " et " + eliminatedPlayerWitch + " sont morts ... !");
				} else if ("Nobody".equals(eliminatedPlayerWolf) && "Nobody".equals(eliminatedPlayerWitch)) {
					sendToRoom(location, "@Narrator;"
							+ "Le jour se leve: les villageois se reveillent et decouvrent avec bonheur que personne n'est mort !");
				} else if (!"Nobody".equals(eliminatedPlayerWolf)) {
					sendToRoom(location,
							"@Narrator;"
									+ "Le jour se leve: les villageois se reveillent et decouvrent avec effroi que "
									+ eliminatedPlayerWolf + " est mort... !");
				} else if ("Nobody".equals(eliminatedPlayerWitch)) {
					sendToRoom(location,
							"@Narrator;"
									+ "Le jour se leve: les villageois se reveillent et decouvrent avec effroi que "
									+ eliminatedPlayerWitch + " est mort... !");
				}

				System.out.println("Alive " + location.getPlayersAlive());
				System.out.println("Dead " + location.getPlayersDead());
				location.getVoteMap().clear();

			}

			if (winner(location).equals(Role.WOLF)) {
				sendToRoom(location, "@Narrator;Les loups-garous ont gagne !");
			} else {
				sendToRoom(location, "@Narrator;Les villageois ont gagne ! Vive le village de Thiercelieux");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean witch_Alive(Room room) {
		for (String player : room.getPlayersAlive()) {
			if (Role.WITCH.equals(room.getRoleMap().get(player))) {
				return true;
			}
		}
		return false;
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
			sendPrivately(usernameVoter, "@Narrator;Vous avez vot� " + playerVoted);
		} else {
			sendPrivately(usernameVoter, "@Narrator;" + "Le joueur est d�j� mort, choisissez un autre !");
		}

	}

	public void initiateListPlayerAlive(Room room) {
		room.setPlayersAlive(new ArrayList<String>());

		for (String player : room.getRoleMap().keySet()) {
			room.getPlayersAlive().add(player);
		}
	}

	public String eliminate(Room room) {
		Map<String, Integer> playersVotedTurn = new HashMap<>();

		for (String name : room.getVoteMap().values()) {

			if (playersVotedTurn.containsKey(name)) {
				playersVotedTurn.put(name, playersVotedTurn.get(name) + 1);
			} else {
				playersVotedTurn.put(name, 1);
			}

		}

		int max = 0;
		String eliminated = "Nobody";

		for (String name : playersVotedTurn.keySet()) {
			if (playersVotedTurn.get(name) > max) {
				max = playersVotedTurn.get(name);
				eliminated = name;
			}
		}
		if (!"Nobody".equals(eliminated)) {
			room.getPlayersAlive().remove(eliminated);
			room.getPlayersDead().addLast(eliminated);
		}

		return eliminated;
	}

	public String sendDeadPlayerToWitch(Room room) throws IOException, InterruptedException {
		String eliminated = room.getPlayersDead().getLast();
		for (String player : room.getRoleMap().keySet()) {
			if (room.getRoleMap().get(player).equals(Role.WITCH) && room.getWitchSavePower()) {
				sendPrivately(player,
						"@Narrator;" + "This player is dead. Do you want to save him ? (/witch_save ${yes or no}");
				sendPrivately(player, "@Narrator;" + room.getPlayersDead().getLast());
				Thread.sleep(DUREE_TOUR - 0);
				if (room.getPlayerSaved()) {
					sendPrivately(player, "@Narrator;" + "Player " + room.getPlayersDead().getLast() + " is saved");
					room.setWitchSavePower(false);
					room.getPlayersAlive().add(room.getPlayersDead().getLast());
					room.getPlayersDead().removeLast();
					eliminated = "Nobody";
					room.setPlayerSaved(false);
				} else {
					sendPrivately(player, "@Narrator;" + "Player is dead");
					room.getRoleMap().remove(room.getPlayersDead().getLast());
				}

			}

		}
		return eliminated;

	}

	public void resultWitchSave(Room room, String witchName, String vote) throws IOException {
		room.setPlayerSaved(false);
		if (vote.equals("yes")) {
			room.setPlayerSaved(true);
			sendPrivately(witchName, "@Narrator;Vous avez decide de sauver le joueur tue par les loups !");
		} else if (vote.equals("no")) {
			room.setPlayerSaved(false);
			sendPrivately(witchName, "@Narrator;Vous avez decide de ne pas sauver le joueur tue par les loups !");
		}
	}

	public String witchKillManagement(Room room) throws IOException, InterruptedException {
		for (String player : room.getRoleMap().keySet()) {
			if (room.getRoleMap().get(player).equals(Role.WITCH) && room.getWitchKillPower()) {
				sendPrivately(player, "@Narrator;"
						+ "You still have your killing power. Do you kill to save someone ? (To kill someone, write /witch_kill ${the player name you want to kill}. Else, just wait.  ");

				Thread.sleep(DUREE_TOUR - 0);

				String toKill = room.getPlayerWitchToKill();
				if (!toKill.isEmpty() && room.getPlayersAlive().contains(toKill)) {
					room.getPlayersAlive().remove(toKill);
					room.getPlayersDead().add(toKill);
					room.getRoleMap().remove(toKill);
					sendPrivately(player, "@Narrator;" + "Player " + toKill + " killed");

					room.setPlayerWitchToKill("");

					return toKill;
				}

				return "Nobody";

			}
		}
		return "Nobody";

	}

	public void resultWitchKill(Room room, String witchUserName, String playername) throws IOException {
		if (room.getPlayersAlive().contains(playername)) {
			room.setPlayerWitchToKill(playername);
			room.setWitchKillPower(false);
			sendPrivately(witchUserName, "@Narrator;" + "Vous decidez de tuer " + playername + ".");
		} else if (room.getPlayersDead().contains(playername)) {
			sendPrivately(witchUserName, "@Narrator;" + "Player " + playername + " isn't alive, you cannot kill him.");
		} else {
			sendPrivately(witchUserName,
					"@Narrator;" + "Player " + playername + " doesn't exist, you cannot kill him.");
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
