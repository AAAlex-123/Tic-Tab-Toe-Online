package ttt_online;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

final class GameUI extends JFrame {

	// TODO maybe add documentation at at least the non-private methods?

	private static final long serialVersionUID = 1L;

	// UI variables
	private final Color color;
	private char name;
	private String chatText = "";
	private boolean dataReceived = false;
	private int answer = -1;

	// UI components
	private final JLabel errorMsg;
	private final Screen screen;
	private final JTextArea logTextArea;
	private final JTextField playerTextArea, moveTextArea, chatTextArea;
	private final JButton moveButton, resignButton, chatButton;
	private final JPanel autismPanel, movePanel, chatPanel, logPanel;
	private final JScrollPane scroll;
	private static final String[] letters = { "A", "B", "C", "D", "E", "F", "G", "H" };

	// UI Constants
	private final int HEIGHT_MULTIPLIER; // used to calculate graphics size
	private final int SCREEN_WIDTH;
	private final int SCREEN_HEIGHT;

	GameUI(Color color, char name, int heightMultiplier) {
		super("Naughts & Crosses Online");
		this.color = color;
		this.name = name;

		SCREEN_WIDTH = 600;
		SCREEN_HEIGHT = 600;
		HEIGHT_MULTIPLIER = heightMultiplier;

		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

		// screen
		screen = new Screen();
		screen.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));

		// logPanel
		logPanel = new JPanel();
		logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));

		// logPanel -- scroll -- log
		logTextArea = new JTextArea("This is a message log\n");
		logTextArea.setEditable(false);

		// scroll to the bottom when new messages are pushed
		((DefaultCaret) logTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		scroll = new JScrollPane(logTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setPreferredSize(new Dimension(100, 250));

/*ADD*/ logPanel.add(scroll);

		// logPanel -- chatPanel
		chatPanel = new JPanel();
		chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.X_AXIS));

		// logPanel -- chatPanel -- chatField / chatButton
		chatTextArea = new JTextField();
		chatTextArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {;}

			@Override
			public void keyReleased(KeyEvent e) {;}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					sendChat();
			}
		});
		chatButton = new JButton("Send");
		chatButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendChat();
			}
		});

/*ADD*/ chatPanel.add(chatTextArea);
/*ADD*/ chatPanel.add(chatButton);
/*ADD*/ logPanel.add(chatPanel);

		// autismPanel
		autismPanel = new JPanel();
		autismPanel.setLayout(new FlowLayout()); // because there is no other way to force Layout to cooperate

		// autismPanel -- player
		playerTextArea = new JTextField("You are player " + name);
		playerTextArea.setEditable(false);

/*ADD*/ autismPanel.add(playerTextArea);

		// error_msg
		errorMsg = new JLabel("");
		errorMsg.setVisible(false);

		// inputPanel
		movePanel = new JPanel();
		movePanel.setLayout(new FlowLayout());

		// inputPanel -- move / submitB
		moveTextArea = new JTextField("Your next move", 10);
		moveTextArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {;}

			@Override
			public void keyReleased(KeyEvent e) {;}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					submitMove();
			}
		});
		moveButton = new JButton("Submit");
		moveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				submitMove();
			}
		});
/*ADD*/ movePanel.add(moveTextArea);
/*ADD*/ movePanel.add(moveButton);

		// disconnectB
		resignButton = new JButton("Resign");
		resignButton.setMnemonic(KeyEvent.VK_R);
		resignButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dataReceived)
					JOptionPane.showMessageDialog(getContentPane(),
							"Please wait for the other players to finish picking their characters", "Error",
							JOptionPane.ERROR_MESSAGE);
				else 
					answer = -2;
			}
		});

/* ADD TO JFRAME */
		add(Box.createRigidArea(new Dimension(50, 35)));
		add(autismPanel);
		add(screen);
		add(Box.createRigidArea(new Dimension(20, 15)));
		add(movePanel);
		add(errorMsg);
		add(Box.createRigidArea(new Dimension(50, 15)));
		add(logPanel);
		add(Box.createRigidArea(new Dimension(50, 35)));
		add(resignButton);

		// when user clicks on window, `move` gets focus
		addWindowFocusListener(new WindowAdapter() {
			public void windowGainedFocus(WindowEvent e) {
				moveTextArea.requestFocusInWindow();
			}
		});

		// initialize with empty board so the Screen can render
		// Screen uses screen.board.size so we need to initialize it
		screen.board = new GameBoard(0, 5);
	}

	// TODO DELETEME comments below if not necessary
	// Use String.format instead of this /// nope
	void pushMessage(String mes, Object... args) {
		logTextArea.setText(String.format(String.format("%s%s\n", logTextArea.getText(), mes), args));
	}

	// TODO DELETEME comments below if not necessary
	// Append \n instead of this /// nope
	/*
	 * Yes we need `boolean newline` even though it's always false because otherwise
	 * when `Object... args` is null (i.e. a simple message, `pushMessage("owo")`
	 * instead of `pushMessage("%s pog", "text")`), this method is called and we
	 * don't get a newline when we need it. However, for some reason,
	 * `pushMessage("text", true)` calls this method instead of the other one, even
	 * though both fit. Maybe the compiler looks for the "most matching"?
	 */
	void pushMessage(String mes, boolean newline) {
		logTextArea.setText(String.format("%s%s%s ", logTextArea.getText(), mes, newline ? "\n" : ""));
	}

	void setScreen(GameBoard gameBoard) {
		// TODO DELETEME comments below if not necessary
		// set size now that we know how big the board is
		// for some reason maybe doesn't work as expected (?)
		// maybe because `screen.setPreferredSize()` is called at constructor?
		screen.board = gameBoard;
		screen.setPreferredSize(new Dimension(100 * gameBoard.SIZE, 100 * gameBoard.SIZE));
		screen.repaint();
		errorMsg.setText(String.format("Invalid input! Please insert: [A-%s][1-%d]",
				letters[screen.board.SIZE - 1], screen.board.SIZE));

		// Wait until it loads then update the whole thing.
		// TODO DELETEME comments below if not necessary
		// JVM has forced my hand
		// yes, this.revalidate() doesn't work here
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		this.update(this.getGraphics());
	}

	// ----- CHAT FIELDS ENABLE METHODS -----
	void setEnableTurn(boolean enable) {
		moveButton.setEnabled(enable);
		resignButton.setEnabled(enable);
		moveTextArea.setEnabled(enable);
		if (!enable)
			moveTextArea.setText("Your next move");
		else {
			moveTextArea.setText("");
			// TODO DELETEME comments below if not necessary
			// bring window to front and
			// get focus when it's your turn
			toFront();
			moveTextArea.requestFocusInWindow();
		}
	}

	void setEnableChat(boolean enable) {
		chatButton.setEnabled(enable);
		chatTextArea.setEnabled(enable);
		if (!enable)
			chatTextArea.setText("You're not connected to chat server");
		else
			chatTextArea.setText("");
	}

	// ----- FOCUS METHODS -----
	void focusMove() {
		focusWindow();
		moveTextArea.requestFocusInWindow();
	}

	void focusChat() {
		focusWindow();
		chatTextArea.requestFocusInWindow();
	}

	void focusWindow() {
		toFront();
	}

	// ----- GETTERS AND SETTERS -----
	void setSymbol(char symbol) {
		name = symbol;
		playerTextArea.setText("You are player " + symbol);
	}

	char getSymbol() {
		return name;
	}

	Color getColor() {
		return color;
	}

	int getAnswer() {
		int ans = -1;
		if (answer != -1) {
			ans = answer;
			answer = -1;
		}
		return ans;
	}

	String getChatText() {
		String ans = "";
		if (!chatText.equals("")) {
			ans = chatText;
			chatText = "";
		}
		return ans;
	}

	void setCustomOptions(char[] chars, Color[] colors) {
		// Is this ever thrown though?
		// Server manages everything and the clients send correct data (hopefully)
		if (colors.length != chars.length)
			throw new RuntimeException("Color and character arrays must be of same length");

		for (int i = 0; i < colors.length; i++)
			screen.colorMap.put(chars[i], colors[i]);

		dataReceived = true;
	}

	// ----- PRIVATE METHODS / CLASSES -----

	/**
	 * Converts the player's input from {@code Char-Int} to {@code Int-Int} so that
	 * the GameBoard can understand it
	 * 
	 * @param str String, the player's input
	 * @return int, the data the GameBoard understands
	 */
	private static int convertInput(String str) {
		String characters = "ABCDEFGH";
		return characters.indexOf(str.charAt(0)) * 10 + Integer.parseInt(Character.toString(str.charAt(1))) - 1;
	}

	private void submitMove() {
		if (!dataReceived) {
			JOptionPane.showMessageDialog(getContentPane(), "Please wait for the other players to connect", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String input = GameEngine.Utility.myStrip(moveTextArea.getText().toUpperCase(), ' ', '\t');
		if (!input.matches(String.format("[A-%s][1-%d]", letters[screen.board.SIZE - 1], screen.board.SIZE))) {
			errorMsg.setVisible(true);
		} else {
			errorMsg.setVisible(false);
			answer = convertInput(input);
			moveTextArea.setText("");
		}
	}

	private void sendChat() {
		chatText += (chatText.equals("") ? "" : "\n") + chatTextArea.getText();
		chatTextArea.setText("");
	}

	private class Screen extends JPanel {

		private static final long serialVersionUID = -4300424235585763140L;
		private GameBoard board;
		private final HashMap<Character, Color> colorMap = new HashMap<Character, Color>();

		public Screen() {
			;
		}

		@Override
		public void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.setFont(new Font("Serif", Font.PLAIN, 30 * HEIGHT_MULTIPLIER));

			// paint board
			for (int i = 1; i < board.SIZE + 1; i++) {
				// 53 instead of 50 to compensate for character width
				g.drawString(Integer.toString(i), i * 26 * HEIGHT_MULTIPLIER, 25 * HEIGHT_MULTIPLIER);
				for (int j = 1; j < board.SIZE + 1; j++) {
					g.drawString(letters[j - 1], 0, (j + 1) * 25 * HEIGHT_MULTIPLIER);
					g.drawRect(i * 25 * HEIGHT_MULTIPLIER, j * 25 * HEIGHT_MULTIPLIER, 25 * HEIGHT_MULTIPLIER,
							25 * HEIGHT_MULTIPLIER); // kill me :(
				}
			}

			// paint marks
			g.setFont(new Font("Monospaced", Font.BOLD, 32 * HEIGHT_MULTIPLIER));
			for (int i = 0; i < board.SIZE; i++) {
				for (int j = 0; j < board.SIZE; j++) {
					char c = board.getBoard()[i][j];
					if (c != GameEngine.DASH) {
						g.setColor(colorMap.get(c));
						g.drawString(Character.toString(c), (j + 1) * 25 * HEIGHT_MULTIPLIER,
								(i + 2) * 25 * HEIGHT_MULTIPLIER);
					}
				}
			}
		}
	}
}
