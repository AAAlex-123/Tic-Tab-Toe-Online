package ttt_online;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

/**
 * Abstract class to run a server, a thread that handles ad hoc connections and
 * reports its activity to the user. GameServer and ClientServer inherit from
 * it.
 */
abstract class Server implements Logging, Runnable {

	// server fields
	protected int playerCount;
	protected boolean printStackTrace;

	/**
	 * Counter to keep track of the number of players connected. Used to correctly
	 * display that number in the {@code {@link Screen#playerLabel}}
	 */
	protected static int gameConnected, chatConnected;

	protected final ObjectInputStream[] inputs;
	protected final ObjectOutputStream[] outputs;

	// screen is essentially a JTextArea for log messages
	protected Screen screen = new Screen();
	protected ServerSocket server;

	protected final char[] symbols;

	// array of chess piece characters used to replace duplicates
	protected final ArrayList<Character> chessPieces = new ArrayList<Character>(
			Arrays.asList('\u2654', '\u2655', '\u2656', '\u2657', '\u2658'));

	protected boolean argumentsPassed = false;

	/**
	 * Constructor of Server superclass. <br>
	 * Gets options from UI and initializes other fields
	 * 
	 * @see Server#getServerOptions() getServerOptions
	 * @see Server#setupScreen() setupScreen()
	 */
	Server() {
		getServerOptions();
		while (!argumentsPassed) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		gameConnected = 0;
		chatConnected = 0;

		inputs = new ObjectInputStream[playerCount];
		outputs = new ObjectOutputStream[playerCount];
		symbols = new char[playerCount];
	}

	/**
	 * Constructor of Server superclass. <br>
	 * Gets set options directly as arguments bypassing the UI.
	 *
	 * @param playerCount     int the number of connections
	 * @param printStackTrace boolean whether to show detailed crash reports
	 * 
	 * @see Server#getServerOptions() getServerOptions
	 * @see Server#setupScreen() setupScreen()
	 */
	Server(int playerCount, boolean printStackTrace) {
		gameConnected = 0;
		chatConnected = 0;

		this.printStackTrace = printStackTrace;
		inputs = new ObjectInputStream[playerCount];
		outputs = new ObjectOutputStream[playerCount];
		symbols = new char[playerCount];
	}

	/**
	 * Abstract method; runs the server.
	 * 
	 * @see GameServer#run()
	 * @see ChatServer#run()
	 */
	@Override
	public abstract void run();

	/**
	 * Abstract method; initializes the server.
	 * 
	 * @see GameServer#initialiseServer()
	 * @see ChatServer#initialiseServer()
	 */
	protected abstract void initializeServer();

	/**
	 * Abstract method; initializes connections to clients.
	 * 
	 * @see GameServer#getConnections()
	 * @see ChatServer#getConnections()
	 */
	protected abstract void getConnections();

	/**
	 * Gets server options from player using a UI and assigns values to the
	 * playerCount and printStackTrace variables.
	 */
	protected void getServerOptions() {

		JFrame optWind = new JFrame(String.format("Select %s Options", this.getClass().getSimpleName()));
		JPanel optPanel = new JPanel();
		optPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500, 300));
		optWind.setResizable(false);

		// ------ playerPanel = playerLabel + playerList
		JPanel playerPanel = new JPanel();
		playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));

		// ---------------- playerLabel
		JLabel playerLabel = new JLabel("Choose the number of players");
		playerPanel.add(playerLabel);

		// ---------------- playerNumbers
		JSlider playerNumbers = new JSlider(JSlider.HORIZONTAL, 2, 6, 2);

		playerNumbers.setFont(new Font("Serif", Font.BOLD, 25));
		playerNumbers.setMajorTickSpacing(2);
		playerNumbers.setMinorTickSpacing(1);
		playerNumbers.setPaintTicks(true);
		playerNumbers.setPaintLabels(true);
		playerPanel.add(playerNumbers);

		optPanel.add(Box.createRigidArea(new Dimension(20, 20)));
		optPanel.add(playerPanel);
		JCheckBox b1 = new JCheckBox("I would like to receive crash reports on my command line");
		optPanel.add(b1);

		// submit Button
		JButton submitButton = new JButton("Submit");
		submitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playerCount = playerNumbers.getValue();
				printStackTrace = b1.isSelected();
				optWind.dispose();
				argumentsPassed = true;
			}
		});
		optPanel.add(Box.createVerticalGlue());
		optPanel.add(submitButton);

		optWind.add(optPanel);
		optWind.revalidate();
	}

	/**
	 * Sets up the {@code screen} used for logging purposes
	 */
	protected void setupScreen() {
		screen.updateGameConnectionCounter(0);
		screen.updateChatConnectionCounter(0);
		screen.setVisible(true);
		screen.setSize(new Dimension(500, 300));
		screen.setResizable(true);
		screen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	/**
	 * Logs {@code text} on the {@code screen} because instead of the command line.
	 * Inserts the class name as a prefix.
	 */
	@Override
	public void log(String text) {
		screen.pushMessage(String.format("!%10s! %s", this.getClass().getSimpleName(), text));
	}

	/**
	 * Sends message {@code msg} to every client connected
	 * 
	 * @param msg  String, text to send
	 * @param args Object[], arguments
	 */
	protected void broadcast(String msg, Object... args) {
		for (int i = 0; i < playerCount; i++) {
			try {
				outputs[i].writeObject(String.format(msg, args));
			} catch (IOException e) {
				logerr("Error in broadcast()\n", e, printStackTrace);
			} catch (NullPointerException e) {
				;
			}
		}

		log(String.format(String.format("Broadcasted: %s", msg), args));
	}

	protected abstract int getGameCount();

	protected abstract int getChatCount();

	/**
	 * A simple UI to display log messages.
	 */
	@SuppressWarnings("serial")
	protected final class Screen extends JFrame {
		private final JTextArea logTextArea;
		private final JScrollPane scrollPane;
		private final JPanel windowPanel;
		/**
		 * Displays the number of connected players
		 */
		private final JLabel playerLabel;

		/**
		 * Initializes the {@code screen} used for logging purposes
		 */
		public Screen() {
			super("Server Log");
			windowPanel = new JPanel();
			windowPanel.setLayout(new BoxLayout(windowPanel, BoxLayout.PAGE_AXIS));

			playerLabel = new JLabel();
			windowPanel.add(playerLabel);

			logTextArea = new JTextArea();
			logTextArea.setEditable(false);
			logTextArea.setFont(new Font("Consolas", Font.PLAIN, 13));
			((DefaultCaret) logTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			scrollPane = new JScrollPane(logTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setPreferredSize(new Dimension(200, 200));

			windowPanel.add(scrollPane);
			add(windowPanel);
		}

		/**
		 * Appends {@code msg} to the screen
		 * 
		 * @param msg String, the message to append
		 */
		public final void pushMessage(String msg) {
			logTextArea.setText(logTextArea.getText() + "\n" + msg);
		}

		/**
		 * Updates the player count label according to the number of players connected
		 */
		private void updatePlayerLabel() {
			playerLabel.setText(String.format("Game connections: %d/%d\t\tChat connections: %d/%d", gameConnected,
					getGameCount(), chatConnected, getChatCount()));
		}

		/**
		 * Changes the {@code gameConnected} variable by {@code i}.<br>
		 * {@code i=0} resets the variable to 0.<br>
		 * 
		 * Afterwards updates the {@code playerLabel}
		 * 
		 * @param i int, the amount by which to change the gameConnected variable
		 * 
		 * @see Server.Screen#updatePlayerLabel() updatePlayerLabel()
		 */
		public final void updateGameConnectionCounter(int i) {
			gameConnected += i;
			if (i == 0)
				gameConnected = 0;
			updatePlayerLabel();
		}

		/**
		 * Changes the {@code chatConnected} variable by {@code i}.<br>
		 * {@code i=0} resets the variable to 0.<br>
		 * 
		 * Afterwards updates the {@code playerLabel}
		 * 
		 * @param i int, the amount by which to change the chatConnected variable
		 * 
		 * @see Server.Screen#updatePlayerLabel() updatePlayerLabel()
		 */
		public final void updateChatConnectionCounter(int i) {
			chatConnected += i;
			if (i == 0)
				chatConnected = 0;
			updatePlayerLabel();
		}
	}
}
