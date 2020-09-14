package ttt_online;

import java.io.Serializable;

public class GameBoard implements Serializable {
	public static void main(String[] args) {
		GameBoard gb = new GameBoard();
		gb.markSquare(1, 'X');
		System.out.println(gb.toString());
		gb.markSquare(2, 'O');
		System.out.println(gb.toString());
		gb.markSquare(14, 'X');
		System.out.println(gb.toString());
		gb.markSquare(42, 'O');
		System.out.println(gb.toString());
		gb.markSquare(43, 'X');
		System.out.println(gb.toString());
	}
	
	private char[][] board = new char[5][5];
	
	public GameBoard() {
		for(int i=0; i<5; i++)
			for(int j=0; j<5; j++)
				board[i][j] = GameEngine.DASH;
	}
	
	public boolean markSquare(int coord, char mark){
		//true if successful, false if square already marked
		if (board[coord/10][coord%10] != GameEngine.DASH || coord > 55)
			return false;
		board[coord/10][coord%10] = mark;
		return true;
	}

	public boolean hasWon() {
		return (diagonalWin() || horizontalWin(board) || verticalWin(board));
	}
	
	private boolean horizontalWin(char[][] table) {
		for (char[] row: table) {
			short consec = 1;
			char curr = row[0];
			
			for (char c : row) {
				if (c == curr && curr != GameEngine.DASH) consec++;
				else {
					curr = c;
					consec = 1;
				}
				if (consec >= 3) return true;
			}	//row
		}
		return false;
	}
	
	private char[][] invertTable(char[][] table){
		char[][] newTable = new char[5][5];
		
		for(int i=0;i<newTable.length;i++)
			for(int j=0;j<newTable.length;j++)
				newTable[i][j] = table[j][i];
		return newTable;
	}
	
	private boolean verticalWin(char[][] table) {
		return horizontalWin(invertTable(table));
	}
	
	private boolean diagonalWin() {
		int[][] directions = {{1, -1}, {1, 1}};		//{1,0}, {0,1} to replace all other checks as well
	    for (int[] d : directions) {
	        int dx = d[0];
	        int dy = d[1];
	        for (int x = 0; x < board.length; x++) {
	            for (int y = 0; y < board.length; y++) {
	                int lastx = x + 2*dx;
	                int lasty = y + 2*dy;
	                if (0 <= lastx && lastx < board.length && 0 <= lasty && lasty < board.length) {
	                    char w = board[x][y];
	                    if (w != GameEngine.DASH && w == board[x+dx][y+dy] && w == board[lastx][lasty])
	                    	return true;	// winner
	                }
	            }
	        }
	    }
	    return false; // no winner
	}
	
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
	
	
}
