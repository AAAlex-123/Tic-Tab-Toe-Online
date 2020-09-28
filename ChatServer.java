package ttt_online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-side application to handle chat between players.
 */
public class ChatServer extends Server {
	
	// port of the Chat Server
	private static final int CHAT_PORT = 10002;

	/**
	 * Keeps track of the running threads to know when another connection can be
	 * accepted.<br>
	 * When one is created, give it a slot. When one is stopped, free up its slot.
	 * <p>
	 * No it's not spaghetti, yes we need it to keep track of which
	 * <code>outputs</code> indexes are empty and can have an Output Stream
	 */
	private final boolean[] available;

	/**
	 * Constructs the Chat Server object.
	 * 
	 * @see Server#Server() Server()
	 */
	public ChatServer() {
		super();
		this.available = new boolean[playerCount];
		for (int i = 0; i < playerCount; i++)
			available[i] = true;
	}

	/**
	 * Main method that calls other methods to actually run the server.<br>
	 * After these methods are done, only the threads listening for input are
	 * running.
	 * 
	 * @see ChatServer#initialiseServer() initialiseServer()
	 * @see ChatServer#getConnections() getConnections()
	 */
	protected void run() {
		initialiseServer();
		getConnections();
	}

	/**
	 * Initialises the Chat Server on port <code>CHAT_PORT</code> with
	 * <code>playerCount</code> total possible connections.
	 */
	protected void initialiseServer() {
		try {
			server = new ServerSocket(CHAT_PORT);
			log("\n\nChat Server ready, listening for up to %d players", playerCount);
		} catch (IOException e) {
			logerr("IOException in initialiseServer()");
			if (printStackTrace)
				e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Every 2 seconds check if there is an <code>available</code> slot to get a
	 * connection. If there is, wait to accept a connection and assign it this slot.
	 * Get its input and output streams. Finally create a new ChatServerThread with
	 * the slot's index and start it.
	 * 
	 * @see ChatServer#available available
	 * @see ChatServer#getAvailable() getAvailable()
	 * @see ChatServerThread
	 */
	protected void getConnections() {
		try {
			int index = -1;
			Socket chatConnection = null;
			while (true) {
				// wait until there is an available slot to accept a connection
				index = getAvailable();
				if (index != -1) {
					chatConnection = server.accept();
				} else {
					try {Thread.sleep(2000);}
					catch (InterruptedException e) {;}
					continue;
				}

				// get Input Stream.
				// get output Stream after broadcasting so that this player doesn't get the message that he joined
				inputs[index] = new ObjectInputStream(chatConnection.getInputStream());
				
				// get symbol, send ack back
				symbols[index] = (char) inputs[index].readObject();

				for (int i=0; i<playerCount; i++) {
					if ((i != index) && (symbols[index] == symbols[i])) {
						char chessPiece = chessPieces
								.remove(ThreadLocalRandom.current().nextInt(0, chessPieces.size()));
						log("Duplicate found '%c', replaced with '\\u%04x'", symbols[index], (int) chessPiece);
						symbols[index] = chessPiece;
					}
				}

				
				// inform everyone that someone has joined
				broadcast("Chat Server: '%c' just joined. Say hi!", symbols[index]);

				// finally get Output Stream
				outputs[index] = new ObjectOutputStream(chatConnection.getOutputStream());
				outputs[index].writeObject(
						String.format("Hi player '%c', you're now connected.\nStart chatting!", symbols[index]));

				log("\nChat Connection #%d established with '%c'", index, symbols[index]);

				new ChatServerThread(index).start();
				index++;
			}
		} catch (IOException e) {
			logerr("IOException in getConnections()\nidkwhatishappeningplshelp...");
			if (printStackTrace)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException in getConnections()\nSomething went very wrong...");
			if (printStackTrace)
				e.printStackTrace();
		}
	}

	/**
	 * Returns the first slot of the <code>available</code> that is available to get
	 * a connection and sets it to <code>false</code>.
	 * 
	 * @return int, the available slot, -1 when no slot is available.
	 * @see ChatServer#available available
	 */
	private int getAvailable() {
		for (int i = 0; i < playerCount; i++)
			if (available[i]) {
				available[i] = false;
				return i;
			}
		return -1;
	}

	/**
	 * Private inner class that listens to a specific client's Output Stream and
	 * upon receiving a message, broadcasts it to every client connected to the Chat
	 * Server.
	 * <p>
	 * When an Exception occurs, this Thread terminates execution, the client's
	 * Streams are closed and the <code>available</code> slot specified by
	 * <code>index</code> is freed up.
	 *
	 * @see ChatServer#broadcast(String, Object[]) broadcast()
	 * @see ChatServer#closeStreams(int) closeOutputStream()
	 * @see ChatServer#available available
	 */
	private class ChatServerThread extends Thread {

		private final int index;

		/**
		 * Constructs the Thread to listen to <code>index</code> input Stream
		 * 
		 * @param count int, used to keep track the slot this thread occupies in the
		 *              <code>available</code> table
		 */
		public ChatServerThread(int count) {
			this.index = count;
		}

		/**
		 * Runs the thread; upon receiving a message, broadcasts it to every client
		 * connected to the Chat Server.
		 * 
		 * @see ChatServer#broadcast(String, Object[]) broadcast()
		 */
		public void run() {
			log("Thread #%d started", index);
			while (true) {
				try {
					broadcast((String) inputs[index].readObject());
				} catch (SocketException e) {
					logerr("SocketException in ChatServerThread.run(); connection #%d closed by user\n", index);
					if (printStackTrace)
						e.printStackTrace();
					closeStreams(index);
					break;
				} catch (IOException e) {
					logerr("IOException in ChatServerThread.run()\nidkwhatishappeningplshelp...");
					if (printStackTrace)
						e.printStackTrace();
					closeStreams(index);
					break;
				} catch (ClassNotFoundException e) {
					logerr("ClassNotFoundException in ChatServerThread.run()\nidkwhatishappeningplshelp...");
					if (printStackTrace)
						e.printStackTrace();
					closeStreams(index);
					break;
				}
			}
		}
	}

	/**
	 * Closes the Streams bound to client <code>index</code> and frees up the
	 * <code>available</code> slot this Thread occupied (specified by
	 * <code>index</code>).
	 * 
	 * @param index int, the index of the client
	 * @see ChatServer#available available
	 */
	private void closeStreams(int index) {
		log("Closing thread #%d", index);
		broadcast("Chat Server: '%c' left the chat.", symbols[index]);
		try {
			outputs[index].close();
			inputs[index].close();
		} catch (IOException e) {
			logerr("IOException in closeStreams()");
			if (printStackTrace)
				e.printStackTrace();
		} finally {
			outputs[index] = null;
			inputs[index] = null;
			symbols[index] = '\u0000';
			available[index] = true;
		}
	}

	/**
	 * Main method. Run to create and run a Chat Server.
	 *
	 * @param args not used
	 * @see Server#Server() Server()
	 */
	public static void main(String[] args) {
		ChatServer server = new ChatServer();
		server.run();
	}
}
