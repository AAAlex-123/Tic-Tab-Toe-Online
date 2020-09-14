package ttt_online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
	private final int playerCount = 2;
	
	private ServerSocket server;
	private final Socket[] sockets = new Socket[playerCount];
	private final ObjectInputStream[] inputs = new ObjectInputStream[playerCount];
	private final ObjectOutputStream[] outputs = new ObjectOutputStream[playerCount];
	private final char[] symbols = new char[playerCount];
	
	private final GameBoard gameBoard = new GameBoard();
	private int currentPlayer = 0;
//	gameBoard.hasWon();
//	gameBoard.markSquare();
	
	private void run() {
		System.out.printf("Server ready, waiting for %d players\n", playerCount);
		getConnections();

		System.out.printf("Starting game\n");
		while (true) {
			makeTurn();
			System.out.println(gameBoard.toString());
		}
	}

	private void makeTurn() {
		System.out.printf("\nPlayer #%d starts their turn\n", currentPlayer);
		//sending ok to start
		try {outputs[currentPlayer].writeObject("Ready to play!");}
		catch (IOException e) {
			System.err.printf("err sending conf to #%d", currentPlayer);
			e.printStackTrace();
		};

		//sending board
		try {outputs[currentPlayer].writeObject((String) gameBoard.toString());}
		catch (IOException e) {
			System.err.printf("err sending board to #%d", currentPlayer);
			e.printStackTrace();
		}
		System.out.println("sent confirmation and board");
		
		// get move
		int move = -1;
		try {move = (int) inputs[currentPlayer].readObject();}
		catch (ClassNotFoundException | IOException e) {
			System.err.printf("err receiving move from #%d", currentPlayer);
			e.printStackTrace();
		}
		System.out.printf("received move: %d\n", move);
		
		if (move == -2) {
			System.err.println("lmao we don't have time for this");
		} else {
			if (gameBoard.markSquare(move, symbols[currentPlayer]))
				System.out.println("succ move");
			else
				System.out.println("fail move");
		}
		
		//sending response to start
		try {outputs[currentPlayer].writeObject(String.format("Move received: %d", move));}
		catch (IOException e) {
			System.err.printf("err sending conf to #%d", currentPlayer);
			e.printStackTrace();
		}
		
		//sending board
		try {outputs[currentPlayer].writeObject((String) gameBoard.toString());}
		catch (IOException e) {
			System.err.printf("err sending board to #%d", currentPlayer);
			e.printStackTrace();
		}
		System.out.println("sent responce and board again");
		
		currentPlayer = (currentPlayer + 1) % playerCount;
	}
	
	private void getConnections() {
		// initialise server and connections
		try {
			server = new ServerSocket(12345, 3);
			
			for (int i=0; i<playerCount; i++) {
				// get connections
				sockets[i] = server.accept();
				inputs[i] = new ObjectInputStream(sockets[i].getInputStream());
				outputs[i] = new ObjectOutputStream(sockets[i].getOutputStream());
				
				// exchange messages
				outputs[i].writeObject(String.format("hi player #%d pls wait for everyone\n", i));
				System.out.printf("player #%d connected\n", i);
				try {System.out.println(inputs[i].readObject());}
				catch (ClassNotFoundException e) {e.printStackTrace();}
				try {symbols[i] = (char) inputs[i].readObject();}
				catch (ClassNotFoundException e) {e.printStackTrace();}
			}
			
			// send ready message
			for (int i=0; i<playerCount; i++) {
				outputs[i].writeObject("Everyone has joined, get readyyy");
			}

		} catch (IOException e) {
			System.err.println("Connection error server reeeeeeeee");
			e.printStackTrace();
		}
		System.out.printf("yay got connections ready to do stuff\n");
	}
	

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

}
