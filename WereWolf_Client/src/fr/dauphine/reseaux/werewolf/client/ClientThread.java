package fr.dauphine.reseaux.werewolf.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
			String message = "";
			try {
				message = (String) in.readObject();
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (message.startsWith("!")) {
				String temp1 = message.substring(1);
				temp1 = temp1.replace("[", "");
				temp1 = temp1.replace("]", "");

				currentUsers = temp1.split(", ");
				Arrays.sort(currentUsers);

				try {

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							Client.userOnlineList.setListData(currentUsers);
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
//			else if(message.startsWith("@")){
//				final String temp3 = message.substring(1);
//				
//				SwingUtilities.invokeLater(
//					new Runnable(){
//						public void run() {
//							Client.displayText.append("\n"+temp3);					
//						}
//					}
//				);
//			}

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

		Client.output.writeObject(writeStr);
		Client.output.flush();

	}

}
