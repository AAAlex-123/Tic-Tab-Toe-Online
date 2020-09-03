
public class GameBoard {
	
	private char[][] board = new char[5][5];
	
	public GameBoard() {
		for(int i=0;i<5;i++) {
			for(int j=0;j<5;j++) {
				board[i][j] = GameEngine.DASH;
			}
		}
	}
	
	public void markSquare(int coord,char mark) throws RuntimeException{
		if(coord>55) throw new RuntimeException("Invalid coordinates");
		board[coord/10][coord%10] = mark;
	}
	public boolean hasWon() {
		return (diagWin()||horizWin(board)||vertWin(board));
	}
	
	private boolean horizWin(char [][] table) {
		
		for(char[] row: table) {
			short consec = 1;
			char curr = row[0];
			
			for(char c : row) {
				if(c==curr && curr!=GameEngine.DASH) consec++;
				else{
					curr = c;
					consec = 1;
				}
				if (consec>=3) return true;
			}//row
		}
		return false;
	}
	
	private char[][] invertTable(char[][] table){
		char[][] newTable = new char[5][5];
		for(int i=0;i<newTable.length;i++) {
			for(int j=0;j<newTable.length;j++) {
				newTable[i][j] = table[j][i];
			}
		}
		return newTable;
	}
	
	private boolean vertWin(char[][] table) {
		return horizWin(invertTable(board));
	}
	
	private boolean diagWin() {
		int[][] directions = {{1,0}, {1,-1}, {1,1}, {0,1}};
	    for (int[] d : directions) {
	        int dx = d[0];
	        int dy = d[1];
	        for (int x = 0; x < board.length; x++) {
	            for (int y = 0; y < board.length; y++) {
	                int lastx = x + 2*dx;
	                int lasty = y + 2*dy;
	                if (0 <= lastx && lastx < board.length && 0 <= lasty && lasty < board.length) {
	                    char w = board[x][y];
	                    if (w != GameEngine.DASH && w == board[x+dx][y+dy] && w == board[lastx][lasty])  return true;
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
