﻿package fr.dauphine.reseaux.werewolf.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread {

	private Server server;
	private Socket socket;
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private String username;
	Object message;

	private Room location;

	public ServerThread(Server server, Socket socket) throws IOException, ClassNotFoundException {
		// TODO Auto-generated constructor stub
		this.server = server;
		this.socket = socket;

		location = null;
		output = new ObjectOutputStream(this.socket.getOutputStream());
		output.flush();
		input = new ObjectInputStream(this.socket.getInputStream());

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
	@SuppressWarnings("deprecation")
	public void run() {

		try {
			// Thread will run until connections are present
			while (true) {
				try {
					message = input.readObject();
				} catch (Exception e) {
					break;
				}
				if (message.toString().contains("@EE@")) {
					String[] tabMsg = message.toString().split(";");

					String command = tabMsg[2];

					if (command.startsWith("/start")) {
						if (location == null || location.getHost() != this.username) {
							System.out.println("your not the host");
							// mettre ca sur le chat
						} else {
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

					} else if (command.startsWith("/createRoom")) {

						String roomName = command.split(" ")[1];
						String maxSize = command.split(" ")[2];

						server.createRoom(roomName, username, maxSize);
						location = server.getRooms().get(roomName);
						System.out.println("room created");

					} else if (command.startsWith("/join")) {
						String roomName = command.split(" ")[1];
						server.joinRoom(roomName, username);
						location = server.getRooms().get(roomName);
					} else if (command.startsWith("/vote")) {
						String vote = command.split(" ")[1];
						server.vote(location, username, vote);
					} else if (command.startsWith("/witch_save")) {

						if (command.split(" ") != null && command.split(" ").length > 1) {

							String vote = command.split(" ")[1];
							server.resultWitchSave(location, username, vote);

						}
					} else if (command.startsWith("/witch_kill")) {
						if (command.split(" ") != null && command.split(" ").length > 1) {
							String playername = command.split(" ")[1];
							server.resultWitchKill(location, username, playername);
						}

					} else {
						if (server.getRoomSelection().contains(username)) {
							server.sendToSelectionRoom(message);

						} else if (location.getUsers().contains(username)) {
							server.sendToRoom(location, message);
						}
					}

				} else {
					String formattedMsg = "@" + username + message.toString().substring(message.toString().indexOf(':'),
							message.toString().length());
					server.sendPrivately(message.toString().substring(1, message.toString().indexOf(':')),
							formattedMsg);
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
