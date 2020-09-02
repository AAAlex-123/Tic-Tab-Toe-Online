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
	  	//start game
	  	
	  	
	  	//this will have to be done for all UIs
	  	String move = null;
	  	while (move==null) {
	  		move = ui.getAnswer();
	  		if (move == null) {
	  			try {
	  				Thread.sleep(250); //wait 1/4 sec before checking 
	  			}catch (InterruptedException e){
	  				e.printStackTrace();
	  			}
	  		}
	  	}
	  	System.out.print(move);
	  	if(move.equals("Resign")) {
	  		System.out.println("Game over");
	  		System.exit(0);
	  	}
	}
	
	
}
