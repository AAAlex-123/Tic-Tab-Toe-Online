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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * Abstract class to run a server; GameServer and ChatServer inherit from it.
 */
public abstract class Server implements Logging {

	// server fields
	protected static int playerCount;
	protected static boolean printStackTrace;

	protected final ObjectInputStream[] inputs;
	protected final ObjectOutputStream[] outputs;
	protected ServerSocket server;
	
	protected final char[] symbols;
	
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
	protected abstract void run();

	/**
	 * Abstract method; initialises the server.
	 * 
	 * @see GameServer#initialiseServer()
	 * @see ChatServer#initialiseServer()
	 */
	protected abstract void initialiseServer();

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
	 * FIXME documentation, clear up spaghetti mby
	 */
	protected void getServerOptions() {

		JFrame optWind = new JFrame("Select Server Options");
		JPanel optPanel = new JPanel();
		optPanel.setLayout(new BoxLayout(optPanel, BoxLayout.PAGE_AXIS));
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500, 300));
		optWind.setResizable(false);

		JPanel lsPanelExt = new JPanel();
		lsPanelExt.setLayout(new BoxLayout(lsPanelExt, BoxLayout.Y_AXIS));
		JLabel lsLabel = new JLabel("Choose the number of players");
		String[] plOptions = { "2 players", "3 players", "4 players" };
		JList<String> list = new JList<String>(plOptions);
		list.setBackground(Color.BLUE);
		Font font = new Font("Serif", Font.BOLD, 25);
		list.setFont(font);
		list.setSelectedIndex(0);

		JPanel lsPanelInt = new JPanel();
		lsPanelInt.setLayout(new FlowLayout(FlowLayout.CENTER));
		lsPanelInt.add(list);

		lsPanelExt.add(lsLabel);
		lsPanelExt.add(lsPanelInt);

		optPanel.add(lsPanelExt);

		JRadioButton b1 = new JRadioButton("I would like to receive crash reports on my command line");
		optPanel.add(b1);

		JButton submitBut = new JButton("Submit");
		submitBut.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				playerCount = list.getSelectedIndex() + 2;
				printStackTrace = b1.isSelected();
				optWind.setVisible(false);
				argumentsPassed = true;
			}

		});
		optPanel.add(Box.createVerticalGlue());
		optPanel.add(submitBut);

		optWind.add(optPanel);
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
				logerr("Error in broadcast()");
				if (printStackTrace)
					e.printStackTrace();
			} catch (NullPointerException e) {
				;
			}
		}

		log("Broadcasted: %s", String.format(msg, args));
	}
}
