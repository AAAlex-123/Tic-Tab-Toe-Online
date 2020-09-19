package ttt_online;

/**
 * Represents the board where Tic-Tac-Toe is played.
 */
public class GameBoard {

	final static char EMPTY = GameEngine.DASH;
	private final int consecutive = 3;
	private final int boardSize = 5;
	private final char[][] board = new char[boardSize][boardSize];
	
	/**
	 * Initialise board of size <code>dimension</code> with <code>EMPTY</code> characters
	 */
	public GameBoard() {
		for(int i=0; i<boardSize; i++)
			for(int j=0; j<boardSize; j++)
				board[i][j] = EMPTY;
	}
	
	/**
	 * Initialise board by copying board <code>table</code>
	 * @param table char[][], the table to be copied
	 */
	public GameBoard(char[][] table) {
		for(int i=0; i<boardSize; i++)
			for(int j=0; j<boardSize; j++)
				board[i][j] = table[i][j];
	}

	/**
	 * Marks square a square of the board with the given mark.<br>
	 * Returns boolean indicating success or failure.
	 * 
	 * @param coord int, the coordinate of the square to be marked, in the form of 10*row + col
	 * @param mark char, the symbol used to mark the board
	 * @return boolean, true if square is empty, false if full (can't be marked).
	 * @see GameBoard#isValid(int) isValid()
	 */
	public boolean markSquare(int coord, char mark){
		if (!isValid(coord)) return false;
		board[coord/10][coord%10] = mark;
		return true;
	}
	
	/**
	 * Checks if a move can be played at square <code>coord</code>
	 * @param coord int, the coordinate to check
	 * @return boolean, true if valid, false if not
	 */
	public boolean isValid(int coord) {
		return board[coord/10][coord%10] == EMPTY && 0 <= coord && coord <= 55 ;
	}

	
	/**
	 * Clears the board; fills all squares with <code>EMPTY</code>
	 */
	public void clear() {
		for(int i=0; i<boardSize; i++)
			for(int j=0; j<boardSize; j++)
				board[i][j] = EMPTY;
	}
	
	/**
	 * Returns the String representation of the board
	 */
	@Override
	public String toString() {
		StringBuffer repr = new StringBuffer("");
		for(char[] row: board) {
			for(char c : row) {
				repr.append(c);
				repr.append(" ");
			}
			repr.append("\n");
		}
		return repr.toString();	
	}	
	
	/**
	 * Checks if the game is over.<br>
	 * Supports custom <code>directions</code> (i.e. steps to take on the board).<br>
	 * Supports any amount of <code>consecutive</code> squares.
	 * 
	 * @return boolean, true if someone has won.
	 */
	public boolean hasWon() {
		int[][] directions = {{1, 0}, {0, 1}, {1, -1}, {1, 1}};
	    for (int[] direction : directions) {
	    	// for every direction...
	        int dx = direction[0];
	        int dy = direction[1];
	        for (int x = 0; x < board.length; x++) {
	            for (int y = 0; y < board.length; y++) {
	            	// for every square...
            		// uncomment below and belower to see the checks being made
	            	// System.out.printf("\nstarting at (%d, %d) with direction {%d, %d}\n", x, y, dx, dy);
	            	try {
	            		// assume it's true
		            	char target = board[x][y];
		            	boolean res = true;
		            	for (int i = 0; i < consecutive; i++) {
		            		// for <consecutive> squares in that direction "and" square==target and the result
		            		// so if once square!=target, res is false
		            		char boardSquare = board[x + i*dx][y + i*dy];
		            		// uncomment below and above to see the checks being made
		            		// System.out.printf("checking: board[%d][%d] == %c\n", x + i*dx, y + i*dy, boardSquare);
		            		res = res && (target == boardSquare) && (boardSquare != EMPTY);
		            	}
		            	if (res) return true; // winner
	            	} catch (ArrayIndexOutOfBoundsException e) {
	            		; // if at any point the search goes outside the board go to the next square; don't return anything
	            	}
	            }
	        }
	    }
	    return false; // no winner
	}

	/**
	 * Returns the GameBoard in 2D array format
	 * 
	 * @return char[][], the board
	 */
	public char[][] getBoard() {
		return board;
	}
}
