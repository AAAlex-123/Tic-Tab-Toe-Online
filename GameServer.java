package ttt_online;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.*;

import javax.swing.JOptionPane;

/**
 * Server implementation that allows its users to play a game of Tic Tac Toe. By
 * default always runs a ChatServer
 */
final class GameServer extends Server {
	// port of the Game Server
	private static final int GAME_PORT = 10001;

	private final Socket[] sockets;
	private final Color[] colors;

	private final GameBoard gameBoard;
	private int currentPlayer = 0, boardSize, winCondition;

	/**
	 * Constructor to initialize fields.
	 * 
	 * @see Server#Server() Server()
	 */
	public GameServer() {
		super();
		sockets = new Socket[playerCount];
		colors = new Color[playerCount];
		gameBoard = new GameBoard(boardSize, winCondition);
	}

	/**
	 * Main method. Run to create and run a Game Server. Also creates and runs a
	 * Chat Server with the same parameters as the Game Server.
	 *
	 * @param args not used
	 * @see Server#Server() Server()
	 */
	public static void main(String[] args) {
		GameServer server = new GameServer();
		ChatServer chatServer = new ChatServer(Server.playerCount, Server.printStackTrace);
		ExecutorService exec = Executors.newCachedThreadPool();
		exec.execute(server);
		exec.execute(chatServer);
	}

	/**
	 * Main method that calls other methods to actually run the server
	 * 
	 * @see GameServer#initializeServer() InitializeServer()
	 * @see GameServer#getConnections() getConnections()
	 * @see GameServer#makeTurn() makeTurn()
	 */
	@Override
	public void run() {
		initializeServer();
		getConnections();

		log("Starting game");
		while (true) {
			makeTurn();
		}
	}

	@Override
	protected void getServerOptions() {
		super.getServerOptions();
		while (!argumentsPassed) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// why set to false again?
		argumentsPassed = false;

		JFrame optWind = new JFrame("Select Game Options");
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500, 300));
		optWind.setResizable(false);

		// mainPanel = listPanel + submitBut
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		Font font = new Font("Serif", Font.BOLD, 25);

		// listPanel = boardPanel+winCondPanel
		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.X_AXIS));

		// boardPanel = boardLabel + (scroll) boardList
		JPanel boardPanel = new JPanel();
		boardPanel.setLayout(new BoxLayout(boardPanel, BoxLayout.Y_AXIS));
		JLabel boardLabel = new JLabel("Choose the boards size");
		String[] boardOptions = { "3x3", "4x4", "5x5", "6x6", "7x7", "8x8" };
		JList<String> boardLs = new JList<String>(boardOptions);
		boardLs.setBackground(Color.BLUE);
		boardLs.setFont(font);
		boardLs.setSelectedIndex(2);
		JScrollPane scrollList = new JScrollPane(boardLs);
		scrollList.setPreferredSize(new Dimension(100, 100));
		boardPanel.add(boardLabel);
		boardPanel.add(scrollList);

		// winCondPanel = winLabel + autismPanel
		JPanel winCondPanel = new JPanel();
		winCondPanel.setLayout(new BoxLayout(winCondPanel, BoxLayout.Y_AXIS));
		// autismPanel = centered winList
		JPanel autismPanel = new JPanel();
		JLabel winLabel = new JLabel("Select how many marks are needed to win");
		String[] winOptions = { "3 marks", "4 marks", "5 marks" };
		JList<String> winLs = new JList<String>(winOptions);
		winLs.setBackground(Color.BLUE);
		winLs.setFont(font);
		winLs.setSelectedIndex(0);
		autismPanel.add(Box.createRigidArea(new Dimension(25, 25)));
		autismPanel.add(winLs);
		winCondPanel.add(winLabel);
		winCondPanel.add(autismPanel);

		listPanel.add(boardPanel);
		listPanel.add(winCondPanel);

		JButton submitBut = new JButton("Submit");
		mainPanel.add(listPanel);
		mainPanel.add(submitBut);
		optWind.add(mainPanel);
		submitBut.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boardSize = boardLs.getSelectedIndex() + 3;
				winCondition = winLs.getSelectedIndex() + 3;
				if (winCondition > boardSize) {
					JOptionPane.showMessageDialog(optWind,
							"The current configuration would lead to an unwinnable game.", "Invalid Options",
							JOptionPane.ERROR_MESSAGE);
				} else {
					optWind.dispose();
					argumentsPassed = true;
				}
			}
		});
		optWind.revalidate();
	}

	/**
	 * Initializes the server on port {@code GAME_PORT} with
	 * {@code playerCount} total connections.
	 */
	protected void initializeServer() {
		try {
			server = new ServerSocket(GAME_PORT, playerCount);
			log(String.format("Game Server ready, waiting for %d players", playerCount));
		} catch (BindException e) {
			logerr("BindException while setting up server; a server is already running on this port", e,
					printStackTrace);
			JOptionPane.showMessageDialog(Server.screen,
					String.format("Error while setting up server:\nPort %d already in use\n\nServer will now exit",
							GAME_PORT),
					"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} catch (IOException e) {
			logerr("IOException while setting up server", e, printStackTrace);
		}
	}

	/**
	 * <ul>
	 * <li>Initializes {@code playerCount} connections.
	 * <li>Gets their input and output streams.
	 * <li>Exchanges some messages.
	 * <li>Increments the {@code {@link Server#gameConnected gameConnected}}
	 * counter.
	 * </ul>
	 * If two or more players have the same symbol, the server allocates them
	 * special unique chess pieces and the clients interpret the message
	 * accordingly.<br>
	 * <br>
	 * If at any point something goes wrong, reset server :)
	 * 
	 * @see GameServer#reset() reset()
	 */
	protected void getConnections() {
		boolean reset = false;
		try {
			// connect to every player
			for (int i = 0; i < playerCount; i++) {
				// get connections
				sockets[i] = server.accept();
				inputs[i] = new ObjectInputStream(sockets[i].getInputStream());
				outputs[i] = new ObjectOutputStream(sockets[i].getOutputStream());

				// get player symbol
				symbols[i] = (char) inputs[i].readObject();
				colors[i] = (Color) inputs[i].readObject();

				// exchange send ack message
				outputs[i].writeObject(
						String.format("Hi player '%c', you're now connected as #%d.\nPlease wait for others to join.",
								symbols[i], i));

				log(String.format("\nPlayer #%d connected as '%c'", i, symbols[i]));
				screen.updateGameConnectionCounter(1);
			}

			char[] found = new char[playerCount];
			int foundIndex = 0;

			// check if there are duplicates and if there are, put chess piece
			// for some reason Arrays.asList(found).contains(symols[i]) doesn't work so
			// nested for loops
			for (int i = 0; i < playerCount; i++) {
				for (int j = 0; j < playerCount; j++) {
					if (found[j] == symbols[i]) {
						// if there is a duplicate, replace it with a random piece from chessPieces
						char chessPiece = chessPieces
								.remove(ThreadLocalRandom.current().nextInt(0, chessPieces.size()));
						log(String.format("Duplicate found '%c', replaced with '\\u%04x'", symbols[i],
								(int) chessPiece));
						symbols[i] = chessPiece;
						break;
					}
				}
				found[foundIndex++] = symbols[i];
			}

			// send ready message and symbol and color array
			for (int j = 0; j < playerCount; j++) {
				outputs[j].writeObject("Everyone has joined; get ready to start the game!");
				outputs[j].writeObject(symbols);
				outputs[j].writeObject(colors);
			}

		} catch (IOException e) {
			logerr("IOException inside getConnections() while getting connections or sending messages", e,
					printStackTrace);
			reset = true;
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException inside getConnections() while getting player symbol", e, printStackTrace);
			reset = true;
		}

		if (reset) {
			reset();
			run();
		}
	}

	/**
	 * Executes one turn of a player.
	 * <ul>
	 * <li>Sends 'ok' to start
	 * <li>Sends the board
	 * <li>Receives and processes the move
	 * <li>Sends acknowledgement for the move
	 * <li>Resends board
	 * </ul>
	 * 
	 * If something goes wrong, reset the server.
	 * 
	 * @see GameServer#sendBoard(int) sendBoard()
	 * @see GameServer#reset() reset()
	 */
	private void makeTurn() {
		boolean reset = false;
		log(String.format("\nPlayer #%d starts their turn", currentPlayer));
		try {
			log(String.format("Sent board:\n%s", gameBoard));
			int move;
			String response;

			// send ok to start
			outputs[currentPlayer].writeObject("Make your move!");

			// send board
			sendBoard(currentPlayer);

			// get, register and respond to move
			move = (int) inputs[currentPlayer].readObject();
			log("Just got " + move);

			if (move == -2) {
				log("Final board:\n" + gameBoard);
				broadcast("Player '%c' resigned", symbols[currentPlayer]);
				log(String.format("Player '%c' resigned!\nGame over", symbols[currentPlayer]));
				for (int i = 0; i < playerCount; i++)
					sendBoard(i);
				log("Server will now reset");
				reset();
				run();
			}

			gameBoard.markSquare(move, symbols[currentPlayer]);
			// check if game has ended
			if (gameBoard.hasWon() || gameBoard.hasTied()) {
				log("Final board:\n" + gameBoard);
				String msg = gameBoard.hasTied() ? "It's a tie!"
						: String.format("Player '%c' won!", symbols[currentPlayer]);
				broadcast(msg);
				log(msg + "\nGame over");
				for (int i = 0; i < playerCount; i++) {
					sendBoard(i);
				}

				log("Server will now reset");
				reset();
				run();
			}

			// send response to move
			response = (String.format("Move received: [%c, %d]", 65 + move / 10, move % 10 + 1));
			outputs[currentPlayer].writeObject(String.format("%c", '\u2713'));

			log(String.format("Move received: '%d', response sent '%s'", move, response));

			// send board again
			sendBoard(currentPlayer);

			currentPlayer = (currentPlayer + 1) % playerCount;

		} catch (SocketException e) {
			logerr("SocketException inside makeTurn()", e, printStackTrace);
			reset = true;
		} catch (IOException e) {
			logerr("IOException inside makeTurn() while sending/receiving data", e, printStackTrace);
			reset = true;
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException inside makeTurn() while getting move\\n\n", e, printStackTrace);
			reset = true;
		}

		if (reset) {
			reset();
			run();
		}
	}

	/**
	 * Resets everything in case something goes wrong while getting connections.<br>
	 * Closes connections, empties {@code symbols} array and resets the
	 * {@code {@link Server#gameConnected gameConnected}} counter.
	 */
	private void reset() {
		gameBoard.clear();
		try {
			server.close();
		} catch (IOException e) {
			logerr("IOException while closing server while reset", e, printStackTrace);
		}
		for (int i = 0; i < playerCount; i++) {
			try {
				sockets[i].close();
				inputs[i].close();
				outputs[i].close();
				symbols[i] = '\u0000';
				colors[i] = new Color(0, 0, 0);
			} catch (SocketException e) {
				logerr(String.format("SocketException inside reset() %d\n", i), e, printStackTrace);
			} catch (IOException e) {
				logerr(String.format("IOException in reset() %d; player disconnected\n", i), e, printStackTrace);
			} catch (NullPointerException e) {
				logerr(String.format("NullPointerException in reset() %d; player never joined\n", i), e,
						printStackTrace);
			}
		}
		log("Done resetting");
		screen.updateGameConnectionCounter(0);
	}

	/**
	 * Sends the board to the currentPlayer.
	 * <p>
	 * It works by de-constructing the GameBoard here and re-constructing it at the
	 * client using the GameBoard's {@code char[][] array} because there is a
	 * problem when sending GameBoard objects.
	 * 
	 * @see GameBoard#getBoard() getBoard()
	 * 
	 * @throws SocketException Thrown if client disconnects while trying to send
	 *                         board
	 */
	private void sendBoard(int currentPlayer) throws SocketException {
		char[][] newBoard = new char[boardSize][boardSize];
		char[][] currentBoard = gameBoard.getBoard();
		for (int i = 0; i < boardSize; i++)
			for (int j = 0; j < boardSize; j++)
				newBoard[i][j] = currentBoard[i][j];
		try {
			outputs[currentPlayer].writeObject(newBoard);
		} catch (IOException e) {
			logerr("Error while sending board", e, printStackTrace);
		}
	}
}
