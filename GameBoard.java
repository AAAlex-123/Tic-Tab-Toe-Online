package ttt_online;

/**
 * Represents the board where Tic-Tac-Toe is played.
 */
public class GameBoard {

	final static char EMPTY = GameEngine.DASH;
	public final int CONSECUTIVE;
	public final int SIZE;
	private final char[][] board;

	/**
	 * Initialize board of size <code>dimension</code> with <code>EMPTY</code>
	 * characters
	 */
	public GameBoard(int boardSize,int winningCondition) {
		this.SIZE = boardSize;
		this.CONSECUTIVE = winningCondition;
		this.board = new char[SIZE][SIZE];
		for (int i = 0; i < boardSize; i++)
			for (int j = 0; j < boardSize; j++)
				board[i][j] = EMPTY;
	}

	/**
	 * Initialize board by copying board <code>table</code>
	 * 
	 * @param table char[][], the table to be copied
	 */
	public GameBoard(char[][] table) {
		this.SIZE = table.length;
		this.CONSECUTIVE = 5;//max value, doesn't matter as the check only happens on the GameServer
		this.board = new char[SIZE][SIZE];
		for (int i = 0; i < SIZE; i++)
			for (int j = 0; j < SIZE; j++)
				board[i][j] = table[i][j];
	}

	/**
	 * Marks square a square <code>coord</code> of the board with
	 * <code>mark</code>.<br>
	 * Returns boolean indicating success or failure.
	 * 
	 * @param coord int, the coordinate of the square to be marked, in the form of
	 *              10*row + col
	 * @param mark  char, the symbol used to mark the board
	 * @return boolean, true if square is empty, false if full (can't be marked).
	 * @see GameBoard#isValid(int) isValid()
	 */
	public boolean markSquare(int coord, char mark) {
		if (!isValid(coord))
			return false;
		board[coord / 10][coord % 10] = mark;
		return true;
	}

	/**
	 * Checks if a move can be played at square <code>coord</code>
	 * 
	 * @param coord int, the coordinate to check
	 * @return boolean, true if valid, false if not
	 */
	public boolean isValid(int coord) {
		return (board[coord / 10][coord % 10] == EMPTY) && (0 <= coord) && (coord <= 55);
	}

	/**
	 * Clears the board; fills all squares with <code>EMPTY</code>
	 */
	public void clear() {
		for (int i = 0; i < SIZE; i++)
			for (int j = 0; j < SIZE; j++)
				board[i][j] = EMPTY;
	}

	/**
	 * Returns a string representation of the board
	 */
	@Override
	public String toString() {
		StringBuffer repr = new StringBuffer("");
		for (char[] row : board) {
			for (char c : row) {
				repr.append(c);
				repr.append(" ");
			}
			repr.append("\n");
		}
		return repr.toString();
	}

	/**
	 * Checks if the game is over.<br>
	 * Supports custom <code>directions</code> (i.e. steps to take on the
	 * board).<br>
	 * Supports any amount of <code>consecutive</code> squares.
	 * 
	 * @return boolean, true if someone has won.
	 */
	public boolean hasWon() {
		int[][] directions = { { 1, 0 }, { 0, 1 }, { 1, -1 }, { 1, 1 } };
		for (int[] direction : directions) {
			// for every direction...
			int dx = direction[0];
			int dy = direction[1];
			for (int x = 0; x < board.length; x++) {
				for (int y = 0; y < board.length; y++) {
					// for every square starting from [x, y] in that direction...
					try {
						// assume it's a winning sequence
						boolean res = true;
						char target = board[x][y];
						for (int i = 0; i < CONSECUTIVE; i++) {
							// for <consecutive> squares in that direction,
							// "and" square==target and the result
							// so if once 'square != target', res is false
							char boardSquare = board[x + i * dx][y + i * dy];
							res = res && (target == boardSquare) && (boardSquare != EMPTY);
						}
						if (res)
							return true; // winner
					} catch (ArrayIndexOutOfBoundsException e) {
						; // if search goes outside the board go to the next square
					}
				}
			}
		}
		return false; // end of loop; no winner
	}

	/**
	 * Checks if the board is filled and no one has won; i.e. it's a tie.
	 * 
	 * @return boolean, true if there is a tie, false otherwise
	 */
	public boolean hasTied() {
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				if (board[i][j] == EMPTY)
					return false;
			}
		}
		return true;
	}

	/**
	 * Returns the GameBoard in 2D char[][] format
	 * 
	 * @return char[][], the board
	 */
	public char[][] getBoard() {
		return board;
	}
}
