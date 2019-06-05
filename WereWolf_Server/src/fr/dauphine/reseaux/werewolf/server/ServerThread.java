package fr.dauphine.reseaux.werewolf.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import fr.dauphine.reseaux.werewolf.server.gameObjects.Role;
import fr.dauphine.reseaux.werewolf.server.gameObjects.Status;

public class ServerThread extends Thread {

	private Server server;
	private Socket socket;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private String username;
	private String message;
	private Room location;

	public ServerThread(Server server, Socket socket) throws IOException, ClassNotFoundException {
		this.server = server;
		this.socket = socket;

		location = null;
		output = new ObjectOutputStream(this.socket.getOutputStream());
		output.flush();
		input = new ObjectInputStream(this.socket.getInputStream());

		output.writeObject(server.clients.keySet().toString());

		username = (String) input.readObject();

		server.clients.put(username, output);
		server.outputStreams.put(socket, output);

		// add the player in selectionRoom
		server.roomSelection.add(username);

		server.sendToSelectionRoom("!" + server.getRooms().keySet());

		server.showMessage("\n" + username + "(" + socket.getInetAddress().getHostAddress() + ") is online");
		// starting the thread
		start();
	}

	@Override
	public void run() {

		try {
			// Thread will run until connections are present
			while (true) {
				try {
					String encryptedMessage = (String) input.readObject();
					message = AES.decrypt(encryptedMessage);
					System.out.println("decrypted " + message);
				} catch (Exception e) {
					break;
				}
				if (message.toString().contains("@EE@")) {
					String[] tabMsg = message.toString().split(";");

					String command = tabMsg[2];

					if (location != null && location.getPlayersDead().contains(username)
							&& !server.roomSelection.contains(username)) {
						if (command.startsWith("/")) {
							server.sendPrivately(username,
									"@Game;Vous etes morts. Vous ne pouvez plus effecter d'actions.");
						} else {
							String newmsg = "";
							newmsg += tabMsg[0] + ";";
							newmsg += "<Dead> " + tabMsg[1] + ";";
							newmsg += tabMsg[2];

							server.sendToDeadRoom(location, newmsg);
						}

					} else if (command.startsWith("/start")) {

						if (server.roomSelection.contains(username)) {
							server.sendPrivately(username, "@Game;Veuillez creer une room pour lancer la partie.");
						}
						if (location == null || location.getHost() != this.username) {

							server.sendPrivately(username,
									"@Game;Vous n'etes pas l'hote, vous ne pouvez pas lancer la partie.");
						} else if (location.getStatus().equals(Status.STARTED)) {
							server.sendPrivately(username,
									"@Game;Vous ne pouvez pas utiliser cette commande, le jeu a deja debute.");
						} else {
							location.setStatus(Status.STARTED);
							server.sendToRoom(location, "@ROLETURN;ROOM");
							server.getRooms().remove(location.getName());

							server.sendToSelectionRoom("ROOM " + server.getRooms().keySet());
							new Thread(new Runnable() {

								@Override
								public void run() {
									try {
										server.startGame(location);
									} catch (InterruptedException e) {

									}
								}
							}).start();
						}
					} else if (command.startsWith("/back")) {
						if (server.getRoomSelection().contains(username)) {
							server.sendPrivately(username, "SYSTEM Vous êtes déjà dans la selectionRoom");
						} else if (this.location.getStatus().equals(Status.WAITING)) {

							if (location.getHost().equals(username)) {

								for (String user : location.getUsers()) {
									server.roomSelection.add(user);
								}
								server.getRooms().remove(location.getName());
								location.setHost(null);
								location.setUsers(null);
								location = null;
							} else {
								server.roomSelection.add(username);
								location.getUsers().remove(username);
								server.sendToRoom(location, location.userKey());

							}

							server.sendToSelectionRoom("ROOM " + server.getRooms().keySet());
						} else {
							if (server.getRoomSelection().contains(username)) {
								server.sendPrivately(username, "SYSTEM impossible de sortir d'une aprtie en cours");
							}
						}

					} else if (command.startsWith("/createRoom")) {
						if (!server.roomSelection.contains(username)) {
							server.sendPrivately(username,
									"@Game;Vous ne pouvez pas utiliser cette commande, vous etes deja dans une room.");
						} else {
							try {
								String roomName = command.split(" ")[1];
								if (!server.getRooms().containsKey(roomName)) {
									String maxSize = command.split(" ")[2];

									server.createRoom(roomName, username, maxSize);
									location = server.getRooms().get(roomName);
									server.sendRoomNameToUser(location, username);
									System.out.println("room created");
								} else {
									server.sendPrivately(username, "SYSTEM \n Ce nom de Room est déjà utilisé");

								}
							} catch (Exception e) {
								server.sendPrivately(username, "SYSTEM \n La commande est incorrecte");
							}
						}
					} else if (command.startsWith("/join")) {
						if (!server.roomSelection.contains(username)) {
							server.sendPrivately(username,
									"@Game;Vous ne pouvez pas utiliser cette commande, vous etes deja dans une room.");
						} else {

							String roomName = command.split(" ")[1];
							if (server.getRooms().get(roomName).getStatus().equals(Status.WAITING)) {
								server.joinRoom(roomName, username);
								location = server.getRooms().get(roomName);
								server.sendRoomNameToUser(location, username);
							} else {
								server.sendPrivately(username,
										"SYSTEM la room que vous souhaitez rejoindre n''existe pas ou n'est plus disponible");
							}
						}
					} else if (command.startsWith("/vote")) {
						if (server.roomSelection.contains(username)) {
							server.sendPrivately(username,
									"@Game;Vous ne pouvez pas utiliser cette commande, vous n'etes pas dans une room.");
						} else {

							String vote = command.split(" ")[1];

							if (location.getRoleTurn().equals(Role.WOLF)) {
								if (!location.getRoleMap().get(username).equals(Role.WOLF)) {
									server.sendPrivately(username,
											"@Game;Vous n'etes pas loups-garou, vous ne pouvez pas voter.");
								} else {
									server.vote(location, username, vote);
								}
							} else if (location.getRoleTurn().equals(Role.VILLAGER)) {
								server.vote(location, username, vote);
							} else {
								server.sendPrivately(username, "@Game;Ce n'est pas votre tour !");
							}
						}
					} else if (command.startsWith("/witch_save")) {

						if (server.roomSelection.contains(username)) {
							server.sendPrivately(username,
									"@Game;Vous ne pouvez pas utiliser cette commande, vous n'etes pas dans une room.");
						} else {

							if (location.getRoleTurn().equals(Role.WITCH)) {
								if (!location.getRoleMap().get(username).equals(Role.WITCH)) {
									server.sendPrivately(username,
											"@Game;Vous n'etes pas la sorciere, vous ne pouvez pas voter.");
								} else {
									if (command.split(" ") != null && command.split(" ").length > 1) {
										String vote = command.split(" ")[1];
										server.resultWitchSave(location, username, vote);
									}
								}
							} else {
								server.sendPrivately(username, "@Game;Ce n'est pas votre tour !");
							}
						}

					} else if (command.startsWith("/witch_kill")) {
						if (server.roomSelection.contains(username)) {
							server.sendPrivately(username,
									"@Game;Vous ne pouvez pas utiliser cette commande, vous n'etes pas dans une room.");
						} else {

							if (location.getRoleTurn().equals(Role.WITCH)) {
								if (!location.getRoleMap().get(username).equals(Role.WITCH)) {
									server.sendPrivately(username,
											"@Game;Vous n'etes pas sorciere, vous ne pouvez pas voter.");
								} else {
									if (command.split(" ") != null && command.split(" ").length > 1) {
										String playername = command.split(" ")[1];
										server.resultWitchKill(location, username, playername);
									}
								}
							} else {
								server.sendPrivately(username, "@Game;Ce n'est pas votre tour !");
							}
						}

					} else if (command.startsWith("/wolf")) {
						if (location.getRoleTurn().equals(Role.WOLF)) {
							if (!location.getRoleMap().get(username).equals(Role.WOLF)) {
								server.sendPrivately(username,
										"@Game;Vous n'etes pas loup-garou, vous ne pouvez pas voter.");
							} else {
								if (command.split(" ") != null && command.split(" ").length > 1) {
									String wolfMessage = command.split(" ")[1];
									// String wolfMessage = command;
									System.out.println(wolfMessage);
									System.out.println(location.getPlayersAlive());
									server.sendToWolves(location, location.getPlayersAlive(), wolfMessage, username);
								}
							}
						} else {
							server.sendPrivately(username, "@Game;Ce n'est pas votre tour !");
						}
						// case où on veut juste envoyer un message

					} else {
						if (server.getRoomSelection().contains(username)) {
							server.sendToSelectionRoom(message);

						} else if (location.getUsers().contains(username)) {
							server.sendToRoom(location, message);
						}
					}

				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
		} finally {
			try {
				server.removeClient(location, username);
				server.removeConnection(socket, username);
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
	}

	/**
	 * @return the location
	 */
	public Room getLocation() {
		return location;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(Room location) {
		this.location = location;
	}

}
