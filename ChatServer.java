package ttt_online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.JOptionPane;

/**
 * Server-side application to handle chat between players.
 */
final class ChatServer extends Server {

	// port of the Chat Server
	private static final int CHAT_PORT = 10002;

	/**
	 * Keeps track of the running threads to know when another connection can be
	 * accepted.<br>
	 * When one is created, give it a slot. When one is stopped, free up its slot.
	 * <p>
	 * No it's not spaghetti, yes we need it to keep track of which {@code outputs}
	 * indexes are empty and can have an Output Stream
	 */
	private final boolean[] available;

	/**
	 * Constructs the Chat Server object.
	 * 
	 * @see Server#Server() Server()
	 */
	ChatServer() {
		super();
		this.available = new boolean[playerCount];
		for (int i = 0; i < playerCount; i++)
			available[i] = true;
	}

	/**
	 * Constructs the Chat Server object without the UI
	 * 
	 * @param playerCount     int number of connections
	 * @param printStackTrace boolean, whether or not to print full Stack Trace on
	 *                        Exceptions
	 * @see Server#Server() Server()
	 */
	ChatServer(int playerCount, boolean printStackTrace) {
		super(playerCount, printStackTrace);
		// System.out.println(this.playerCount);
		this.available = new boolean[playerCount];
		for (int i = 0; i < playerCount; i++)
			available[i] = true;
	}

	/**
	 * Main method that calls other methods to actually run the server.<br>
	 * 
	 * @see ChatServer#initializeServer() InitializeServer()
	 * @see ChatServer#getConnections() getConnections()
	 */
	@Override
	public void run() {
		initializeServer();
		getConnections();
	}

	/**
	 * Initializes the Chat Server on port {@code CHAT_PORT} with
	 * {@code playerCount} total possible connections.
	 */
	protected void initializeServer() {
		try {
			server = new ServerSocket(CHAT_PORT);
			log(String.format("Chat Server ready, listening for up to %d players", playerCount));
		} catch (IOException e) {
			logerr("IOException in InitializeServer()", e, printStackTrace);
			if (printStackTrace)
				e.printStackTrace();
			JOptionPane.showMessageDialog(screen,
					String.format("Error while setting up server:\nPort %d already in use\n\nServer will now exit",
							CHAT_PORT),
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	/**
	 * Every 2 seconds check if there is an {@code available} slot to get a
	 * connection. If there is, wait to accept a connection and assign it this slot.
	 * Get its input and output streams. Create a new ChatServerThread with the
	 * slot's index and start it. Finally increment the {@code {@link
	 * Server#chatConnected chatConnected}} counter.
	 * 
	 * @see ChatServer#available available
	 * @see ChatServer#getAvailable() getAvailable()
	 * @see ChatServerThread
	 */
	protected void getConnections() {
		ExecutorService exec = Executors.newCachedThreadPool();
		try {
			int index = -1;
			Socket chatConnection = null;
			while (true) {
				// wait until there is an available slot to accept a connection
				index = getAvailable();
				if (index != -1) {
					chatConnection = server.accept();
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						;
					}
					continue;
				}

				// get Input Stream and Output Stream
				inputs[index] = new ObjectInputStream(chatConnection.getInputStream());
				outputs[index] = new ObjectOutputStream(chatConnection.getOutputStream());

				// get symbol, send ack back
				symbols[index] = (char) inputs[index].readObject();

				for (int i = 0; i < playerCount; i++) {
					if ((i != index) && (symbols[index] == symbols[i])) {
						char chessPiece = chessPieces
								.remove(ThreadLocalRandom.current().nextInt(0, chessPieces.size()));
						log(String.format("Duplicate found '%c', replaced with '\\u%04x'", symbols[index],
								(int) chessPiece));
						symbols[index] = chessPiece;
					}
				}

				// inform everyone that someone has joined
				broadcast("Chat Server: '%c' just joined. Say hi!", symbols[index]);

				outputs[index].writeObject(
						String.format("Hi player '%c', you're now connected.\nStart chatting!", symbols[index]));

				log(String.format("Chat Connection #%d established with '%c'", index, symbols[index]));

				exec.execute(new ChatServerThread(index));
				index++;
				screen.updateChatConnectionCounter(1);
			}
		} catch (IOException e) {
			logerr("IOException in getConnections()", e, printStackTrace);
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException in getConnections()", e, printStackTrace);
		}
	} // main part of ChatServer class

	/**
	 * Private inner class that listens to a specific client's Output Stream and
	 * upon receiving a message, {@code broadcasts} it to every client connected to
	 * the Chat Server.
	 * <p>
	 * When an Exception occurs, this Thread terminates execution, the client's
	 * Streams are closed, the {@code available} slot specified by {@code index} is
	 * freed up and the {@code {@link Server#chatConnected chatConnected}} counter
	 * is decremented
	 *
	 * @see ChatServer#broadcast(String, Object[]) broadcast()
	 * @see ChatServer#closeStreams(int) closeOutputStream()
	 * @see ChatServer#available available
	 */
	private class ChatServerThread implements Runnable {

		private final int index;

		/**
		 * Constructs the Thread to listen to {@code index} input Stream
		 * 
		 * @param count int, used to keep track the slot this thread occupies in the
		 *              {@code available} table
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
		@Override
		public void run() {
			log(String.format("Thread #%d started", index));
			while (true) {
				try {
					broadcast((String) inputs[index].readObject());
				} catch (SocketException e) {
					logerr(String.format("SocketException in ChatServerThread.run(); connection #%d closed by user\n",
							index), e, printStackTrace);
					closeStreams(index);
					break;
				} catch (IOException e) {
					logerr("IOException in ChatServerThread.run()", e, printStackTrace);
					closeStreams(index);
					break;
				} catch (ClassNotFoundException e) {
					logerr("ClassNotFoundException in ChatServerThread.run()", e, printStackTrace);
					closeStreams(index);
					break;
				}
			}
		}
	}

	// Utility methods for ChatServer below

	/**
	 * Returns the first slot of the {@code available} that is available to get a
	 * connection and sets it to {@code false}.
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
	 * Closes the Streams bound to client {@code index}, frees up the
	 * {@code available} slot this Thread occupied (specified by {@code index}) and
	 * decrements the {@code {@link Server#chatConnected chatConnected}} counter.
	 * 
	 * @param index int, the index of the client
	 * @see ChatServer#available available
	 */
	private void closeStreams(int index) {
		log("Closing thread " + index);
		broadcast("Chat Server: '%c' left the chat.", symbols[index]);
		try {
			outputs[index].close();
			inputs[index].close();
		} catch (IOException e) {
			logerr("IOException in closeStreams()", e, printStackTrace);
		} finally {
			outputs[index] = null;
			inputs[index] = null;
			symbols[index] = '\u0000';
			available[index] = true;
			screen.updateChatConnectionCounter(-1);
		}
	}
	
	void setScreen(Screen screen) {
		this.screen.dispose();
		this.screen = screen;
	}
	
	protected int getGameCount() {
		return 0;
	}	

	protected int getChatCount() {
		return playerCount;
	}

	/**
	 * Main method. Run to create and run a Chat Server.
	 *
	 * @param args not used
	 * @see Server#Server() Server()
	 */
	public static void main(String[] args) {
		ChatServer server = new ChatServer();
		server.setupScreen();
		server.run();
	}
}
