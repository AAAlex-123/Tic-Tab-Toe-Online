package ttt_online;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

@SuppressWarnings("serial")
public class GameUI extends JFrame {
	/*
	 * Use getSymbol, getColor to get the char/color of each UI. Then use the public
	 * method setCustomOptions by giving a char and color array such that ```char of
	 * player i -> color of player i```. In any other case the game will use a blue X
	 * and a red O for the players
	 */

	private char name;
	private Color color;
	private String chatText = "";
	private boolean dataReceived = false;
	private int answer = -1;
	private final JLabel error_msg;
	private final Screen screen;
	private final JTextArea log;
	private final JTextField player, move, chatField;
	private final JButton submitB, disconnectB, chatButton;
	private final JPanel autismPanel, inputPanel, chatPanel, logPanel;
	private final JScrollPane scroll;

	public static void main(String[] args) {
		GameUI ui = new GameUI();
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(320, 720);
		ui.setVisible(true);
		ui.setResizable(true);
	}

	public GameUI() {
		super("Naughts & Crosses Online");

		getOptions();
		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

		// screen
		screen = new Screen();
		screen.setPreferredSize(new Dimension(500, 500));

		// logPanel
		logPanel = new JPanel();
		logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));

		// logPanel -- scroll -- log
		log = new JTextArea("This is a message log\n");
		log.setEditable(false);
		// scroll to the bottom when new messages are pushed. pls don't remove :)
		((DefaultCaret) log.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		scroll = new JScrollPane(log, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setPreferredSize(new Dimension(100, 250));

/*ADD*/ logPanel.add(scroll);

		// logPanel -- chatPanel
		chatPanel = new JPanel();
		chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.X_AXIS));

		// logPanel -- chatPanel -- chatField / chatButton
		chatField = new JTextField();
		chatButton = new JButton("Send");
		chatButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				chatText += (chatText.equals("") ? "" : "\n") + chatField.getText();
				chatField.setText("");
			}
		});

/*AD */ chatPanel.add(chatField);
/*ADD*/ chatPanel.add(chatButton);
/*ADD*/ logPanel.add(chatPanel);

		// autismPanel
		autismPanel = new JPanel();
		autismPanel.setLayout(new FlowLayout()); // because there is no other way to force Layout to cooperate

		// autismPanel -- player
		player = new JTextField("You are player " + name);
		player.setEditable(false);

/*ADD*/ autismPanel.add(player);

		// error_msg
		error_msg = new JLabel("Invalid input: Insert [letter][number]");
		error_msg.setVisible(false);

		// inputPanel
		inputPanel = new JPanel();
		inputPanel.setLayout(new FlowLayout());

		// inputPanel -- move / submitB
		move = new JTextField("Your next move", 10);
		submitB = new JButton("Submit");
		submitB.setMnemonic(KeyEvent.VK_SPACE);
		submitB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dataReceived) {
					JOptionPane.showMessageDialog(getContentPane(),
							"Please wait for the other players to finish picking their characters", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				String input = move.getText().toUpperCase().strip();
				if (!input.matches("[A-E][1-5]")) {
					error_msg.setVisible(true);
					return;
				}
				answer = convertInput(input);
				// un-comment if you need for debugging
//				pushMessage(String.format("Player %s played %s", name, move.getText()));
				move.setText("");
				error_msg.setVisible(false);
			}
		});
/*ADD*/ inputPanel.add(move);
/*ADD*/ inputPanel.add(submitB);

		// disconnectB
		disconnectB = new JButton("Resign");
		disconnectB.setMnemonic(KeyEvent.VK_R);
		disconnectB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!dataReceived) {
					JOptionPane.showMessageDialog(getContentPane(),
							"Please wait for the other players to finish picking their characters", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				answer = -2;
				// un-comment if you need for debugging
//				pushMessage("Player "+ name +" resigned the game!");
			}
		});

/* ADD TO JFRAME */
		add(Box.createRigidArea(new Dimension(50, 35)));
		add(autismPanel);
		add(screen);
		add(Box.createRigidArea(new Dimension(20, 15)));
		add(inputPanel);
		add(error_msg);
		add(Box.createRigidArea(new Dimension(50, 15)));
		add(logPanel);
		add(Box.createRigidArea(new Dimension(50, 35)));
		add(disconnectB);
	}

	public void pushMessage(String mes) {
		log.setText(String.format("%s%s\n", log.getText(), mes));
	}
	
	// for future use so we can do:
	// ui.pushMessage("player %s played %d", ui.getSymbol, move);
	// instead of:
	// ui.pushMessage(String.format("player %s played %d", ui.getSymbol, move));
	// similar to how GameEngine#log works
	public void pushMessage(String mes, Object... args) {
		log.setText(String.format("%s%s\n", log.getText(), mes, args));
	}

	public void pushMessage(String mes, boolean newline) {
		log.setText(String.format("%s%s%s", log.getText(), mes, newline ? "\n" : ""));
	}

	public void setScreen(GameBoard gboard) {
		screen.board = gboard;
		screen.repaint();
		// Wait until it loads then update the whole thing.
		// JVM has forced my hand
		// spaghetti
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		this.update(this.getGraphics());
	}

	public void setEnableTurn(boolean enable) {
		submitB.setEnabled(enable);
		disconnectB.setEnabled(enable);
		move.setEnabled(enable);
		if (!enable)
			move.setText("Your next move");
		else
			move.setText("");
	}

	public void setEnableChat(boolean enable) {
		chatButton.setEnabled(enable);
		chatField.setEnabled(enable);
		if (!enable)
			chatField.setText("You're not connected to chat server");
		else
			chatField.setText("");
	}

	public int getAnswer() {
		int ans = -1;
		if (this.answer != -1) {
			ans = this.answer;
			this.answer = -1;
		}
		return ans;
	}

	public char getSymbol() {
		return this.name;
	}
	
	public void setSymbol(char symbol) {
		this.name = symbol;
		player.setText("You are player " + name);
	}

	public Color getColor() {
		return this.color;
	}

	// same logic as getAnswer()
	public String getChatText() {
		String ans = "";
		if (!this.chatText.equals("")) {
			ans = this.chatText;
			this.chatText = "";
		}
		return ans;
	}

	/**
	 * Converts the player's input from <code>Char-Int</code> to
	 * <code>Int-Int</code> so that the GameBoard can understand it
	 * 
	 * @param str String, the player's input
	 * @return int, the data the GameBoard understands
	 */
	private static int convertInput(String str) {
		char row = str.charAt(0);
		int index;
		switch (row) {
		case '\u0041':
			index = 0;
			break;

		case '\u0042':
			index = 10;
			break;

		case '\u0043':
			index = 20;
			break;

		case '\u0044':
			index = 30;
			break;

		default:
			index = 40;
		}

		index += Integer.parseInt(Character.toString(str.charAt(1))) - 1;
		return index;
	}

	// gets character player and color
	private void getOptions() {

		Object[] chars = { GameEngine.X, GameEngine.O, '\u0021', '\u0022', '\u0023', '\u0024', '\u0025', '\u002A',
				'\u002B', '\u0041', '\u0042', '\u0043', '\u0044', '\u0045', '\u0046', '\u0047', '\u0048', '\u0049',
				'\u0050', '\u0051', '\u0052', '\u0053', '\u0054', '\u0055', '\u0056', '\u0057', '\u003C', '\u003F',
				'\u007E' };

		boolean correct = false;
		do {
			try {
				this.name = (Character) JOptionPane.showInputDialog(null, "Select one of the following characters",
						"Character Selection", JOptionPane.PLAIN_MESSAGE, null, chars, chars[0]);
				correct = true;
			} catch (NullPointerException e) {
				JOptionPane.showMessageDialog(this, "Please select a character", "Error", JOptionPane.ERROR_MESSAGE);
				correct = false;
			}
		} while (!correct);

		Color col;
		do {
			col = JColorChooser.showDialog(this, "Choose a color", Color.BLACK);
			if (col == null)
				JOptionPane.showMessageDialog(this, "Please select a color", "Error", JOptionPane.ERROR_MESSAGE);
		} while (col == null);

		this.color = col;
	}

	public void setCustomOptions(char[] chars, Color[] colors) {
		if (colors.length != chars.length)
			throw new RuntimeException("Color and character arrays must be of same length");

		for (int i = 0; i < colors.length; i++)
			screen.colorMap.put(chars[i], colors[i]);

		dataReceived = true;
	}

	private class Screen extends JPanel {
		private GameBoard board;
		private final HashMap<Character, Color> colorMap = new HashMap<Character, Color>();
		private String[] letters = { "A", "B", "C", "D", "E" };

		public Screen() {
			board = new GameBoard();
		}

		@Override
		public void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.setFont(new Font("Serif", Font.PLAIN, 60));

			// paint board
			for (int i = 1; i < 6; i++) {
				g.drawString(Integer.toString(i), i * 53, 50);// 53 instead of 50 to compensate for character width
				for (int j = 1; j < 6; j++) {
					g.drawString(letters[j - 1], 0, (j + 1) * 50);
					g.drawRect(i * 50, j * 50, 50, 50);
				}
			}

			// paint marks
			g.setFont(new Font("Monospaced", Font.BOLD, 65));
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					char c = board.getBoard()[i][j];
					if (c != GameEngine.DASH) {
						g.setColor(colorMap.get(c));
						g.drawString(Character.toString(c), (j + 1) * 50, (i + 2) * 50);
					}
				}
			}
		}
	}
}
