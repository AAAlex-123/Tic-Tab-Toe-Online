import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

@SuppressWarnings("serial")
public class GameUI extends JFrame{
	
	private String curr_game;
	private final char name;
	private String answer = null;
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
		
		
		inputPanel = new JPanel();
		inputPanel.setLayout(new FlowLayout());
		move = new JTextField("Your next move",10); inputPanel.add(move);
		submitB = new JButton("Submit"); inputPanel.add(submitB);
		submitB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				answer = move.getText();
				pushMessage("Player "+ name +" played "+ move.getText());
				move.setText("");
			}
		});
		
		disconnectB = new JButton("Resign");
		disconnectB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				answer = "Resign";
				pushMessage("Player "+ name +" resigned the game!");
			}
			
		});
		add(Box.createRigidArea(new Dimension(50,35)));
		add(autismPanel);
		add(screen);
		add(Box.createRigidArea(new Dimension(50,15)));
		add(inputPanel);
		add(Box.createRigidArea(new Dimension(50,15)));
		add(log);
		add(Box.createRigidArea(new Dimension(50,35)));
		add(disconnectB);
		
	}
	
	public void pushMessage(String mes) {
		log.setText(log.getText()+"\n"+mes);
	}
	
	public void setScreen(String game_obj) {
		curr_game = game_obj;
		screen.setText(game_obj);
	}
	
	public String getAnswer() {
		//If the player has given an answer, return it and set it to null
		String ans;
		if(answer!=null) { 
			ans = this.answer;
			this.answer = null;
		}else ans = null;
		return ans;
	}
	
	
}
