package main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class SudokuSolver extends Solver {
    private Scanner scanner;
    private PrintWriter coverInput;
    private static final String SUDOKU_INPUT_PATH = "data/sudoku.txt";
    private static final String EXACT_COVER_INPUT_PATH = "data/exactCover.txt";
    private static final String EXACT_COVER_OUTPUT_PATH = "data/solution.txt";

    /**
     * Runs the Sudoku Solver, used in SudokuGUI.java
     * @param args
     */
    public static void main(String[] args) {
        try {
            SudokuSolver solver = new SudokuSolver();
            solver.parseSudoku();
            solver.readInput(EXACT_COVER_INPUT_PATH);
            solver.solve(10);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reduces an instance of sudoku found in data/sudoku.txt into
     * an exactCover instance and writes it to data/exactCover.txt
     * 
     * ASSUMPTIONS: input file is composed of 9 lines, where each line 
     * is composed of 9 characters that are either digits
     * or '.' to indicate a cell that isn't filled in 
     * @throws FileNotFoundException
     */
    public void parseSudoku() throws FileNotFoundException {
        coverInput = new PrintWriter(EXACT_COVER_INPUT_PATH);
        // Items
        StringBuilder items = new StringBuilder();
        for (int x = 0 ; x < 9 ; x++){
            for (int y = 0 ; y < 9 ; y++){
                items.append(String.format("p%d%d ", x,y));
            }
        }
        for (int x = 0 ; x < 9 ; x++){
            for (int k = 1 ; k < 10 ; k++){
                items.append(String.format("r%d%d ", x,k));
            }
        }
        for (int y = 0 ; y < 9 ; y++){
            for (int k = 1 ; k < 10 ; k++){
                items.append(String.format("c%d%d ", y,k));
            }
        }
        for (int z = 0 ; z < 9 ; z++){
            for (int k = 1 ; k < 10 ; k++){
                items.append(String.format("b%d%d ", z,k));
            }
        }

        
        coverInput.println(items);

        scanner = new Scanner(new File(SUDOKU_INPUT_PATH));
        for (int x = 0 ; x < 9 ; x++){
            String row = scanner.next();
            for (int y = 0 ; y < 9 ; y++){
                char val = row.charAt(y);
                int z = 3*(x/3) + y/3;
                if (val == '.'){
                    for (int k = 1; k < 10 ; k++){
                        coverInput.println(String.format("p%d%d r%d%d c%d%d b%d%d", x, y, x, k, y, k, z, k));
                    }
                } else {
                    coverInput.println(String.format("p%d%d r%d%s c%d%s b%d%s", x, y, x, val, y, val, z, val));
                }
            }
        }
        scanner.close();
        coverInput.close();
    }

    /**
     * @return an array representing the amount of solutions found from exactCover where each board is 81 digits
     * @throws FileNotFoundException
     */
    public String[] extractSudokuSolutions() throws FileNotFoundException {
        scanner = new Scanner(new File(EXACT_COVER_OUTPUT_PATH));
        String[] output = new String[1];
        int solutionCount = 0;
        while(scanner.hasNext() && scanner.nextLine().startsWith("Solution")){
            char[][] solution = new char[9][9];
            for (int i = 0 ; i < 81 ; i++){
                String option = scanner.nextLine();
                int p = option.indexOf('p');
                char x = option.charAt(p+1);
                char y = option.charAt(p+2);
                int r = option.indexOf('r');
                char k = option.charAt(r+2);
                solution[x-'0'][y-'0'] = k;
            }
            StringBuilder str = new StringBuilder();
            for (int i = 0 ; i < 9 ; i++){
                for (int j = 0 ; j < 9 ; j++){
                    str.append(solution[i][j]);
                }
            }

            // Increase the size as needed
            if(solutionCount >= output.length){
                String[] temp = new String[output.length * 2];
                System.arraycopy(output, 0, temp, 0, output.length);
                output = temp;
            }
            output[solutionCount] = str.toString();
            solutionCount++;
        }

        // Reduce the size of the output array
        if (solutionCount != output.length){
            String[] temp = new String[solutionCount];
            System.arraycopy(output, 0, temp, 0, solutionCount);
            return temp;
        }
        return output;
    }
}
