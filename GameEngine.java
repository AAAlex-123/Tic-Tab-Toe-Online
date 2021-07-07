package ttt_online;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 * Client-side application to handle communications with the server. Uses a
 * GameUI instance as input/output to the user.
 */
final class GameEngine implements Logging {

	// Constants
	// default ports of the Game and Chat Servers
	private static final int GAME_PORT = 10001;
	private static final int CHAT_PORT = 10002;

	final static char DASH = '-';

	private static final int INFORMATION = JOptionPane.INFORMATION_MESSAGE;
	private static final int WARNING = JOptionPane.WARNING_MESSAGE;
	private static final int ERROR = JOptionPane.ERROR_MESSAGE;

	private static final int CHAT = 1;
	private static final int GAME = 2;
	private static final int CHAT_GAME = 3;

	// variables Initialized from UI
	private String address;
	private boolean printStackTrace;
	// used to determine UI and graphics size
	private static final int HEIGHT_MULTIPLIER = Toolkit.getDefaultToolkit().getScreenSize().height < 750 ? 1 : 2;

	private int serverCode;
	private Color color = Color.BLACK;
	private char character;

	private boolean argumentsPassed = false;

	private final GameUI ui;

	private Socket serverSocket;
	private ObjectOutputStream serverOutput;
	private ObjectInputStream serverInput;

	private GameBoard localGameBoard;

	private final ChatClient chatClient;

	/**
	 * Constructs the GameEngine and sets up the UI.
	 * 
	 * @see GameEngine#getClientOptions() getClientOptions
	 */
	GameEngine() {
		chatClient = new ChatClientImpl();
		getClientOptions();
		while (!argumentsPassed) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log(String.format("Started client for %s",
				serverCode == 0 ? "chat" : serverCode == 1 ? "game" : "game and chat"));

		ui = new GameUI(color, character, GameEngine.HEIGHT_MULTIPLIER);
		ui.add(chatClient.render());
		setupUI();
	}

	/**
	 * Main method. Run to create and run a client.
	 */
	public static void main(String[] args) {
		GameEngine gameEngine = new GameEngine();
		gameEngine.run();
	}

	/**
	 * Runs the GameEngine initializing connections to Game and Chat servers
	 * according to the {@code serverCode} field
	 */
	private void run() {
		// use return codes of each method to determine success or failure
		// and continue execution accordingly
		if (serverCode == CHAT_GAME || serverCode == CHAT) {
			if (chatClient.getChatConnection(address, CHAT_PORT) == 0) {
				chatClient.initChat();
				chatClient.setEnableChat(true);
			}
		}
		if (serverCode == CHAT_GAME || serverCode == GAME) {
			if (getServerConnection() == 1)
				return;

			// this loops exits when:
			// - any method does not execute normally (return code 1)
			// - game ends normally inside setup() (return code 2)
			while (true) {
				if (setup(true) != 0)
					break;
				if (play() != 0)
					break;
				if (setup(false) != 0)
					break;
			}
		}
		ui.setEnableTurn(false);
	}

	/**
	 * Sets up the UI.
	 */
	private void setupUI() {
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(500, 300 * HEIGHT_MULTIPLIER + 300);
		ui.setVisible(true);
		ui.setResizable(true);
		ui.setEnableTurn(false);
		chatClient.setEnableChat(false);
		ui.pushMessage("Please wait to connect to servers");
	}

	/**
	 * Initializes a connection to the Game Server.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If two or more players have the same symbol, the server allocates them
	 * special unique chess pieces and informs the client here.<br>
	 * <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return int, 0 or 1, indicating success or fail
	 */
	private int getServerConnection() {
		try {
			// get connection
			serverSocket = new Socket(InetAddress.getByName(address), GAME_PORT);
			serverOutput = new ObjectOutputStream(serverSocket.getOutputStream());
			serverInput = new ObjectInputStream(serverSocket.getInputStream());

			// exchange messages and get the index of the connection to server (look below)
			serverOutput.writeObject(ui.getSymbol());
			serverOutput.writeObject(ui.getColor());

			String serverMsg = (String) serverInput.readObject();
			ui.pushMessage("\nGame Server said: %s", serverMsg);

			Matcher m = Pattern.compile(".*#(\\d).*").matcher(serverMsg);
			m.find();
			int connectionToServerIndex = Integer.parseInt(m.group(1));

			log(String.format(
					"Connected to Game Server successfully as player '%c' with number #%d with color (r, g, b): (%d, %d %d)",
					ui.getSymbol(), connectionToServerIndex, ui.getColor().getRed(), ui.getColor().getGreen(),
					ui.getColor().getBlue()));

			// wait for ready message and for updated symbols/colors
			ui.pushMessage("\nGame Server said: %s", (String) serverInput.readObject());

			char[] symbols = ((char[]) serverInput.readObject());
			Color[] colors = ((Color[]) serverInput.readObject());

			// using index of the connection to server, figure out if symbol has changed
			// if it has, show popup and update the label at the top
			if (ui.getSymbol() != symbols[connectionToServerIndex]) {
				JOptionPane.showMessageDialog(this.ui,
						"Looks like you selected the same symbol as another player connected to the Game Server.\nWorry not, because we provided you with an exclusive chess piece as your symbol!",
						"Message", INFORMATION);
				ui.setSymbol(symbols[connectionToServerIndex]);
			}
			ui.setCustomOptions(symbols, colors);

		} catch (IOException e) {
			exit("Couldn't connect to Game Server; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in getServerConnection()\n", WARNING, e, serverCode == GAME,
					"Connection Error");
			return 1;
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers",
					"!game! ClassNotFoundException in getServerConnection()\n", ERROR, e, true, "Very Serious Error");
			return 1;
		}
		return 0;
	}

	/**
	 * Sets up the UI and exchanges messages before and after the player makes their
	 * move.<br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @param starting boolean, whether or not it is the start or the end of the
	 *                 player's turn
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return int, 0, 1 or 2 indicating success, fail or normal game ending
	 *         respectively
	 */
	private int setup(boolean starting) {
		try {
			if (starting) {
				ui.pushMessage("");
				ui.focusMove();
				log("\nStarting turn");
			} else {
				// disable buttons/text
				if (serverCode == CHAT_GAME) {
					chatClient.focusChat();
				}
				ui.setEnableTurn(false);
			}

			// update boards till your turn
			Object serverMessage = serverInput.readObject();
			if (starting) {
				do {
					try {
						updateBoard((char[][]) serverMessage);
					} catch (ClassCastException e) {
						break;
					}
					serverMessage = serverInput.readObject();
				} while (serverMessage instanceof char[][]);
			}
			log("done reading boards");

			// get ready message
			String response = (String) serverMessage;

			// if message is "won" or "resigned" or "tie"
			// display some messages and stop game thread (return code 2)
			if (response.matches("Player.*resigned") || response.matches("Player.*won!")) {
				if (response.charAt(8) == ui.getSymbol())
					ui.pushMessage("%c", '\u2713');
				updateBoard((char[][]) serverInput.readObject());
				ui.setEnableTurn(false);

				// trust the spaghetti, it makes the correct message without 4 if statements
				String msg = String.format("\n\n%s\n\nGame ended; %s",
						response.charAt(8) == ui.getSymbol()
								? response.matches("Player.*resigned") ? "You resigned :(" : "You won :)"
								: response.matches("Player.*resigned") ? response + " :)" : response + " :(",
						serverCode == GAME ? "please exit" : "you can still chat, or exit to play another game");
				exit(msg, "!game! game ended", INFORMATION, null, serverCode == GAME, "Game Over");
				return 2;
			} else if (response.equals("It's a tie!")) {
				updateBoard((char[][]) serverInput.readObject());
				ui.setEnableTurn(false);
				String msg = String.format("\n\n%s\n\nGame ended; %s", "It's a tie!",
						serverCode == GAME ? "please exit" : "you can still chat, or exit to play another game");
				exit(msg, "!game! game ended", INFORMATION, null, serverCode == GAME, "Game Over");
				return 2;
			}

			log("Got response: " + response);
			ui.pushMessage(response);

			// update board
			updateBoard((char[][]) serverInput.readObject());

			// enable buttons/text
			ui.setEnableTurn(starting);

			log(String.format("End %s setup", starting ? "starting" : "ending"));
		} catch (EOFException e) {
			exit("Another player unexpectedly disconnected; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! EOFException in setup()", INFORMATION, e, serverCode == GAME, "Player Disconnected");
			return 1;
		} catch (IOException e) {
			exit("Connection to Game Server lost; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in setup()", WARNING, e, serverCode == GAME, "Connection Error");
			return 1;
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers",
					"!game! ClassNotFoundException in setup()", ERROR, e, true, "Very Serious Error");
			return 1;
		}
		return 0;
	}

	/**
	 * Gets the player's move, and sends it to the server. <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return int, 0 or 1, indicating success or fail
	 */
	private int play() {

		// get the move in the worst possible way (:
		int move = -1;
		while (move == -1) {
			move = ui.getAnswer();
			if (move != -1 && move != -2 && !localGameBoard.isValid(move)) {
				ui.pushMessage("You can't play %c%s!", 65 + move / 10, move % 10 + 1);
				log(String.format("Tried to play %c%s", 65 + move / 10, move % 10 + 1));
				move = -1;
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				if (printStackTrace)
					e.printStackTrace();
			}
		}

		if (move != -2)
			ui.pushMessage(String.format("You played %c%s", 65 + move / 10, move % 10 + 1), false);

		// send the move
		try {
			serverOutput.writeObject(move);
		} catch (IOException e) {
			exit("Connection to Game Server lost; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in play()", WARNING, e, serverCode == GAME, "Connection Error");
			return 1;
		}
		log(String.format("Got and sent move: [%d, %d]", move / 10, move % 10));

		return 0;
	}

	/**
	 * Displays a pop-up with information, logs the message and potentially exits
	 * the program if an Exception occurred.
	 * 
	 * @param error_msg String, the message to show the user
	 * @param log_msg   String, the message to log in the Error Stream
	 * @param type      int, ERROR, WARNING or INFORMATION, the type of the message
	 * @param e         Exception, the exception that occurred
	 * @param terminate boolean, whether or not an error has occurred and the
	 *                  application has to exit
	 * @param title     String, the title of the pop-up
	 */
	private void exit(String error_msg, String log_msg, int type, Exception e, boolean terminate, String title) {
		if (e == null)
			log(log_msg);
		else
			logerr(log_msg, e, printStackTrace);

		JOptionPane.showMessageDialog(this.ui, error_msg, title, type);
		if (terminate)
			System.exit(0);
	}

	/**
	 * Updates the board on the UI.
	 * <p>
	 * It works by de-constructing the GameBoard at the server and re-constructing
	 * it here using the {@code char[][] array} because there is a problem when
	 * sending GameBoard objects.
	 * 
	 * @see GameBoard#GameBoard(char[][]) GameBoard(char[][])
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException            thrown when server disconnects
	 * @throws EOFException           thrown when server closes connection
	 */
	private void updateBoard(char[][] localGameBoardConstructor)
			throws ClassNotFoundException, IOException, EOFException {
		localGameBoard = new GameBoard(localGameBoardConstructor);
		ui.setScreen(localGameBoard);
	}

	/**
	 * Creates a UI to get the GameEngine options.
	 * 
	 */
	private void getClientOptions() {

		JFrame optWind = new JFrame("Select Player Options");

		JPanel optPanel = new JPanel();
		optPanel.setLayout(new BoxLayout(optPanel, BoxLayout.PAGE_AXIS));
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500, 500));
		optWind.setResizable(true);

		// upper panel: address + character
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new FlowLayout());

		JPanel addressPanel = new JPanel();
		addressPanel.setLayout(new BoxLayout(addressPanel, BoxLayout.Y_AXIS));
		// TODO maybe separate addresses to game and chat servers
		// fancy checkbox: same address for both servers
		// that on KeyEvent copies the address from one server to the other (:
		JLabel addressLabel = new JLabel("Server IP:");
		addressLabel.setPreferredSize(new Dimension(100, 50));
		JTextField addressField = new JTextField("127.0.0.1");
		addressPanel.add(addressLabel);
		addressPanel.add(addressField);

		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		String[] chars = { "X", "O", "!", "#", "$", "%", "*", "+", "A", "B", "C", "D", "E", "F", "G", "H", "I", "P",
				"Q", "R", "S", "T", "U", "V", "W", "<", "?", "~" };
		JList<String> charList = new JList<String>(chars);
		charList.setBackground(Color.BLUE);
		charList.setSelectedIndex(0);
		JScrollPane scrollList = new JScrollPane(charList);
		JLabel listLabel = new JLabel("Choose your character");
		listPanel.add(listLabel);
		listPanel.add(scrollList);

		upperPanel.add(addressPanel);
		upperPanel.add(listPanel);
		optPanel.add(Box.createVerticalGlue());

		// lower panel: printStackTrace + chat/game + color/submit buttons
		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		JCheckBox printButton = new JCheckBox("I would like to receive the full crash report on my error log");

		ButtonGroup bg = new ButtonGroup();
		JRadioButton gameChatButton = new JRadioButton("I want to play the game with chat enabled");
		JRadioButton gameOnlyButton = new JRadioButton("I want to play the game with chat disabled");
		JRadioButton chatOnlyButton = new JRadioButton("I just want to chat");
		bg.add(gameChatButton);
		bg.add(gameOnlyButton);
		bg.add(chatOnlyButton);
		gameChatButton.setSelected(true);
		lowerPanel.add(printButton);
		lowerPanel.add(Box.createVerticalGlue());
		lowerPanel.add(gameChatButton);
		lowerPanel.add(gameOnlyButton);
		lowerPanel.add(chatOnlyButton);
		lowerPanel.add(Box.createVerticalGlue());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		JButton submitButton = new JButton("Submit");
		JButton colorButton = new JButton("Select color");
		buttonPanel.add(submitButton);
		buttonPanel.add(colorButton);

		lowerPanel.add(buttonPanel);
		optPanel.add(upperPanel);
		optPanel.add(lowerPanel);

		optWind.add(optPanel);
		optWind.revalidate();

		// event listeners
		colorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				color = JColorChooser.showDialog(optWind, "Choose a color", Color.BLACK);
				if (color == null)
					color = Color.BLACK;
			}
		});

		submitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				character = charList.getSelectedValue().charAt(0);
				printStackTrace = printButton.isSelected();
				address = GameEngine.Utility.myStrip(addressField.getText(), ' ', '\t');
				if (gameChatButton.isSelected())
					serverCode = CHAT_GAME;
				else if (gameOnlyButton.isSelected())
					serverCode = GAME;
				else
					serverCode = CHAT;
				argumentsPassed = true;
				optWind.dispose();
			}
		});
	} // main GameEngine class stuff

	/**
	 * A class with a set of static utility methods to be used throughout the
	 * project.
	 */
	abstract static class Utility {

		/**
		 * Returns a string, stripped by {@code chars}.<br>
		 * Similar to python's {@code str.strip(string)}. Needed to make the program
		 * compatible with Java8+
		 * 
		 * @param string String, the string to strip
		 * @param chars  char[], the characters to strip from the string
		 * @return String, the stripped string
		 */
		public static String myStrip(String string, char... chars) {
			if (string.equals(""))
				return "";
			int firstNonCharsIndex = -1;
			int lastNonCharsIndex = -1;
			boolean inChars;
			char curr;

			for (int i = 0; i < string.length(); i++) {
				inChars = false;
				curr = string.charAt(i);
				for (char c : chars)
					inChars |= curr == c;
				if (!inChars) {
					firstNonCharsIndex = i;
					break;
				}
			}

			for (int i = string.length() - 1; i > -1; i--) {
				inChars = false;
				curr = string.charAt(i);
				for (char c : chars)
					inChars |= curr == c;
				if (!inChars) {
					lastNonCharsIndex = i;
					break;
				}
			}

			return string.substring(firstNonCharsIndex, lastNonCharsIndex + 1);
		}
	}

	/**
	 * An implementation of the {@link ChatClient} class
	 */
	private class ChatClientImpl extends ChatClient {

		@Override
		protected void displayIncomingMessage(String s) {
			ui.pushMessage(s);
		}

		@Override
		protected void exit(String error_msg, String log_msg, int type, Exception e, boolean terminate, String title) {
			GameEngine.this.exit(error_msg, log_msg, type, e, terminate, title);
		}

		@Override
		protected String getChatText() {
			String ans = "";
			if (!chatFieldSentMessages.equals("")) {
				ans = String.format("%c: %s", ui.getSymbol(), chatFieldSentMessages);
				chatFieldSentMessages = "";
			}
			return ans;
		}

		@Override
		protected void focusChat() {
			ui.focusWindow();
			super.focusChat();
		}

		/**
		 * Initializes a connection to the Chat Server.<br>
		 * Gets its input and output streams.<br>
		 * Exchanges some messages.<br>
		 * <br>
		 * If at any point something goes wrong, show pop-up message then exit.
		 * 
		 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
		 * @return int, 0 or 1, indicating success or fail
		 */
		@Override
		protected int _getChatConnection() {
			try {
				// send symbol and wait for ack
				chatOutput.writeObject(ui.getSymbol());

				ui.pushMessage((String) chatInput.readObject());

				String response = (String) chatInput.readObject();

				Matcher m = Pattern.compile(".*'(.{0,2})'.*").matcher(response);
				m.find();
				char newSymbol = m.group(1).charAt(0);
				log(String.format("Symbol after duplicate check: '%c' ('%d')", newSymbol, (int) newSymbol));

				if (newSymbol != ui.getSymbol()) {
					JOptionPane.showMessageDialog(ui,
							"Looks like you selected the same symbol as another player connected to the Chat Server.\nWorry not, because we provided you with an exclusive chess piece as your symbol!",
							"Message", INFORMATION);
					ui.setSymbol(newSymbol);
				}

				ui.pushMessage("\nChat Server said: %s", response);

				log("Connected to Chat Server successfully as player " + ui.getSymbol());

			} catch (IOException e) {
				exit("Couldn't connect to Chat Server; if you're connected to game server you may still play.\n\nIf you don't know why this happened, please inform the developers",
						"!chat! IOException in getChatConnection()\nExiting...\n", WARNING, e, serverCode == CHAT,
						"Connection Error");
				return 1;
			} catch (ClassNotFoundException e) {
				exit("Something went very wrong; please exit and inform the developers.",
						"!chat! ClassNotFoundException in chatReader.run()", ERROR, e, true, "Very Serious Error");
				return 1;
			}
			return 0;
		}
	}
}
