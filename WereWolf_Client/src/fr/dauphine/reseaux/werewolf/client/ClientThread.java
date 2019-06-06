package fr.dauphine.reseaux.werewolf.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ClientThread implements Runnable {

	// Globals
	Socket SOCK;
	public ObjectInputStream in;
	String[] currentUsers;

	// Constructor getting the socket
	public ClientThread(Socket X) {
		this.SOCK = X;
	}

	/*
	 * 
	 * PARTIE 1 | LANCEMENT DU THREAD D'ECOUTE EN CONTINUE DU CANAL *
	 * 
	 */

	@Override
	public void run() {

		CheckStream();

	}

	public void CheckStream() {
		while (true) {
			RECEIVE();
		}
	}

	/*
	 * 
	 * PARTIE 2 | SEND / RECEIVE via deux threads *
	 * 
	 */

	/**
	 * Envoie le message avec pour en-tête '@EE@'
	 */
	public void SEND(final String str) throws IOException {
		String writeStr;

		writeStr = "@EE@;" + Client.userName + ";" + str;

		String encryptedMessage = AES.encrypt(writeStr);

		Client.output.writeObject(encryptedMessage);
		Client.output.flush();

	}

	/**
	 * 
	 */
	public void RECEIVE() {
		if (!in.equals(null)) {
			String encryptedMessage = "";
			try {

				encryptedMessage = (String) in.readObject();

			} catch (ClassNotFoundException e1) {
			} catch (IOException e1) {
			}

			String message = AES.decrypt(encryptedMessage);
			System.out.println(message + " decrypted");

			if (message.startsWith("ROOM")) {
				String temp1 = message.substring(4);
				temp1 = temp1.replace("[", "");
				temp1 = temp1.replace("]", "");

				currentUsers = temp1.split(", "); // Arrays.sort(currentUsers);

				try {

					SwingUtilities.invokeLater(new Runnable() {
						@SuppressWarnings("unchecked")
						@Override
						public void run() {
							Client.userOnlineList.setListData(currentUsers);

						}
					});
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Unable to set Online list data");
				}
			}

			if (message.startsWith("@ROLETURN")) {
				String temp = message.split(";")[1];

				Client.roleTurn = temp;
			} else if (message.startsWith("@END")) {

				Client.displayText.setText("");
				Client.top.setText("");
				Client.displayText.append(message.substring(5) + " won");
				Client.displayText.append("\n welcome back in selection room");
				Client.displayText.append("\n use /createRoom (roomName) (size) to create a room");
				Client.displayText.append("\n use /join (roomName) to join a room");

			}

			if (message.startsWith("SYSTEM")) {

				Client.displayText.append("Système: " + message.substring(7));
			}

			if (message.startsWith("!")) {
				System.out.println("List joueurs: " + message);
				String temp1 = message.substring(1);
				temp1 = temp1.replace("[", "");
				temp1 = temp1.replace("]", "");

				currentUsers = temp1.split(", ");
				System.out.println(currentUsers[0]);
				// Arrays.sort(currentUsers);

				try {

					SwingUtilities.invokeLater(new Runnable() {
						@SuppressWarnings("unchecked")
						@Override
						public void run() {
							Client.userOnlineList.setListData(currentUsers);
							System.out.println(currentUsers[0] + "3");

						}
					});
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Unable to set Online list data");
				}
			}

			else if (message.startsWith("@EE@;")) {
				final String[] temp2 = message.split(";");

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Client.displayText.append("\n" + temp2[1] + ": " + temp2[2]);
					}
				});
			}

			else if (message.startsWith("@Wolf;")) {
				final String[] temp2 = message.split(";");
				// final String msg = message.split(";")[2];

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Client.displayText.append("\n" + "<Wolf " + temp2[1] + "> " + temp2[2]);
					}
				});
			}

			else if (message.startsWith("@Narrator")) {
				final String temp2 = message.split(";")[1];

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Client.displayText.append("\n");
						// Client.displayText.append("\n \n \n \n\n\n\n\n\n");
						// DefaultCaret caret = (DefaultCaret) Client.displayText.getCaret();
						// caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
						Client.displayText.append("\n" + "NARRATOR >  " + temp2);

					}
				});

			} else if (message.startsWith("@Game")) {
				final String temp2 = message.split(";")[1];

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Client.displayText.append("\n");
						// Client.displayText.append("\n \n \n \n\n\n\n\n\n");
						// DefaultCaret caret = (DefaultCaret) Client.displayText.getCaret();
						// caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
						Client.displayText.append("\n" + "Info >  " + temp2);

					}
				});

			} else if (message.startsWith("@Timing")) {
				final String temp2 = message.split(";")[1];

				SwingUtilities.invokeLater(new Runnable() {
					Timer timer;
					long startTime = -1;
					final long duration = 30000;

					@Override
					public void run() {

						timer = new Timer(10, new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								if (startTime < 0) {
									startTime = System.currentTimeMillis();
								}
								long now = System.currentTimeMillis();
								long clockTime = now - startTime;
								if (clockTime >= duration) {
									clockTime = duration;
									timer.stop();
								}
								SimpleDateFormat df = new SimpleDateFormat("mm:ss:SSS");

								Client.top.setText("Online --- " + temp2 + " " + df.format(duration - clockTime));
							}
						});
						timer.setInitialDelay(0);
						timer.start();

					}
				});

			} else if (message.startsWith("@Role")) {
				final String temp2 = message.split(";")[1];

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						Client.displayText.append("\n");
						// Client.displayText.append("\n \n \n \n\n\n\n\n\n");
						// DefaultCaret caret = (DefaultCaret) Client.displayText.getCaret();
						// caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
						Client.displayText.append("\n" + "YOUR ROLE IS " + temp2);
					}
				});
			}
			// else if(message.startsWith("@")){
			// final String temp3 = message.substring(1);
			//
			// SwingUtilities.invokeLater(
			// new Runnable(){
			// public void run() {
			// Client.displayText.append("\n"+temp3);
			// }
			// }
			// );
			// }

		}
	}

}
