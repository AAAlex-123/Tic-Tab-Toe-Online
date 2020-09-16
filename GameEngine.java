package ttt_online;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JFrame;

public class GameEngine { // aka client
	private boolean printStackTrace;
	
	private GameUI ui;

	private Socket socket;
	private ObjectOutputStream output;
	private ObjectInputStream input;

	private GameBoard localGameBoard;
	private char[][] localGameBoardConstructor;

	public void run(char symbol, boolean printStackTrace) {
		this.printStackTrace = printStackTrace;
		setUI(symbol);
		getConnection();
		while (true) {
			setup(true);
			play();
			setup(false);
		}
	}

	/**
	 * Sets up the UI
	 * 
	 * @param symbol
	 */
	private void setUI(char symbol) {
		ui = new GameUI(symbol);
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(400, 550);
		ui.setVisible(true);
	  	ui.setResizable(false);
	}

	/**
	 * Initialises a connection to the server connections.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br><br>
	 * If at any point something goes wrong, exit :)
	 */
	private void getConnection() {
		boolean exit = false;
		try {
			// get connection
			socket = new Socket(InetAddress.getByName("127.0.0.1"), 12345);
			output = new ObjectOutputStream(socket.getOutputStream());
			input =  new ObjectInputStream(socket.getInputStream());
			
			// exchange messages
			ui.pushMessage(String.format("server said: %s", input.readObject()));
			output.writeObject(ui.getSymbol());
			
			// wait for ready message
			ui.pushMessage((String) input.readObject());
			
			log("Connected to server successfully as player '%c'", ui.getSymbol());
			
		} catch (IOException e) {
			logerr("IOException while getting connections or sending messages\nExiting...\n");
			if (printStackTrace) e.printStackTrace();
			exit = true;
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException while reading messages\nExiting...\n");
			if (printStackTrace) e.printStackTrace();
			exit = true;
		}
		
		if (exit) {
			System.exit(1);
		}
	}

	/**
	 * Sets up the UI and exchanges messages before and after the player makes their move.
	 * 
	 * @param starting boolean, whether or not it is the start or the end of the player's turn
	 */
	private void setup(boolean starting) {
		try {
			if (starting) {
				ui.resetMove();
				ui.pushMessage("");
				log("\nStarting turn");
			}

			// get ready message
			String response = (String) input.readObject();
			
			// if message is resignation (only when starting==false) do something
			if (response.matches("Player '.' resigned")) {
				ui.pushMessage(String.format("\n%s\n\nGame ended; please exit",
						response.charAt(8) == ui.getSymbol() ? "You resigned :(" : response+" :)"));
				ui.setEnableTurn(false);
				while(true) {;}
			} else if (response.matches("Player '.' won!")) {
				ui.pushMessage(String.format("\n%s\n\nGame ended; please exit",
						response.charAt(8) == ui.getSymbol() ? "You won :)" : response+" :("));
				ui.setEnableTurn(false);
				updateBoard();
				while(true) {;}
			}
			log("Got response: '%s'", response);
			ui.pushMessage(response);
			
			// update board
			updateBoard();
	
			// enable/disable buttons/text
			ui.setEnableTurn(starting);

			log("End %s setup", starting ? "starting" : "ending");
		} catch (EOFException e) {
			logerr("EOFException while getting server message");
			if (printStackTrace) e.printStackTrace();
			ui.pushMessage("Another player unexpectedly disconnected; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (IOException e) {
			logerr("IOException while getting server message");
			if (printStackTrace) e.printStackTrace();
			ui.pushMessage("Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException while getting server message");
			if (printStackTrace) e.printStackTrace();
			ui.pushMessage("Something went wrong; please exit and inform the developers");
			while (true) {;}
		}
	}

	/**
	 * Gets the player's move, and sends it to the server.
	 */
	private void play() {
		// get the move in the worst possible way
	  	int move = -1;
	  	while (move == -1) {
	  		move = ui.getAnswer();
	  		if (move != -1 && move != -2 && !localGameBoard.isValid(move)) {
	  			ui.pushMessage(String.format("You can't play %c%s!", 65+move/10, move%10+1));
	  			log("Tried to play %c%s", 65+move/10, move%10+1);
	  			move = -1;
	  		}
  			try {Thread.sleep(250);}
  			catch (InterruptedException e) {
  				if (printStackTrace) e.printStackTrace();
  			}
	  	}
		ui.pushMessage(String.format("You played %c%s", 65+move/10, move%10+1));
	  	
	  	// send the move
	  	try {output.writeObject(move);}
	  	catch (IOException e) {
			logerr("IOException while getting server message");
			if (printStackTrace) e.printStackTrace();
			ui.pushMessage("Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		}
	  	log("Got and sent move: [%d, %d]", move/10, move%10);
	}
	
	private void updateBoard() throws ClassNotFoundException, IOException, EOFException {
		localGameBoardConstructor = (char[][]) input.readObject();
		localGameBoard = new GameBoard(localGameBoardConstructor);
		ui.setScreen(localGameBoard.toString());
	}
	
	/**
	 * Same as <code>System.out.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	private static void log(String text, Object... args) {
		System.out.printf(text+"\n", args);
	}
	
	/**
	 * Same as <code>System.err.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	private static void logerr(String text, Object... args) {
		System.err.printf(text+"\n", args);
	}
	
	/**
	 * Main method. Run to create and run a client
	 * 
	 * @param args args[0] is used for the player's symbol
	 */
	public static void main(String[] args) {
		char symbol;
		try {symbol = args[0].charAt(0);}
		catch (ArrayIndexOutOfBoundsException e) {
			logerr("Error: No <symbol> argument provided;\nPlease exit;");
			while (true) {;}
		}
		boolean printStackTrace;
		try {printStackTrace = args[1].equals("1") ? true : false;}
		catch (ArrayIndexOutOfBoundsException e) {
			logerr("Error: No <printStackTrace> argument provided;\nInitialised to false;");
			printStackTrace = false;
		}
		GameEngine gameEngine = new GameEngine();
		gameEngine.run(symbol, printStackTrace);
	}
}
