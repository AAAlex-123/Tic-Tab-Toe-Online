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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public abstract class Server {

	protected static int playerCount,boardSize;
	protected static boolean printStackTrace, argumentsPassed = false;

	protected final ObjectInputStream[] inputs;
	protected final ObjectOutputStream[] outputs;
	protected ServerSocket server;

	public Server() {
		getServerOptions();
		while(!argumentsPassed) {
			try {Thread.sleep(500);}
			catch(InterruptedException e) {e.printStackTrace();}
		}
                     
		inputs = new ObjectInputStream[playerCount];
		outputs = new ObjectOutputStream[playerCount];
	}
	protected abstract void run();
	protected abstract void initialiseServer();
	protected abstract void getConnections();
	
	/**
	 * Gets server options from player using GUI.
	 * Assigns values to the playerCount and printStackTrace variables
	 * 
	 * FIXME
	 */
	protected void getServerOptions() {
		
		JFrame optWind = new JFrame("Select Server Options");
		JPanel optPanel = new JPanel();
		optPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500,300));
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
		listPanel.add(boardPanel);
		
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

	/**
	 * Sends message <code>msg</code> to every client connected<br>
	 * <code>System.out.printf(text, args)</code>
	 * 
	 * @param text    String, text to send
	 * @param args    Object[], arguments
	 */
	protected void broadcast(String msg, Object... args) {
		for (int i = 0; i < playerCount; i++) {
			try {
				outputs[i].writeObject(String.format(msg, args));
			} catch (IOException e) {
				logerr("Error while broadcasting");
				if (printStackTrace)
					e.printStackTrace();
			}
		}

		log("Broadcasted: %s", String.format(msg, args));
	}

	/**
	 * Same as <code>System.out.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	protected static void log(String text, Object... args) {
		System.out.printf(text+"\n", args);
	}

	/**
	 * Same as <code>System.err.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	protected static void logerr(String text, Object... args) {
		System.err.printf(text+"\n", args);
	}
}

