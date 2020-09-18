package ttt_online;

import java.awt.Color;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JFrame;

/**
 * Client-side application to handle communications with the server
 */
public class GameEngine { // aka client
	public final static char X = 'X';
	public final static char O = 'O';
	public final static char DASH = '-';
	
	private String address;
	private boolean printStackTrace;
	private boolean pushErrorMessages = true;
	
	private GameUI ui;

	private Socket socket;
	private ObjectOutputStream output;
	private ObjectInputStream input;

	private GameBoard localGameBoard;
	private char[][] localGameBoardConstructor;
	
	/**
	 * Constructor with parameters <code>address, symbol</code>
	 * <code>printStackTrace</code> will be <code>false</code>
	 * Also sets up the UI
	 * @param address String, the IP address of the server
	 * @param symbol char, the symbol the player will use when they play
	 */
	public GameEngine(String address, char symbol) {
		this.address = address;
		this.printStackTrace = false;
		setUI(symbol);
	}

	/**
	 * Constructor with parameters <code>address, symbol</code> and <code>printStackTrace</code>
	 * Also sets up the UI
	 * @param address String, the IP address of the server
	 * @param symbol char, the symbol the player will use when they play
	 * @param printStackTrace boolean, whether or not to print stack trace when Exceptions occur
	 */
	public GameEngine(String address, char symbol, boolean printStackTrace) {
		this.address = address;
		this.printStackTrace = printStackTrace;
		setUI(symbol);
	}

	/**
	 * Runs the GameEngine
	 */
	private void run() {
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
	 * @param symbol char, the player's symbol
	 */
	private void setUI(char symbol) {
		ui = new GameUI();
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(320, 720);
		ui.setVisible(true);
	  	ui.setResizable(true);
	}

	/**
	 * Initialises a connection to the server connections.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br><br>
	 * If at any point something goes wrong, exit :)
	 */
	private void getConnection() {
		try {
			// get connection
			socket = new Socket(InetAddress.getByName(address), 12345);
			output = new ObjectOutputStream(socket.getOutputStream());
			input =  new ObjectInputStream(socket.getInputStream());
			
			// exchange messages
			ui.pushMessage(String.format("server said: %s", input.readObject()));
			output.writeObject(ui.getSymbol());
			output.writeObject(ui.getColor());
			
			// wait for ready message and for updated symbols/colors
			ui.pushMessage((String) input.readObject());

			char[] symbols = (char[]) input.readObject();
			Color[] colors = (Color[]) input.readObject();
			ui.setCustomOptions(symbols, colors);
			
			log("Connected to server successfully as player '%c' with color %s", ui.getSymbol(), ui.getColor());
			
		} catch (IOException e) {
			logerr("IOException while getting connections or sending messages\nExiting...\n");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage("Couldn't connect to server; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException while reading messages\nExiting...\n");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage("Couldn't connect to server; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
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
				ui.setEnableTurn(false);
				ui.pushMessage("");
				log("\nStarting turn");
			}

			// get ready message
			String response = (String) input.readObject();
			
			// if message is resignation (can only happen when starting==false) do something
			if (response.matches("Player '\\X' resigned")) {
				ui.pushMessage(String.format("\n%s\n\nGame ended; please exit",
						response.charAt(8) == ui.getSymbol() ? "You resigned :(" : response+" :)"));
				updateBoard();
				ui.setEnableTurn(false);
				pushErrorMessages = false;
				while(true) {;}
			} else if (response.matches("Player '\\X' won!")) {
				ui.pushMessage(String.format("\n%s\n\nGame ended; please exit",
						response.charAt(8) == ui.getSymbol() ? "You won :)" : response+" :("));
				updateBoard();
				ui.setEnableTurn(false);
				pushErrorMessages = false;
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
			if (pushErrorMessages) ui.pushMessage("Another player unexpectedly disconnected; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (IOException e) {
			logerr("IOException while getting server message");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage("Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException while getting server message");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage("Something went wrong; please exit and inform the developers");
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
			if (pushErrorMessages) ui.pushMessage("Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		}
	  	log("Got and sent move: [%d, %d]", move/10, move%10);
	}
	
	/**
	 * Updates the board on the UI
	 * 
	 * @throws ClassNotFoundException idk when it's thrown
	 * @throws IOException thrown when server disconnects (?)
	 * @throws EOFException thrown when server disconnects (?)
	 */
	private void updateBoard() throws ClassNotFoundException, IOException, EOFException {
		localGameBoardConstructor = (char[][]) input.readObject();
		localGameBoard = new GameBoard(localGameBoardConstructor);
		ui.setScreen(localGameBoard);
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
	 * @param args args[1] is used to determine <code>printStackTrace</code> field, '1' for true, other for false
	 */
	public static void main(String[] args) {
		log("Getting symbol and color options");
		GameEngine gameEngine;

		char symbol;
		try {symbol = args[0].charAt(0);}
		catch (ArrayIndexOutOfBoundsException e) {
			logerr("Error: No <symbol> argument provided;\nPlease exit;");
			while (true) {;}
		}

		String address;
		try {address = args[1];}
		catch (ArrayIndexOutOfBoundsException e) {
			logerr("Warning: No <address> argument provided;\nInitialised to '127.0.0.1';");
			address = "127.0.0.1";
		}

		boolean printStackTrace;
		try {
			printStackTrace = args[2].equals("1") ? true : false;
			gameEngine = new GameEngine(address, symbol, printStackTrace);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			logerr("Warning: No <printStackTrace> argument provided;\nInitialised to false;");
			gameEngine = new GameEngine(address, symbol);
		}
		gameEngine.run();
	}
}
