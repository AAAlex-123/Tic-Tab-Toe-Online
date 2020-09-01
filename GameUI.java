import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

@SuppressWarnings("serial")
public class GameUI extends JFrame{
	
	private String curr_game;
	private final char name;
	private final JTextArea log, screen;
	private final JTextField player,move;
	private final JButton submit;
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
		submit = new JButton("Submit"); inputPanel.add(submit);
		submit.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				GameEngine.turnReceived(curr_game);
				pushMessage("Player "+ name +" played "+ move.getText());
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
		
	}
	
	public void pushMessage(String mes) {
		log.setText(log.getText()+"\n"+mes);
	}
	
	public void setScreen(String game_obj) {
		curr_game = game_obj;
		screen.setText(game_obj);
	}
	
	
}
