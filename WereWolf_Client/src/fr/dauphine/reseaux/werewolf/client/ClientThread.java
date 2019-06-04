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

	@Override
	public void run() {

		try {
			in = new ObjectInputStream(SOCK.getInputStream());
		} catch (IOException e) {

		}
		CheckStream();

	}

	public void CheckStream() {
		while (true) {
			RECEIVE();
		}
	}

	public void RECEIVE() {
		if (!in.equals(null)) {
			String encryptedMessage = "";
			try {

				encryptedMessage = (String) in.readObject();


			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {

			}

			String message = AES.decrypt(encryptedMessage);
			System.out.println(message + " decrypted");

			if (message.startsWith("ROOM")) {
				System.out.println(message);
				String temp1 = message.substring(4);
				temp1 = temp1.replace("[", "");
				temp1 = temp1.replace("]", "");

				currentUsers = temp1.split(", ");
				System.out.println(currentUsers[0]);
				// Arrays.sort(currentUsers);

				try {

					SwingUtilities.invokeLater(new Runnable() {
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

			if (message.startsWith("@END")) {
				/*
				 * DISPLAY POPUP
				 * 
				 * 
				 */
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

	public void SEND(final String str) throws IOException {
		String writeStr;
		if (str.startsWith("@")) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					Client.displayText.append("\n" + Client.userName + ": " + str);
				}

			});
			writeStr = str;
		} else
			writeStr = "@EE@;" + Client.userName + ";" + str;
		String encryptedMessage = AES.encrypt(writeStr);

		Client.output.writeObject(encryptedMessage);
		Client.output.flush();

	}

}
