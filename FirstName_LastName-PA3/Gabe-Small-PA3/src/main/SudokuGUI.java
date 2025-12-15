package main;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class SudokuGUI extends JFrame {

	private static final long serialVersionUID = 1L;

	private final JTextField[][] cells = new JTextField[9][9];
	private final boolean[][] originalClues = new boolean[9][9];
	private JPanel solutionsPanel;

	public SudokuGUI() {
		super("Sudoku Input");
		initUi();
	}

	private void initUi() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {}

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setResizeWeight(0.6);

		JPanel leftPanel = createLeftPanel();
		JScrollPane solutionsScrollPane = createSolutionsPanel();

		splitPane.setLeftComponent(leftPanel);
		splitPane.setRightComponent(solutionsScrollPane);

		setContentPane(splitPane);
		pack();
		setSize(1000, 700);
		splitPane.setDividerLocation(600);
		setResizable(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
	}

	private JPanel createLeftPanel() {
		JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
		leftPanel.setBorder(new LineBorder(Color.GRAY));

		JPanel gridPanel = createGridPanel();
		JPanel squareWrapper = new JPanel(new BorderLayout()) {
			@Override
			public Dimension getPreferredSize() {
				Dimension d = gridPanel.getPreferredSize();
				int size = Math.max(d.width, d.height);
				return new Dimension(size, size);
			}
			@Override
			public void doLayout() {
				int width = getWidth();
				int height = getHeight();
				int size = Math.min(width, height);
				gridPanel.setBounds((width - size) / 2, (height - size) / 2, size, size);
			}
		};
		squareWrapper.add(gridPanel, BorderLayout.CENTER);

		JPanel actions = createActionPanel();

		leftPanel.add(new JLabel("Enter digits 1-9; empty cells leave blank."), BorderLayout.NORTH);
		leftPanel.add(squareWrapper, BorderLayout.CENTER);
		leftPanel.add(actions, BorderLayout.SOUTH);

		return leftPanel;
	}

	private JPanel createGridPanel() {
		JPanel gridPanel = new JPanel(new GridLayout(9, 9, 0, 0));
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				JTextField tf = createCell(r, c);
				cells[r][c] = tf;
				gridPanel.add(tf);
			}
		}
		return gridPanel;
	}

	private JTextField createCell(int r, int c) {
		JTextField tf = new JTextField() {
			@Override
			public void setBounds(int x, int y, int width, int height) {
				int size = Math.min(width, height);
				super.setBounds(x, y, size, size);
			}
			@Override
			public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				int size = Math.max(d.width, d.height);
				return new Dimension(size, size);
			}
		};
		tf.setHorizontalAlignment(JTextField.CENTER);
		tf.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
		tf.setPreferredSize(new Dimension(40, 40));
		tf.setDocument(new javax.swing.text.PlainDocument() {
			@Override
			public void insertString(int offs, String str, javax.swing.text.AttributeSet a) throws javax.swing.text.BadLocationException {
				if (str == null) return;
				String filtered = str.replaceAll("[^1-9]", "");
				if (filtered.isEmpty()) return;
				if (getLength() == 0) {
					super.insertString(0, filtered.substring(0, 1), a);
				} else {
					replace(0, getLength(), filtered.substring(0, 1), a);
				}
			}
		});

		tf.setBorder(new javax.swing.border.MatteBorder(
			(r % 3 == 0) ? 3 : 0, (c % 3 == 0) ? 3 : 0,
			(r == 8) ? 3 : 1, (c == 8) ? 3 : 1, Color.DARK_GRAY));

		tf.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				int newRow = r, newCol = c;
				
				if (keyCode == KeyEvent.VK_UP) newRow = Math.max(0, r - 1);
				else if (keyCode == KeyEvent.VK_DOWN) newRow = Math.min(8, r + 1);
				else if (keyCode == KeyEvent.VK_LEFT) newCol = Math.max(0, c - 1);
				else if (keyCode == KeyEvent.VK_RIGHT) newCol = Math.min(8, c + 1);
				else return;
				
				if (newRow != r || newCol != c) {
					cells[newRow][newCol].requestFocus();
					cells[newRow][newCol].selectAll();
					e.consume();
				}
			}
		});
		return tf;
	}

	private JPanel createActionPanel() {
		JPanel actions = new JPanel();
		actions.add(createButton("Load", () -> {
			try {
				loadPuzzleFromFile();
				showMessage("Loaded from data/sudoku.txt");
			} catch (Exception ex) {
				showError("Failed to load sudoku.txt: " + ex.getMessage());
			}
		}));
		actions.add(createButton("Save", () -> {
			try {
				writePuzzleToFile();
				showMessage("Saved to data/sudoku.txt");
			} catch (FileNotFoundException ex) {
				showError("Failed to write sudoku.txt: " + ex.getMessage());
			}
		}));
		actions.add(createButton("Parse", () -> {
			try {
				writePuzzleToFile();
				SudokuSolver solver = new SudokuSolver();
				solver.parseSudoku();
				showMessage("Generated data/exactCover.txt");
			} catch (Exception ex) {
				showError("Parse failed: " + ex.getMessage());
			}
		}));
		actions.add(createButton("Solve", () -> {
			try {
				writePuzzleToFile();
				updateOriginalClues();
				SudokuSolver.main(new String[0]);
				loadAndDisplaySolutions();
				showMessage("Solved! Solutions displayed on the right.");
			} catch (Exception ex) {
				showError("Solve failed: " + ex.getMessage());
			}
		}));
		actions.add(createButton("Clear", () -> {
			for (int r = 0; r < 9; r++) {
				for (int c = 0; c < 9; c++) {
					cells[r][c].setText("");
				}
			}
		}));
		return actions;
	}

	private JButton createButton(String text, Runnable action) {
		JButton btn = new JButton(text);
		btn.addActionListener(e -> action.run());
		return btn;
	}

	private void showMessage(String msg) {
		JOptionPane.showMessageDialog(this, msg);
	}

	private void showError(String msg) {
		JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private JScrollPane createSolutionsPanel() {
		solutionsPanel = new JPanel();
		solutionsPanel.setLayout(new BoxLayout(solutionsPanel, BoxLayout.Y_AXIS));
		JScrollPane scrollPane = new JScrollPane(solutionsPanel);
		scrollPane.setPreferredSize(new Dimension(400, 600));
		scrollPane.setBorder(new LineBorder(Color.GRAY));
		solutionsPanel.add(new JLabel("Solutions will appear here after solving"));
		return scrollPane;
	}

	/**
	 * Specific to GUI, reads sudoku txt file 
	 * @throws IOException
	 */
	private void loadPuzzleFromFile() throws IOException {
		File sudokuFile = new File("data/sudoku.txt");
		if (!sudokuFile.exists()) {
			throw new FileNotFoundException("data/sudoku.txt not found");
		}
		
		try (BufferedReader reader = new BufferedReader(new FileReader(sudokuFile))) {
			for (int r = 0; r < 9; r++) {
				String line = reader.readLine();
				if (line == null) break;
				line = line.trim();
				for (int c = 0; c < 9 && c < line.length(); c++) {
					char ch = line.charAt(c);
					cells[r][c].setText((ch >= '1' && ch <= '9') ? String.valueOf(ch) : "");
				}
			}
		}
	}

	/**
	 * Specific to GUI, writes current display to sudoku file
	 * @throws FileNotFoundException
	 */
	private void writePuzzleToFile() throws FileNotFoundException {
		File dataDir = new File("data");
		if (!dataDir.exists()) dataDir.mkdirs();
		
		try (PrintWriter out = new PrintWriter(new File(dataDir, "sudoku.txt"))) {
			for (int r = 0; r < 9; r++) {
				StringBuilder sb = new StringBuilder(9);
				for (int c = 0; c < 9; c++) {
					String text = cells[r][c].getText();
					sb.append((text == null || text.isEmpty()) ? '.' : text.charAt(0));
				}
				out.println(sb.toString());
			}
		}
	}

	private void updateOriginalClues() {
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				originalClues[r][c] = !cells[r][c].getText().isEmpty();
			}
		}
	}

	private void loadAndDisplaySolutions() {
		solutionsPanel.removeAll();
		try {
			SudokuSolver s = new SudokuSolver();
			String[] sudokuBoards = s.extractSudokuSolutions();

			int i = 0;
			if (sudokuBoards != null) {
				for (String board : sudokuBoards) {
					if (board != null) {
						displaySolution(i++, board);
					}
				}
			}
			if (i == 0) {
				JLabel titleLabel = new JLabel("No Solutions found");
				titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
				solutionsPanel.add(titleLabel);
			}
			solutionsPanel.revalidate();
			solutionsPanel.repaint();
		} catch (FileNotFoundException e) {
			solutionsPanel.add(new JLabel("Error reading solutions: " + e.getMessage()));
		}
	}

	private void displaySolution(int solutionNum, String board) {
		char[][] grid = new char[9][9];
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				grid[i][j] = '.';
			}
		}

		int j, k, cells;
		j = k = cells = 0;
		for (int i=0; i<board.length(); i++) {
			char digit = board.charAt(i);
			if ('1' <= digit && digit <= '9') {
				grid[j][k] = digit;
				cells++;
				k++;
				if (k==9) {
					j++;
					k = 0;
				}
			}
		}
		
		if (cells != 81) {
			solutionsPanel.add(new JLabel("board string did not contain 81 digits"));
		}

		JPanel solutionPanel = new JPanel(new BorderLayout());
		JLabel titleLabel = new JLabel("Solution #" + (solutionNum + 1));
		titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
		solutionPanel.add(titleLabel, BorderLayout.NORTH);

		JPanel gridPanel = new JPanel(new GridLayout(9, 9, 0, 0));
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				JTextField cell = new JTextField();
				cell.setHorizontalAlignment(JTextField.CENTER);
				cell.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
				cell.setPreferredSize(new Dimension(25, 25));
				cell.setEditable(false);

				if (grid[r][c] != '.') {
					cell.setText(String.valueOf(grid[r][c]));
					if (originalClues[r][c]) {
						cell.setBackground(new Color(255, 255, 200));
						cell.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
					} else {
						cell.setBackground(Color.WHITE);
					}
				} else {
					cell.setBackground(Color.LIGHT_GRAY);
				}
				cell.setBorder(new javax.swing.border.MatteBorder(
					(r % 3 == 0) ? 2 : 0, (c % 3 == 0) ? 2 : 0,
					(r == 8) ? 2 : 1, (c == 8) ? 2 : 1, Color.DARK_GRAY));

				gridPanel.add(cell);
			}
		}
		solutionPanel.add(gridPanel, BorderLayout.CENTER);
		solutionsPanel.add(solutionPanel);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new SudokuGUI().setVisible(true));
	}
}
