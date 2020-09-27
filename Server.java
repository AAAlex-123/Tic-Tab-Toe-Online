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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public abstract class Server {

	protected static int playerCount;
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
		optPanel.setLayout(new BoxLayout(optPanel,BoxLayout.PAGE_AXIS));
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500,300));
		optWind.setResizable(false);
		
		
		JPanel lsPanelExt = new JPanel();
		lsPanelExt.setLayout(new BoxLayout(lsPanelExt,BoxLayout.Y_AXIS));
		JLabel lsLabel = new JLabel("Choose the number of players");
		String[] plOptions = {"2 players","3 players","4 players"};
		JList<String>list = new JList<String>(plOptions);
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
				playerCount = list.getSelectedIndex()+2;
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

