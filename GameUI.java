package ttt_online;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.*;

@SuppressWarnings("serial")
public class GameUI extends JFrame{
	/*
	 * Use the public method setCustomOptions by giving a char and color array such that ```char of player i -> color of player i```
	 * In any other case the game will use a blue X and a red O for the players
	 * The game graphics glitch out occasionally. Slightly resize the window if it happens. It's annoying but idk what else to do.
	 * Also the game has to take a GameBoard object *not* a String to draw the graphics.
	 */
	
	private char name;
	private char[] charArr = null;
	private Color[] colorArr = null;
	private int answer = -1;
	private final JLabel error_msg;
	private final Screen screen;
	private final JTextArea log;
	private final JTextField player,move;
	private final JButton submitB,disconnectB;
	private final JPanel autismPanel,inputPanel;
	private final JScrollPane scroll;
	
	public GameUI(char xo) {
		super("Naughts & Crosses Online");
		name = xo;
		setLayout(new BoxLayout(getContentPane(),BoxLayout.PAGE_AXIS));
		if(charArr==null||colorArr==null) screen = new Screen();
		else screen = new Screen(charArr,colorArr);
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
		submitB = new JButton("Submit"); inputPanel.add(submitB);
		submitB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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
		
		disconnectB = new JButton("Resign");
		disconnectB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
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
		
		
	}
	
	public void pushMessage(String mes) {
		log.setText(String.format("%s\n%s", log.getText(), mes));
	}
	
	public void setScreen(GameBoard gboard) {
		screen.repaint(gboard);
	}
	
	public int getAnswer() {
		/*
		 * Returns: -1 if no answer
		 * 			-2 if resigned
		 * 			int between 0,55 if correct answer
		 */
		int ans;
		if (this.answer != -1) { 
			ans = this.answer;
			this.answer = -1;
		} else ans = -1;
		return ans;
	}
	
	public void setEnableTurn(boolean enable) {
		submitB.setEnabled(enable);
		disconnectB.setEnabled(enable);
	}
	
	public char getSymbol() {
		return this.name;
	}
	
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
	
	public void setCustomOptions(char[] charArr, Color[] colorArr) {
		this.charArr=charArr;
		this.colorArr=colorArr;
	}
	
	private class Screen extends JPanel{
		private GameBoard board;
		private final HashMap<Character,Color> colorMap = new HashMap<Character,Color>();
		private String[] letters = {"A","B","C","D","E"};
		
		public Screen(char[] chars, Color[] colors) {
			if(colors.length!=chars.length) throw new RuntimeException("Color and character arrays must be of same length");
			
			for(int i=0;i<colors.length;i++) {
				colorMap.put(chars[i], colors[i]);
			}
			board = new GameBoard();
		}
		
		public Screen() {
			colorMap.put(GameEngine.X, Color.BLUE);
			colorMap.put(GameEngine.O,Color.RED);
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
		
		
		public void repaint(GameBoard board) {
			this.board = board;
			this.repaint();
		}
		
	}
	
}
