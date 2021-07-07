package ttt_online;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * An abstract class that, when implemented, provides an easy way to integrate a
 * chat client within any other class.
 * <p>
 * To use it, first extend this class with a concrete class, possibly inner
 * class, while implementing the necessary methods, then create an object of
 * this class inside your class. <br>
 * Call the {@link ChatClient#getChatConnection(String, int)
 * getChatConnection(String, int)} method to connect to the server. <br>
 * Then the {@link ChatClient#initChat() initChat()} method to initialize the
 * two threads responsible for communicating with the server. <br>
 * Finally, call {@link ChatClient#setEnableChat(boolean)
 * setEnableChat(boolean)} to enable the chat box and button.
 * <p>
 * You <i>should</i> now be set!
 */
abstract class ChatClient implements Logging {

	private static final int WARNING = JOptionPane.WARNING_MESSAGE;
	private static final int ERROR = JOptionPane.ERROR_MESSAGE;

	protected String chatFieldSentMessages = "";
	private JPanel chatPanel;
	private JTextField chatTextArea;
	private JButton chatButton;

	private ChatReader chatReader;
	private ChatWriter chatWriter;

	private Socket chatSocket;
	protected ObjectOutputStream chatOutput;
	protected ObjectInputStream chatInput;

	/**
	 * Defines any other operations that need to take place when connecting to the
	 * Chat Server. Must return {@code 0} or {@code 1}.
	 * 
	 * @return 0 or 1, indicating success or failure
	 */
	abstract protected int _getChatConnection();

	/**
	 * Defines how incoming chat messages should be displayed to the user.
	 * 
	 * @param s String, the string to be displayed
	 */
	abstract protected void displayIncomingMessage(String s);

	/**
	 * Defines how the client should format the outgoing messages. Must always
	 * return {@code ""} when there aren't any messages to be sent.
	 * 
	 * @return the outgoing message
	 */
	abstract protected String getChatText();

	/**
	 * Defines how the ChatClient should exit when an Exception occurs.<br>
	 * Logs the error and informs the player with a pop-up which, when closed, exits
	 * the program or not, depending on {@code terminate} parameter. <br>
	 * Possible implementation with {@code JOptionPane.showMessageDialog}
	 * 
	 * @param error_msg String, the message to show the user
	 * @param log_msg   String, the message to log in the Error Stream
	 * @param type      int, ERROR, WARNING, the severity of the error
	 * @param e         Exception, the exception that occurred
	 * @param terminate boolean, whether or not an error has occurred and the
	 *                  application has to exit
	 * @param title     String, the title of the pop-up
	 */
	abstract protected void exit(String error_msg, String log_msg, int type, Exception e, boolean terminate,
			String title);

	/**
	 * Focuses the chat field
	 */
	protected void focusChat() {
		chatTextArea.requestFocusInWindow();
	}

	/**
	 * Initializes a connection to the Chat Server.<br>
	 * Gets its input and output streams.<br>
	 * <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @param address  String, the IP address of the ChatServer
	 * @param chatPort int, the port of the ChatServer where the client will connect
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return 0 or 1, indicating success or fail
	 */
	protected final int getChatConnection(String address, int chatPort) {
		try {
			// get connection
			chatSocket = new Socket(InetAddress.getByName(address), chatPort);
			chatOutput = new ObjectOutputStream(chatSocket.getOutputStream());
			chatOutput.flush();

			chatInput = new ObjectInputStream(chatSocket.getInputStream());

			if (_getChatConnection() == 1)
				return 1;

			focusChat();

		} catch (IOException e) {
			exit("Couldn't connect to Chat Server; if you're connected to game server you may still play.\n\nIf you don't know why this happened, please inform the developers",
					"!chat! IOException in getChatConnection()\nExiting...\n", WARNING, e, false, "Connection Error");
			return 1;
		}
		return 0;
	}

	/**
	 * Initializes connection to the Chat Server and starts two threads; one for
	 * reading and one for writing to chat.
	 */
	protected final void initChat() {
		chatReader = new ChatReader();
		chatWriter = new ChatWriter();
		ExecutorService exec = Executors.newCachedThreadPool();
		exec.execute(chatReader);
		exec.execute(chatWriter);
	}

	/**
	 * Pushes any message it receives from Chat Server to the UI.
	 */
	private final class ChatReader implements Runnable {
		public void run() {
			boolean err = false;
			while (!err) {
				try {
					// wait to receive a chat message and push it to the log JTextArea
					String msg = (String) chatInput.readObject();
					log("!chat! received message: " + msg);
					displayIncomingMessage(msg);
				} catch (IOException e) {
					exit("Connection to Chat Server lost;\n\nIf you don't know why this happened, please inform the developers",
							"!chat! IOException in chatReader.run()", WARNING, e, false, "Connection Error");
					return;
				} catch (ClassNotFoundException e) {
					exit("Something went very wrong; please exit and inform the developers.",
							"!chat! ClassNotFoundException in chatReader.run()", ERROR, e, true, "Very Serious Error");
				}
			}
		}
	} // ChatReader class

	/**
	 * Private inner class that, whenever there is chat text to send, sends it to
	 * the ChatServer's input Stream.
	 * <p>
	 * When an Exception occurs, a pop-up is displayed, the ChatServer's Streams are
	 * closed and this Thread terminates execution,
	 *
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 */
	private final class ChatWriter implements Runnable {

		/**
		 * Runs this thread; whenever there is chat text to send, sends it to the
		 * ChatServer's input Stream. When an Exception occurs, a pop-up is displayed,
		 * the ChatServer's Streams are closed and this Thread terminates execution,
		 *
		 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
		 */
		public void run() {
			boolean err = false;
			while (!err) {
				String chatText = getChatText();
				// if no chat has been sent, try again in 1 second
				if (chatText.equals("")) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// if a chat is sent, send it to the Chat Server
				else {
					try {
						log("!chat! sent message:     " + chatText);
						chatOutput.writeObject(chatText);
					} catch (IOException e) {
						exit("Connection to Chat Server lost; if you're connected to game server you may still play.\n\nIf you don't know why this happened, please inform the developers",
								"!chat! IOException in chatWriter.run()", WARNING, e, false, "Connection Error");
						return;
					}
				}
			}
		}
	} // ChatWriter class

	/**
	 * Enables or disables the chat UI elements
	 * 
	 * @param enable boolean, whether to enable or disable
	 */
	protected final void setEnableChat(boolean enable) {
		chatButton.setEnabled(enable);
		chatTextArea.setEnabled(enable);
		if (!enable)
			chatTextArea.setText("You're not connected to chat server");
		else
			chatTextArea.setText("");
	}

	/**
	 * Appends the text in the chat JTextField to the messages to be sent
	 */
	private final void sendChat() {
		chatFieldSentMessages += (chatFieldSentMessages.equals("") ? "" : "\n") + chatTextArea.getText();
		chatTextArea.setText("");
	}

	/**
	 * Returns a JPanel with the necessary UI for the ChatClient to function
	 * 
	 * @return the JPanel
	 */
	protected final JPanel render() {
		// logPanel -- chatPanel
		chatPanel = new JPanel();
		chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.X_AXIS));

		// logPanel -- chatPanel -- chatField / chatButton
		chatTextArea = new JTextField();
		chatTextArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				;
			}

			@Override
			public void keyReleased(KeyEvent e) {
				;
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					sendChat();
			}
		});
		chatButton = new JButton("Send");
		chatButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendChat();
			}
		});

		/* ADD */ chatPanel.add(chatTextArea);
		/* ADD */ chatPanel.add(chatButton);
		return chatPanel;
	}
}
