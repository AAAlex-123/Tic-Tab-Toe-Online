import javax.swing.JFrame;

public class GameEngine {
	public final static char X = '\u0058';
	public final static char O = '\u004F';
	public final static char DASH ='\u002D';
	public static void main(String[] args) {
		GameUI ui = new GameUI(X);
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(400,550);
		ui.setVisible(true);
	  	ui.setResizable(false);
	  	//start game
	  	
	  	
	  	//this will have to be done for all UIs
	  	int move = -1;
	  	while (move==-1) {
	  		move = ui.getAnswer();
	  		if (move == -1) {
	  			try {
	  				Thread.sleep(250); //wait 1/4 sec before checking 
	  			}catch (InterruptedException e){
	  				e.printStackTrace();
	  			}
	  		}
	  	}
	  	System.out.print(move);
	  	if(move==-2) {
	  		System.out.println("Game over");
	  		System.exit(0);
	  	}
	}
	
	
}
