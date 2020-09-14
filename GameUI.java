import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

@SuppressWarnings("serial")
public class GameUI extends JFrame{
	
	private char name;
	private int answer = -1;
	private final JLabel error_msg;
	private final JTextArea log, screen;
	private final JTextField player,move;
	private final JButton submitB,disconnectB;
	private final JPanel autismPanel,inputPanel;
	//private final JPanel gamePanel;
	
	public GameUI(char xo) {
		super("Naughts & Crosses Online");
		name = xo;
		setLayout(new BoxLayout(getContentPane(),BoxLayout.PAGE_AXIS));
		screen = new JTextArea("This is the screen that will display the game");
		log = new JTextArea("This is a message log");
		
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
				if (!input.matches("[A-Ε][1-5]")) {
					error_msg.setVisible(true);
					return;
				}
				answer = convertInput(input);
				pushMessage("Player "+ name +" played "+ move.getText());
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
		add(Box.createRigidArea(new Dimension(50,15)));
		add(inputPanel);
		add(error_msg);
		add(Box.createRigidArea(new Dimension(50,15)));
		add(log);
		add(Box.createRigidArea(new Dimension(50,35)));
		add(disconnectB);
		
	}
	
	public void pushMessage(String mes) {
		log.setText(log.getText()+"\n"+mes);
	}
	
	public void setScreen(GameBaord game_obj) {
		screen.setText(game_obj.toString());
	}
	
	public int getAnswer() {
		/*
		 * Returns: -1 if no answer
		 * 			-2 if resigned
		 * 			int between 0,55 if correct answer
		 */
		int ans;
		if(answer!=-1) { 
			ans = this.answer;
			this.answer = -1;
		}else ans = -1;
		return ans;
	}
	
	private int convertInput(String str) {
		char row = str.charAt(0);
		int index;
		switch (row){
		case'\u0041':{
			index = 0; 
			break;
		}
		case'\u0042': {
			index = 10;
			break;
		}
		case'\u0043': {
			index = 20;
			break;
		}
		case'\u0044':{
			index = 30;
			break;
		}
		default: index = 40;
		}
		index += Integer.parseInt(Character.toString(str.charAt(1)));
		System.out.println(index);
		return index;
	}
	public void setPlayer(char c) {
		name = c;
		player.setText("You are player "+ Character.toString(c));
	}
}
