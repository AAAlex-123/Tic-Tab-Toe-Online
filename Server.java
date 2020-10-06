package ttt_online;

import java.awt.Color;
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
 * Abstract class to run a server; GameServer and ChatServer inherit from it.
 */
public abstract class Server implements Logging,Runnable {

	// server fields
	protected static int playerCount;
	protected static boolean printStackTrace;
	protected static int gameConnected;
	protected static int chatConnected;

	protected final ObjectInputStream[] inputs;
	protected final ObjectOutputStream[] outputs;
	protected Screen screen = new Screen(); 
	protected ServerSocket server;
	
	protected final char[] symbols;
	protected int boardSize;
	
	// array of chess piece characters used to replace duplicates
	protected final ArrayList<Character> chessPieces = new ArrayList<Character>(
			Arrays.asList('\u2654', '\u2655', '\u2656', '\u2657', '\u2658'));

	// used to determine when user has entered options to the UI
	protected static boolean argumentsPassed = false;

	/**
	 * Constructor of Server superclass. <br>
	 * Gets options from ui and initialises inputs and outputs arrays.
	 * 
	 * @see Server#getServerOptions() getServerOptions
	 */
	public Server() {
		getServerOptions();
		while (!argumentsPassed) {
			try {Thread.sleep(500);}
			catch (InterruptedException e) {e.printStackTrace();}
		}

		gameConnected = 0;
		chatConnected = 0;
		inputs = new ObjectInputStream[playerCount];
		outputs = new ObjectOutputStream[playerCount];
		symbols = new char[playerCount];

		screen.updateGameConnectionCounter(0);
		screen.updateChatConnectionCounter(0);
		screen.setVisible(true);
		screen.setSize(new Dimension(500,300));
		screen.setResizable(true);
		screen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	/**
	 * Constructor of Server superclass. <br>
	 * Gets set options directly as arguments bypassing the UI.
	 *
	 * @param int playerCount the number of connections
	 * @param boolean printStackTrace whether to show detailed crash reports
	 * @see Server#getServerOptions() getServerOptions
	 */
	public Server(int playerCount,boolean printStackTrace) {
		Server.printStackTrace = printStackTrace;
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
	 * Abstract method; initialises the server.
	 * 
	 * @see GameServer#initialiseServer()
	 * @see ChatServer#initialiseServer()
	 */
	protected abstract void initializeServer();

	/**
	 * Abstract method; initialises connections to clients.
	 * 
	 * @see GameServer#getConnections()
	 * @see ChatServer#getConnections()
	 */
	protected abstract void getConnections();

	/**
	 * Gets server options from player using GUI. Assigns values to the playerCount
	 * and printStackTrace variables
	 * 
	 */
	protected void getServerOptions() {

		JFrame optWind = new JFrame(String.format("Select %s Options", this.getClass().getSimpleName()));
		JPanel optPanel = new JPanel();
		optPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500, 300));
		optWind.setResizable(false);
		
		//listPanel = playerPanel + board Panel 
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel,BoxLayout.X_AXIS));
		
		//playerPanel = playerLabel + playerList
		JPanel playerPanel = new JPanel();
		playerPanel.setLayout(new BoxLayout(playerPanel,BoxLayout.Y_AXIS));
		JLabel playerLabel = new JLabel("Choose the number of players");
		String[] plOptions = {"2 players","3 players","4 players"};
		JList<String>playerList = new JList<String>(plOptions);
		playerList.setBackground(Color.BLUE);
		Font font = new Font("Serif", Font.BOLD, 25);
		playerList.setFont(font);
		playerList.setSelectedIndex(0);
		playerPanel.add(playerLabel);
		playerPanel.add(playerList);
		
		//boardPanel = boardLabel + (scroll) boardList
		JPanel boardPanel = new JPanel();
		boardPanel.setLayout(new BoxLayout(boardPanel,BoxLayout.Y_AXIS));
		JLabel boardLabel= new JLabel("Choose the boards size");
		String[] boardOptions = {"3x3","4x4","5x5","6x6","7x7","8x8"};
		JList<String>boardLs = new JList<String>(boardOptions);
		boardLs.setBackground(Color.BLUE);
		boardLs.setFont(font);
		boardLs.setSelectedIndex(2);
		JScrollPane scrollList = new JScrollPane(boardLs);
		scrollList.setPreferredSize(new Dimension(100,100));
		boardPanel.add(boardLabel);
		boardPanel.add(scrollList);
		
		listPanel.add(playerPanel);
		listPanel.add(Box.createRigidArea(new Dimension(50,50)));
		if (this.getClass().getSimpleName().equals("GameServer")) {
			listPanel.add(boardPanel);
		} else {
			listPanel.add(Box.createRigidArea(new Dimension(150,150)));
		}
		
		optPanel.add(Box.createRigidArea(new Dimension(20,20)));
		optPanel.add(listPanel);
		optPanel.add(Box.createRigidArea(new Dimension(50,50)));
		JCheckBox b1 = new JCheckBox("I would like to receive crash reports on my command line"); 
		optPanel.add(b1);

		JButton submitBut = new JButton("Submit");
		submitBut.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				playerCount = playerList.getSelectedIndex()+2;
				printStackTrace = b1.isSelected();
				boardSize = boardLs.getSelectedIndex()+3;
				optWind.setVisible(false);
				argumentsPassed = true;
			}

		});
		optPanel.add(Box.createVerticalGlue());
		optPanel.add(submitBut);

		optWind.add(optPanel);
	}
	
	@Override 
	public void log(String text) {
		screen.pushMessage(text);
	}
	
	/**
	 * Sends message <code>msg</code> to every client connected;<br>
	 * uses <code>System.out.printf(text, args)</code>
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
	
	@SuppressWarnings("serial")
	protected class Screen extends JFrame{
		private final JTextArea log;
		private final JScrollPane scroll;
		private final JPanel panel;
		private final JLabel playerLabel;
		public Screen() {
			super("Server Log");
			panel = new JPanel(); //used for layout
			panel.setLayout(new BoxLayout(panel,BoxLayout.PAGE_AXIS));
			playerLabel = new JLabel();
			log = new JTextArea();
			((DefaultCaret) log.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			scroll = new JScrollPane(log, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scroll.setPreferredSize(new Dimension(200,200));
			panel.add(playerLabel);
			panel.add(scroll);
			add(panel);
		}
		
		public void pushMessage(String msg) {
			log.setText(log.getText()+"\n"+msg);
		}
		
		private void updatePlayerLabel() {
			playerLabel.setText(String.format(
					"Game connections: %d/%d\t\tChat connections: %d/%d", gameConnected, Server.playerCount, chatConnected, Server.playerCount
					));
		}
		
		public void updateGameConnectionCounter(int i) {
			gameConnected += i;
			if (i == 0) gameConnected = 0;
			updatePlayerLabel();
		}		
		public void updateChatConnectionCounter(int i) {
			chatConnected += i;
			if (i == 0) chatConnected = 0;
			updatePlayerLabel();
		}
	}
}
