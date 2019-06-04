package fr.dauphine.reseaux.werewolf.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class Client {

	// TESTS
	private final static boolean TEST = false;

	// Globals
	private static ClientThread clientThread;
	public static String userName = "Anonymous";
	public static Socket SOCK;
	public static ObjectOutputStream output;

	// GUI Globals - Main Window
	public static JFrame mainWindow = new JFrame();
	public static JPanel gui = new JPanel();

	public static JPanel topBar = new JPanel();
	public static JLabel top = new JLabel();
	public static JPanel PLAFContainer = new JPanel();
	public static String[] themeNames;
	public static JComboBox<?> themeChooser;

	public static JPanel userList = new JPanel();
	@SuppressWarnings("rawtypes")
	public static JList userOnlineList = new JList();
	public static JScrollPane listScroll = new JScrollPane();
	public static JButton submit = new JButton();

	public static JPanel textCenter = new JPanel();
	public static JTextArea displayText = new JTextArea();
	public static JPanel buttonLabelText = new JPanel();
	public static JPanel buttonText = new JPanel();
	public static JTextArea typeText = new JTextArea();
	public static JLabel message = new JLabel("Message:");

	// GUI Globals - Login Window
	public static JFrame logInWindow = new JFrame();
	public static JFrame popUpWindow = new JFrame();
	public static JPanel logInWindowGui = new JPanel();
	public static JPanel popUpWindowGui = new JPanel();
	public static JLabel logInEnterUsername = new JLabel("Enter Username: ");
	public static JTextField logInUsernameBox = new JTextField(20);
	public static JButton logInEnter = new JButton("Enter");
	public static JLabel popUpText = new JLabel("");
	public static JButton popUpEnter = new JButton("Back");

	// NETWORK
	public static final int port = 2620;

	public static final boolean localhost = true;

	public static final String ipServer = "25.31.163.176";

	private static List<String> serverUsersConnected = new ArrayList<String>();

	@SuppressWarnings("unchecked")
	public static void preConnect() {
		try {
			if (localhost) {

				SOCK = new Socket(InetAddress.getLocalHost(), port);
			} else {
				SOCK = new Socket(InetAddress.getByName(ipServer), port);

			}
			clientThread = new ClientThread(SOCK);

			// sending UserName
			output = new ObjectOutputStream(SOCK.getOutputStream());

			clientThread.in = new ObjectInputStream(SOCK.getInputStream());

			// waiting for list of users connected
			String tmp = (String) clientThread.in.readObject();
			// toString of a Set
			tmp = tmp.replaceAll("[\\[\\]]", "");

			for (String user : tmp.split(", ")) {
				serverUsersConnected.add(user);
			}

		} catch (Exception x) {
			System.out.println(x);
			JOptionPane.showMessageDialog(null, "Server Not Responding");
			System.exit(0);
		}
	}

	public static void Connect() {
		try {

			output.writeObject(userName);
			System.out.println(userName);
			output.flush();
		} catch (IOException ioException) {
			JOptionPane.showMessageDialog(null, "Error - UserName not Sent!");
		}

		top.setText("Online");

		Thread X = new Thread(clientThread);
		X.start();

	}

	public static void BuildMainWindow() {

		mainWindow.setTitle("Project ChatRoom - " + userName);

		ConfigureMainWindow();
		MainWindow_Action();
		mainWindow.setVisible(true);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void ConfigureMainWindow() {

		// setting MainWindow
		mainWindow.setContentPane(gui);
		mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		mainWindow.setMinimumSize(new Dimension(500, 300));
		mainWindow.pack();
		mainWindow.setLocationRelativeTo(null);
		try {
			// 1.6+
			mainWindow.setLocationByPlatform(true);
			mainWindow.setMinimumSize(mainWindow.getSize());
		} catch (Throwable ignoreAndContinue) {
		}

		// setting top bar
		top.setText("Offline");

		final UIManager.LookAndFeelInfo[] themes = UIManager.getInstalledLookAndFeels();
		themeNames = new String[themes.length];
		int ii;
		for (ii = 0; ii < themes.length; ii++) {
			themeNames[ii] = themes[ii].getName();
		}

		PLAFContainer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

		themeChooser = new JComboBox(themeNames);
		themeChooser.setSelectedIndex(ii - 1);

		PLAFContainer.add(themeChooser);

		topBar.setLayout(new BorderLayout(5, 5));
		topBar.setBorder(new TitledBorder(""));
		topBar.add(top, BorderLayout.WEST);
		topBar.add(PLAFContainer, BorderLayout.EAST);

		// setting the username list
		listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		listScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		listScroll.setViewportView(userOnlineList);
		listScroll.setPreferredSize(new Dimension(130, 200));
		listScroll.setMinimumSize(new Dimension(130, 200));

		// setting the submit Button
		submit.setText("SEND");
		submit.setPreferredSize(new Dimension(100, 100));
		submit.setMinimumSize(new Dimension(100, 30));

		// setting the east
		userList.setLayout(new BorderLayout(5, 5));
		userList.add(listScroll, BorderLayout.CENTER);
		userList.add(submit, BorderLayout.SOUTH);

		// setting the chat display area
		displayText.setText("");
		displayText.setBorder(new LineBorder(Color.GRAY));
		displayText.setEditable(false);

		// setting the textarea to type chat
		typeText.setPreferredSize(new Dimension(400, 60));
		typeText.setEditable(true);
		typeText.setBorder(new LineBorder(Color.GRAY));

		buttonText.setLayout(new BorderLayout(5, 5));
		buttonText.add(new JScrollPane(typeText), BorderLayout.CENTER);

		buttonLabelText.setLayout(new BorderLayout(5, 5));
		buttonLabelText.add(buttonText, BorderLayout.CENTER);
		buttonLabelText.add(message, BorderLayout.WEST);

		textCenter.setLayout(new BorderLayout(5, 5));
		textCenter.add(new JScrollPane(displayText), BorderLayout.CENTER);
		textCenter.add(buttonLabelText, BorderLayout.SOUTH);

		// setting everything in gui
		gui.setLayout(new BorderLayout(5, 5));
		gui.add(topBar, BorderLayout.NORTH);
		gui.add(userList, BorderLayout.EAST);
		gui.add(textCenter, BorderLayout.CENTER);

	}

	public static void MainWindow_Action() {

		mainWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				int result = JOptionPane.showConfirmDialog(null, "Are you sure", "Confirm", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);

				if (result == JOptionPane.YES_OPTION) {
					try {
						output.close();
						clientThread.in.close();
						SOCK.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					System.exit(0);
				} else {
					// Do nothing
				}
			}
		});

		submit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					SUBMIT_ACTION();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		});

		userOnlineList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				USERONLINELIST_ACTION(e);
			}
		});

		themeChooser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				int index = themeChooser.getSelectedIndex();
				final UIManager.LookAndFeelInfo[] themes = UIManager.getInstalledLookAndFeels();
				try {
					UIManager.setLookAndFeel(themes[index].getClassName());
					SwingUtilities.updateComponentTreeUI(mainWindow);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	public static void USERONLINELIST_ACTION(MouseEvent e) {
		if (e.getClickCount() == 2) {
			final String selectedUser = (String) userOnlineList.getSelectedValue();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					typeText.setText("@" + selectedUser + ": ");
					typeText.requestFocus();
				}
			});
		}
	}

	public static void SUBMIT_ACTION() throws IOException {
		if (!typeText.getText().equals("")) {
			String[] test = typeText.getText().split(" ");
			if (typeText.getText().contains("/createRoom") && typeText.getText().split(" ").length > 3) {
				BuildPopUpWindow("Room name cannot contain spaces !");
			} else {
				clientThread.SEND(typeText.getText());
				typeText.requestFocus();
				typeText.setText("");
			}
		}
	}

	public static void Initialize() {
		preConnect();
		submit.setEnabled(false);
		mainWindow.setEnabled(false);
	}

	public static void BuildLogInWindow() {

		logInWindow.setTitle("Log In");

		ConfigureLogInWindow();
		LogInWindow_Action();

		if (TEST) {
			Random rand = new Random();

			logInUsernameBox.setText("Client_" + rand.nextInt(100000));
			LOGIN_ACTION();
		} else {
			logInWindow.setVisible(true);
		}
	}

	public static void ConfigureLogInWindow() {
		logInWindow.setContentPane(logInWindowGui);
		logInWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		logInWindow.setMinimumSize(new Dimension(370, 90));
		logInWindow.pack();
		logInWindow.setLocationRelativeTo(null);
		try {
			// 1.6+
			logInWindow.setLocationByPlatform(true);
			logInWindow.setMinimumSize(logInWindow.getSize());
		} catch (Throwable ignoreAndContinue) {
		}

		logInWindowGui.setLayout(new FlowLayout());
		logInWindowGui.add(logInEnterUsername);
		logInWindowGui.add(logInUsernameBox);
		logInWindowGui.add(logInEnter);
	}

	public static void LogInWindow_Action() {
		logInEnter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!serverUsersConnected.contains(logInUsernameBox.getText().trim())) {
					LOGIN_ACTION();
				} else {
					BuildPopUpWindow("Username already taken");
				}
			}

		});

		logInUsernameBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LOGIN_ACTION();
			}
		});
	}

	public static void LOGIN_ACTION() {
		if (!logInUsernameBox.getText().equals("") && !logInUsernameBox.getText().contains(" ")) {
			userName = logInUsernameBox.getText().trim();
			mainWindow.setTitle("ChatRoom - " + userName);
			logInWindow.dispose();
			submit.setEnabled(true);
			mainWindow.setEnabled(true);
			typeText.requestFocus();
			Connect();
		} else {
			JOptionPane.showMessageDialog(null, "Please Enter a name (without spaces)!");
		}
	}

	public static void BuildPopUpWindow(String text) {

		popUpWindow.setTitle("Information");
		popUpText.setText(text);

		ConfigurePopUpWindow();
		PopUpWindow_Action();

		popUpWindow.setVisible(true);

	}

	public static void ConfigurePopUpWindow() {
		popUpWindow.setContentPane(popUpWindowGui);
		popUpWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		popUpWindow.setMinimumSize(new Dimension(370, 90));
		popUpWindow.pack();
		popUpWindow.setLocationRelativeTo(null);
		try {
			// 1.6+
			popUpWindow.setLocationByPlatform(true);
			popUpWindow.setMinimumSize(popUpWindow.getSize());
		} catch (Throwable ignoreAndContinue) {
		}

		popUpWindowGui.setLayout(new FlowLayout());
		popUpWindowGui.add(popUpText);
		popUpWindowGui.add(popUpEnter);
	}

	public static void PopUpWindow_Action() {
		popUpEnter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				popUpWindow.dispose();
			}

		});

	}

}