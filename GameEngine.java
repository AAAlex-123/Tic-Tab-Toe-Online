import javax.swing.JFrame;

public class GameEngine {
	private final static char X = '\u0058';
	private final static char O = '\u004F';
	public static void main(String[] args) {
		GameUI ui = new GameUI(X);
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(400,550);
		ui.setVisible(true);
	  	ui.setResizable(false);
	}
	
	public static void turnReceived(String game_obj) {
		//do stuff
		System.out.println(game_obj);
	}
}
