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


public class GameUI extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private char name;
	private Color color;
	private String chatText = "";
	private boolean dataReceived = false;
	private int answer = -1;
	
	// UI components
	private final JLabel error_msg;
	private final Screen screen;
	private final JTextArea logTextArea;
	private final JTextField playerTextArea, moveTextArea, chatTextArea;
	private final JButton moveButton, resignButton, chatButton;
	private final JPanel autismPanel, movePanel, chatPanel, logPanel;
	private final JScrollPane scroll;
	private static String[] letters = { "A", "B", "C", "D", "E","F","G","H"};
	
	// Constants
	private static int HEIGHT_MULTIPLIER;  // used to calculate graphics size
	private static int SCREEN_WIDTH;
	private static int SCREEN_HEIGHT;


	public GameUI(Color color, char name, int heightMultiplier) {
		super("Naughts & Crosses Online");
		this.color = color;
		this.name = name;
		
		SCREEN_WIDTH = 600;
		SCREEN_HEIGHT = 600;
		GameUI.HEIGHT_MULTIPLIER = heightMultiplier;
  
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
		error_msg = new JLabel("Invalid input: Insert [letter][number]");
		error_msg.setVisible(false);

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
				if (!dataReceived) {
					JOptionPane.showMessageDialog(getContentPane(),
							"Please wait for the other players to finish picking their characters", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				answer = -2;
			}
		});

/* ADD TO JFRAME */
		add(Box.createRigidArea(new Dimension(50, 35)));
		add(autismPanel);
		add(screen);
		add(Box.createRigidArea(new Dimension(20, 15)));
		add(movePanel);
		add(error_msg);
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
		screen.board = new GameBoard(0,5);
	}
	

	public void pushMessage(String mes, Object... args) {//Use String.format instead of this
		logTextArea.setText(String.format("%s%s\n", logTextArea.getText(), mes, args));
	}

	public void pushMessage(String mes, boolean newline) {//Append \n instead of this
		logTextArea.setText(String.format("%s%s%s", logTextArea.getText(), mes, newline ? "\n" : ""));
	}

	public void setScreen(GameBoard gboard) {
		// set size now that we know how big the board is
		// for some reason maybe doesn't work as expected (?)
		// maybe because `screen.setPreferredSize()` is called at constructor?
		screen.board = gboard;
		screen.setPreferredSize(new Dimension(100*gboard.SIZE, 100*gboard.SIZE));
		screen.repaint();
		
		// Wait until it loads then update the whole thing.
		// JVM has forced my hand
		// yes, this.revalidate() doesn't work here
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		this.update(this.getGraphics());
	}

	public void setEnableTurn(boolean enable) {
		moveButton.setEnabled(enable);
		resignButton.setEnabled(enable);
		moveTextArea.setEnabled(enable);
		if (!enable)
			moveTextArea.setText("Your next move");
		else {
			moveTextArea.setText("");
			// bring window to front and
			// get focus when it's your turn
			toFront();
			moveTextArea.requestFocusInWindow();
		}
	}

	public void setEnableChat(boolean enable) {
		chatButton.setEnabled(enable);
		chatTextArea.setEnabled(enable);
		if (!enable)
			chatTextArea.setText("You're not connected to chat server");
		else
			chatTextArea.setText("");
	}
	
	public void focusMove() {
		focusWindow();
		moveTextArea.requestFocusInWindow();
	}
	
	public void focusChat() {
		focusWindow();
		chatTextArea.requestFocusInWindow();
	}
	
	public void focusWindow() {
		toFront();
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
		playerTextArea.setText("You are player " + name);
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
		String characters = "ABCDEFGH";
		return characters.indexOf(str.charAt(0))*10+Integer.parseInt(Character.toString(str.charAt(1))) - 1;
	}

	public void setCustomOptions(char[] chars, Color[] colors) {
		if (colors.length != chars.length)
			throw new RuntimeException("Color and character arrays must be of same length");

		for (int i = 0; i < colors.length; i++)
			screen.colorMap.put(chars[i], colors[i]);

		dataReceived = true;
	}
	
	private void submitMove() {
    	if (!dataReceived) {
					JOptionPane.showMessageDialog(getContentPane(),
							"Please wait for the other players to finish picking their characters", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
			}
      String input = GameEngine.Utility.myStrip(moveTextArea.getText().toUpperCase(), ' ', '\t');
      if (!input.matches(String.format("[A-%s][1-%d]",letters[screen.board.SIZE-1], screen.board.SIZE))) {
        error_msg.setVisible(true);
        return;
      }
      answer = convertInput(input);
      moveTextArea.setText("");
      error_msg.setVisible(false);
	}
	
	private void sendChat() {
		chatText += (chatText.equals("") ? "" : "\n") + chatTextArea.getText();
		chatTextArea.setText("");
	}

	private class Screen extends JPanel {

		private static final long serialVersionUID = -4300424235585763140L;
		private GameBoard board;
		private final HashMap<Character, Color> colorMap = new HashMap<Character, Color>();
	
		public Screen() {;}

		@Override
		public void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.setFont(new Font("Serif", Font.PLAIN, 30*HEIGHT_MULTIPLIER));

			// paint board
			for (int i = 1; i < board.SIZE+1; i++) {
				g.drawString(Integer.toString(i), i * 26*HEIGHT_MULTIPLIER, 25*HEIGHT_MULTIPLIER);// 53 instead of 50 to compensate for character width
				for (int j = 1; j < board.SIZE+1; j++) {
					g.drawString(letters[j - 1], 0, (j + 1) * 25*HEIGHT_MULTIPLIER);
					g.drawRect(i * 25*HEIGHT_MULTIPLIER, j * 25*HEIGHT_MULTIPLIER, 25*HEIGHT_MULTIPLIER, 25*HEIGHT_MULTIPLIER); //kill me
				}
			}

			// paint marks
			g.setFont(new Font("Monospaced", Font.BOLD, 32*HEIGHT_MULTIPLIER));
			for (int i = 0; i < board.SIZE; i++) {
				for (int j = 0; j < board.SIZE; j++) {
					char c = board.getBoard()[i][j];
					if (c != GameEngine.DASH) {
						g.setColor(colorMap.get(c));
						g.drawString(Character.toString(c), (j + 1) * 25*HEIGHT_MULTIPLIER, (i + 2) * 25*HEIGHT_MULTIPLIER);
					}
				}
			}
		}
	}
}
