package ttt_online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server-side application to handle communications with the clients
 */
public class ChatServer extends Server {
	private static final int CHAT_PORT = 10002;
	
	/**
	 * Keeps track of the running threads to know when another connection can be
	 * accepted.<br>
	 * When one is created, give it a slot. When one is stopped, free up its slot.
	 * <p>
	 * No it's not spaghetti, yes we need it to keep track of which <code>outputs</code> indexes
	 * are empty and can have an Output Stream
	 */
	private final boolean[] available;
	
	/**
	 * Constructs the Chat Server object.
	 * 
	 * FIXME documentation
	 */
	public ChatServer() {
		super();
		this.available = new boolean[playerCount];
		for (int i = 0; i < playerCount; i++)
			available[i] = true;
	}

	/**
	 * Runs the Chat Server
	 */
	protected void run() {
		initialiseServer();
		getConnections();
		log("all set");
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
	 * connection. If there is, accept a connection and assign it this slot then
	 * start a new <code>ChatServerThread</code> handing it the accepted Socket.
	 * 
	 * FIXME documentation
	 * 
	 * @see ChatServerThread
	 * @see ChatServer#getAvailable() getAvailable()
	 * @see ChatServer#available available
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
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						;
					}
					continue;
				}

				inputs[index] = new ObjectInputStream(chatConnection.getInputStream());
				outputs[index] = new ObjectOutputStream(chatConnection.getOutputStream());
				
				// exchange send ack message
				outputs[index].writeObject(String.format("Hi player #%d, you're now connected.\nPlease wait for others to join\n", index));

				log("\nChat Connection #%d established", index);

				new ChatServerThread(index).start();
				index++;
			}
		} catch (IOException e) {
			logerr("IOException in getConnections()\nidkwhatishappeningplshelp...");
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
	 * Private inner class that listens to a specific client's Output Stream<br>
	 * and upon receiving a message, broadcasts it to every client connected to the
	 * Chat Server.
	 * <p>
	 * When an Exception occurs, this Thread terminates execution,<br>
	 * the client's Output Stream is closed and the <code>available</code> slot<br>
	 * specified by <code>index</code> is freed up.
	 * 
	 * FIXME documentation
	 *
	 * @see ChatServer#broadcast(String, Object[]) broadcast()
	 * @see ChatServer#closeOutputStream(int) closeOutputStream()
	 * @see ChatServer#available available
	 */
	private class ChatServerThread extends Thread {

		private final int index;

		/**
		 * Constructs the Thread to listen to <code>socket</code>.
		 * 
		 * @param socket Socket, the socket this Threads listens to
		 * @param count  int, used to keep track the slot this thread occupies in the
		 *               <code>available</code> table
		 *               
		 * FIXME documentation
		 */
		public ChatServerThread(int count) {
			this.index = count;
		}
		
		/**
		 * Runs the thread
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
					closeOutputStream(index);
					break;
				} catch (IOException e) {
					logerr("IOException in ChatServerThread.run()\nidkwhatishappeningplshelp...");
					if (printStackTrace)
						e.printStackTrace();
					closeOutputStream(index);
					break;
				} catch (ClassNotFoundException e) {
					logerr("ClassNotFoundException in ChatServerThread.run()\nidkwhatishappeningplshelp...");
					if (printStackTrace)
						e.printStackTrace();
					closeOutputStream(index);
					break;
				}
			}
		}
	}

	/**
	 * Closes the Output Stream bound to client #<code>index</code><br>
	 * and frees up the <code>available</code> slot this Thread occupied (specified
	 * by <code>index</code>).
	 * 
	 * @param index int, the index of the client
	 * @see ChatServer#available available
	 * 
	 * FIXME documentation
	 */
	private void closeOutputStream(int index) {
		log("Closing thread #%d", index);
		try {
			outputs[index].close();
			inputs[index].close();
			outputs[index] = null;
			inputs[index] = null;
			available[index] = true;
		} catch (IOException e) {
			logerr("IOException in closeOutputStream()");
			if (printStackTrace)
				e.printStackTrace();
		}
	}

	/**
	 * Main method. Run to create and run a chat server
	 * 
	 * @param args args[0] is used for the number of players expected to connect
	 * @param args args[1] is used to determine <code>printStackTrace</code> field,
	 *             '1' for true, other for false
	 *             
	 * FIXME documentation
	 */
	public static void main(String[] args) {
		ChatServer server = new ChatServer();
		server.run();
	}
}
