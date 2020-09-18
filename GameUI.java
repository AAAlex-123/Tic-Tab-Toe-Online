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

@SuppressWarnings("serial")
public class GameUI extends JFrame{
	/*
	 * Use getSymbol, getColor to get the char/color of each UI
	 * Then use the public method setCustomOptions by giving a char and color array such that ```char of player i -> color of player i```
	 * In any other case the game will use a blue X and a red O for the players
	 */
	
	private char name;
	private Color color;
	private boolean dataReceived = false;
	private int answer = -1;
	private final JLabel error_msg;
	private final Screen screen;
	private final JTextArea log;
	private final JTextField player,move;
	private final JButton submitB,disconnectB;
	private final JPanel autismPanel,inputPanel;
	private final JScrollPane scroll;
	
	
	/**
	 * 
	 */
	public GameUI() {
		super("Naughts & Crosses Online");
		
		getOptions();		
		setLayout(new BoxLayout(getContentPane(),BoxLayout.PAGE_AXIS));
		screen = new Screen();
		screen.setPreferredSize(new Dimension(500,500));

		log = new JTextArea("This is a message log");
		log.setPreferredSize(new Dimension(100,250));
		scroll = new JScrollPane (log,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		log.setEditable(false);
		
		autismPanel = new JPanel(); autismPanel.setLayout(new FlowLayout()); //because there is no other way to force Layout to cooperate
		player = new JTextField("You are player "+name);
		player.setEditable(false);
		autismPanel.add(player);
		
		error_msg = new JLabel("Invalid input: Insert [letter][number]");
		error_msg.setVisible(false);
		
		inputPanel = new JPanel();
		inputPanel.setLayout(new FlowLayout());
		move = new JTextField("Your next move",10); inputPanel.add(move);
		submitB = new JButton("Submit"); inputPanel.add(submitB); submitB.setMnemonic(KeyEvent.VK_SPACE);
		submitB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(!dataReceived) {
					JOptionPane.showMessageDialog(getContentPane(), "Please wait for the other players to finish picking their characters","Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				String input = move.getText().toUpperCase().strip();
				if (!input.matches("[A-E][1-5]")) {
					error_msg.setVisible(true);
					return;
				}
				answer = convertInput(input);
				pushMessage(String.format("Player %s played %s", name, move.getText()));
				move.setText("");
				error_msg.setVisible(false);
			}
		});
		
		disconnectB = new JButton("Resign"); disconnectB.setMnemonic(KeyEvent.VK_R);
		disconnectB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(!dataReceived) {
					JOptionPane.showMessageDialog(getContentPane(), "Please wait for the other players to finish picking their characters","Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				answer = -2;
				pushMessage("Player "+ name +" resigned the game!");
			}
			
		});
		
		add(Box.createRigidArea(new Dimension(50,35)));
		add(autismPanel);
		add(screen);
		add(Box.createRigidArea(new Dimension(20,15)));
		add(inputPanel);
		add(error_msg);
		add(Box.createRigidArea(new Dimension(50,15)));
		add(scroll);
		add(Box.createRigidArea(new Dimension(50,35)));
		add(disconnectB);
		pushMessage("Waiting for all other players to choose a character");
	}
	
	
	public void pushMessage(String mes) {
		log.setText(String.format("%s\n%s", log.getText(), mes));
	}

	
	public void setScreen(GameBoard gboard) {
		screen.board = gboard;
		screen.repaint();
		//Wait until it loads then update the whole thing.
		//JVM has forced my hand
		try {
			Thread.sleep(10);  
		}catch (InterruptedException e){
			e.printStackTrace();
		}
		this.update(this.getGraphics());
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

	
	public Color getColor() {
		return this.color;
	}
	
	/**
	 * Converts the player's input from <code>Char-Int</code> to <code>Int-Int</code>
	 * so that the GameBoard can understand it
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
			
			default: index = 40;
		}
		index += Integer.parseInt(Character.toString(str.charAt(1)))-1;
		return index;
	}
	
	private void getOptions() { //gets character player and color
		
		Object [] chars = {GameEngine.X,GameEngine.O,'\u2654' ,'\u2655' ,'\u2656' ,'\u2657' ,'\u2658','\u0021' ,'\u0022' ,'\u0023' ,'\u0024' ,'\u0025' ,'\u002A','\u002B','\u0041' ,'\u0042' ,'\u0043' ,'\u0044' ,'\u0045' ,'\u0046' ,'\u0047' ,'\u0048' ,'\u0049' ,'\u0050' ,'\u0051' 
				,'\u0052' ,'\u0053' ,'\u0054' ,'\u0055' ,'\u0056' ,'\u0057' ,'\u003C','\u003F','\u007E'};
		
		boolean correct;
		do {
			try {
				this.name = (Character)JOptionPane.showInputDialog(null,"Select one of the following characters","Test",JOptionPane.PLAIN_MESSAGE,null,chars,chars[0]);
				correct = true;
			}catch (NullPointerException e) {
				JOptionPane.showMessageDialog(this, "Please select a character", "Error", JOptionPane.ERROR_MESSAGE);
				correct = false;
			}
		}while(!correct);
		
		Color col;
		do {
			col = JColorChooser.showDialog(this, "Choose a color", Color.BLACK);
			if (col==null) JOptionPane.showMessageDialog(this, "Please select a color", "Error", JOptionPane.ERROR_MESSAGE);
		}while(col==null);
		this.color = col;
		
	}
	
	public void setCustomOptions(char[] chars, Color[] colors) {
		if(colors.length!=chars.length) throw new RuntimeException("Color and character arrays must be of same length");
		
		for(int i=0;i<colors.length;i++) {
			screen.colorMap.put(chars[i], colors[i]);
		}
		dataReceived = true;
	}
	

	private class Screen extends JPanel{
		private GameBoard board;
		private final HashMap<Character,Color> colorMap = new HashMap<Character,Color>();
		private String[] letters = {"A","B","C","D","E"};
		
		public Screen() {
			board = new GameBoard();
		}
		
		
		@Override
		public void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.setFont(new Font("Serif", Font.PLAIN, 60));
			//paint board
			for(int i=1; i<6; i++) {
				g.drawString(Integer.toString(i), i*53, 50);//53 instead of 50 to compensate for character width
				for(int j=1;j<6;j++) {
					g.drawString(letters[j-1], 0, (j+1)*50);
					g.drawRect(i*50, j*50, 50, 50);
				}
			}
			//paint marks
			g.setFont(new Font("Monospaced", Font.BOLD, 80));
			for(int i=0;i<5;i++) {
				for(int j=0;j<5;j++) {
					char c = board.getBoard()[i][j];
					if (c!=GameEngine.DASH) { 
						g.setColor(colorMap.get(c));
						g.drawString(Character.toString(c), (j+1)*50, (i+2)*50);
					
					}
				}
			}
		}		
	}
	
}
