package ttt_online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JFrame;

public class GameEngine { // aka client
	public final static char DASH ='\u002D';
	static int symbolIndex = 0;
	
	private GameUI ui;

	private Socket socket;
	private ObjectOutputStream output;
	private ObjectInputStream input;


	public void run(char symbol) {
		setUI(symbol);
		getConnection();
		while (true) {
			setup(true);
			play();
			setup(false);
		}
	}
	
	private void setup(boolean starting) {
		// wait for message from server and send it
		String responce = "";
		try {
			responce = (String) input.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.err.printf("err %s turn\n", starting ? "starting" : "ending");
			e.printStackTrace();
		}
		System.out.println("got responce: " + responce);

		if (responce.equals("Move received: -2")) {
			ui.pushMessage("lmao ggez u looz git gud git hub");
			/*try {
				output.writeObject("lmaobye");
				socket.close();}
			catch (IOException e) {
				System.err.println("err closing");
				e.printStackTrace();
			}*/
		} else {
			ui.pushMessage(responce);
		}
		
		// update board
		try {ui.setScreen((String) input.readObject());}
		catch (ClassNotFoundException | IOException e) {
			System.err.println("err receiving board");
			e.printStackTrace();
		}
		System.out.println("got board");
		
		// enable buttons / text
		ui.setEnableTurn(starting);
		System.out.println("done setup");
	}

	private void play() {
		// get the move in the worst possible way
		// move == -2 will be handled by the server
	  	int move = -1;
	  	while (move == -1) {
	  		move = ui.getAnswer();
	  		if (move == -1) {
	  			try {Thread.sleep(250);}
	  			catch (InterruptedException e) {e.printStackTrace();}
	  		}
	  	}
		System.out.println("got move: " + move);
	  	
	  	// send the move
	  	try {output.writeObject(move);}
	  	catch (IOException e) {
	  		System.err.println("err sending move");
			e.printStackTrace();
		}
		System.out.println("done play");
	}

	private void getConnection() {
		try {
			// get connection
			socket = new Socket(InetAddress.getByName("127.0.0.1"), 12345);
			output = new ObjectOutputStream(socket.getOutputStream());
			input =  new ObjectInputStream(socket.getInputStream());
			
			// exchange messages
			try {System.out.printf("server said: %s", input.readObject());}
			catch (ClassNotFoundException e) {e.printStackTrace();}
			output.writeObject(new String("hi server :)"));
			output.writeObject(ui.getSymbol());
			
			// wait for ready message
			try {ui.pushMessage((String) input.readObject());}
			catch (ClassNotFoundException e) {e.printStackTrace();}
			
		} catch (IOException e) {
			System.err.println("Connection error client reeeeeeeee");
			e.printStackTrace();
		}
	}

	private void setUI(char symbol) {
		ui = new GameUI(symbol);
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(400, 550);
		ui.setVisible(true);
	  	ui.setResizable(false);
	  	System.out.println("yoo im: " + ui.getSymbol());
	  	System.out.println(symbolIndex + " " + GameEngine.symbolIndex);
	}
	
	public static void main(String[] args) {
		GameEngine gameEngine = new GameEngine();
		gameEngine.run(args[0].charAt(0));
	}
}
