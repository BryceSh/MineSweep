import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.time.LocalTime;

public class MineSweapPart extends JFrame
{
    // Configuration for if you wanted to.
    private static final String UNEXPOSED_FLAGGED_MINE_SYMBOL = "@"; // Symbol for a flagged mine; NOTE: you can use emoji alt codes
    private static final String EXPOSED_MINE_SYMBOL = "X"; // Symbol for a mine
    private static final String HINT_MINE_SYMBOL = "?"; // Symbol for hints
    private static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE; // Default color of the unexposed cells
    private static final Color DEFAULT_HINT_COLOR = Color.green.darker(); // Default color for a hint cell
    private static final Color CELL_EXPOSED_BACKGROUND_COLOR = Color.lightGray; // Background color for exposed cells
    private static final Color[] CELL_EXPOSED_FOREGROUND_COLOR_MAP = {Color.lightGray, Color.blue, Color.green, Color.cyan, Color.yellow, Color.orange, Color.pink, Color.magenta, Color.red, Color.red}; // Color map for the cell numbers
    // Goes 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 | 9 = mine 0 = Empty

    private static final boolean DebugMode = false; // Enabling this will tell you were the mines are located, if you guess right and the total mines left.
    private static final int flagKey = ActionEvent.CTRL_MASK;  // What button you need to press to flag a cell
    private static final int unflagKey = ActionEvent.ALT_MASK; // What button you need to press to unflag a cell

    private static int hintCount = 3; // How many hints you can start with. Set 0 to disable


    // No touchy!
    private static final long serialVersionUID = 1L;
    private static int WINDOW_HEIGHT; // Add 20 to account for the menu bar
    private static int WINDOW_WIDTH;
    private static int MINE_GRID_ROWS;
    private static int MINE_GRID_COLS;
    private static int TOTAL_MINES;
    private static final int NO_MINES_IN_PERIMETER_GRID_VALUE = 0;
    private static final int ALL_MINES_IN_PERIMETER_GRID_VALUE = 8;
    private static final int IS_A_MINE_IN_GRID_VALUE = 9;
    private static LocalTime GameTime;
    private static final int DEFAULT_HINT = hintCount;

    private static int guessedMinesLeft = TOTAL_MINES;
    private static int actualMinesLeft = TOTAL_MINES;

    // visual indication of an exposed MyJButton
    // colors used when displaying the getStateStr() String

    private boolean running = true;
    // holds the "number of mines in perimeter" value for each MyJButton
    private final int[][] mineGrid;
    // Array list to hold where mines that haven't been found are. I'll store them in a 1D array index
    private static final ArrayList<Integer> minesLeftOnGrid = new ArrayList<>();
    private static final ArrayList<Integer> hintIndex = new ArrayList<>();

    public void updateGameData(int rows, int cols, int bombs) {
        MINE_GRID_ROWS = rows;
        MINE_GRID_COLS = cols;
        TOTAL_MINES = bombs;
        if ((rows > 0 && rows <= 10) || (cols > 0 && rows <=10)) {
            WINDOW_HEIGHT = 780;
            WINDOW_WIDTH = 760;
        } else if ((rows > 11 && rows <= 25) || (cols > 11 && rows <=25)) {
            WINDOW_HEIGHT = 880;
            WINDOW_WIDTH = 860;
        } else {
            WINDOW_HEIGHT = 1080;
            WINDOW_WIDTH = 1660;
        }
    }

    public MineSweapPart(int difficulty, int row, int col, int mines)
    {
        if (difficulty == 0)  // Easy
            updateGameData(9,9,10);
        else if (difficulty == 1) // Medium
            updateGameData(16, 16, 40);
        else if (difficulty == 2) // Hard
            updateGameData(25,25,90);
        else if (difficulty == 3) // Impossible
            updateGameData(25, 25, 623);
        else if (difficulty == 999)
            updateGameData(row, col, mines);

        mineGrid = new int[MINE_GRID_ROWS][MINE_GRID_COLS];
        minesLeftOnGrid.clear(); // This clears the ArrayList for the hint system.
        hintCount = Math.min(TOTAL_MINES, DEFAULT_HINT);
        guessedMinesLeft = TOTAL_MINES;
        actualMinesLeft = TOTAL_MINES;
        GameTime = LocalTime.now(); // Gets the current time
        this.setTitle("MineSweeper" + MineSweapPart.guessedMinesLeft +" Mines left");
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setResizable(false);
        //this.setLayout(new GridLayout(MINE_GRID_ROWS, MINE_GRID_COLS, 0, 0));
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        this.createMenu();
        this.createContents();
        // place MINES number of mines in mineGrid and adjust all the "mines in perimeter" values
        this.setMines();
        this.setVisible(true);
    }
    static JButton hintButton;
    public void createMenu() {
        JPanel panel = new JPanel();
        JMenuBar menuBar = new JMenuBar();
        JButton newGame = new JButton("New Game");
        hintButton = new JButton("Hint: " + hintCount);
        JButton resetHighscore = new JButton("Reset Highscore");
        newGame.addActionListener(new NewGameButton());
        hintButton.addActionListener(new HintButton());
        resetHighscore.addActionListener(new ResetHighScore());
        menuBar.add(newGame);
        menuBar.add(hintButton);
        menuBar.add(resetHighscore);
        panel.add(menuBar);
        this.add(panel, BorderLayout.NORTH);
    }

    static JPanel buttonPanel;

    public void createContents()
    {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(MINE_GRID_ROWS, MINE_GRID_COLS, 1, 1));
        for (int gr = 0; gr < MINE_GRID_ROWS; ++gr)
        {
            for (int gc = 0; gc < MINE_GRID_COLS; ++gc)
            {
                // set sGrid[gr][gc] entry to 0 - no mines in it's perimeter
                this.mineGrid[gr][gc] = 0;
                // create a MyJButton that will be at location (br, bc) in the GridLayout
                MyJButton but = new MyJButton("", gr, gc);
                // register the event handler with this MyJbutton
                but.setOpaque(true);
                but.setBorderPainted(false);
                but.setBorder(BorderFactory.createEmptyBorder());
                but.setBackground(DEFAULT_BACKGROUND_COLOR);
                but.addActionListener(new MyListener());
                but.setContentAreaFilled(true);
                // add the MyJButton to the GridLayout collection
                buttonPanel.add(but);
            }
        }
        this.add(buttonPanel, BorderLayout.CENTER);
    }

    // place TOTAL_MINES number of mines in mineGrid and adjust all of the "mines in perimeter" values
    // 40 pts
    private void setMines()
    {
        if (DebugMode) {
            System.out.println("Debug Mode Enabled! Mine Locations: ");
        }
        Random rn = new Random();
        for (int i = 0; i < TOTAL_MINES; ++i) {
            int gridROW;
            int gridCOL;
            do {
                gridROW = rn.nextInt(MINE_GRID_ROWS);
                gridCOL = rn.nextInt(MINE_GRID_COLS);
            } while (this.mineGrid[gridROW][gridCOL] == 9);
            this.mineGrid[gridROW][gridCOL] = 9;
            int mineIndex = (gridROW * MINE_GRID_ROWS) + gridCOL;
            minesLeftOnGrid.add(mineIndex);
            if (gridROW - 1 != -1 && gridCOL - 1 != -1 && this.mineGrid[gridROW - 1][gridCOL - 1] != 9)
                ++this.mineGrid[gridROW - 1][gridCOL - 1];
            if (gridROW - 1 != -1 && this.mineGrid[gridROW - 1][gridCOL] != 9)
                ++this.mineGrid[gridROW - 1][gridCOL];
            if (gridROW - 1 != -1 && gridCOL + 1 != MINE_GRID_COLS && this.mineGrid[gridROW - 1][gridCOL + 1] != 9)
                ++this.mineGrid[gridROW - 1][gridCOL + 1];
            if (gridCOL - 1 != -1 && this.mineGrid[gridROW][gridCOL - 1] != 9)
                ++this.mineGrid[gridROW][gridCOL - 1];
            if (gridCOL + 1 != MINE_GRID_COLS && this.mineGrid[gridROW][gridCOL + 1] != 9)
                ++this.mineGrid[gridROW][gridCOL + 1];
            if (gridROW + 1 != MINE_GRID_ROWS && gridCOL - 1 != -1 && this.mineGrid[gridROW + 1][gridCOL - 1] != 9)
                ++this.mineGrid[gridROW + 1][gridCOL - 1];
            if (gridROW + 1 != MINE_GRID_ROWS && this.mineGrid[gridROW + 1][gridCOL] != 9)
                ++this.mineGrid[gridROW + 1][gridCOL];
            if (gridROW + 1 != MINE_GRID_ROWS && gridCOL + 1 != MINE_GRID_COLS && this.mineGrid[gridROW + 1][gridCOL + 1] != 9)
                ++this.mineGrid[gridROW + 1][gridCOL + 1];
            if (DebugMode)
                System.out.println("\t Mine @ [" + gridROW + "][" + gridCOL + "]");
        }
    }

    private String getGridValueStr(int row, int col)
    {
        // no mines in this MyJbutton's perimeter
        if ( this.mineGrid[row][col] == NO_MINES_IN_PERIMETER_GRID_VALUE )
            return "";
            // 1 to 8 mines in this MyJButton's perimeter
        else if ( this.mineGrid[row][col] > NO_MINES_IN_PERIMETER_GRID_VALUE &&
                this.mineGrid[row][col] <= ALL_MINES_IN_PERIMETER_GRID_VALUE )
            return "" + this.mineGrid[row][col];
            // this MyJButton in a mine
        else // this.mineGrid[row][col] = IS_A_MINE_IN_GRID_VALUE
            return MineSweapPart.EXPOSED_MINE_SYMBOL;
    }

    private static class NewGameButton implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            System.out.println("Button Pressed");
            game.dispose();
            main(null);
        }
    }

    /*
    This is the hint system I made. If picks a random value from an array list of mines left that I made up above.
    So I had to do some tweaking to the way the buttons are stored in the panel. I had to make the panel a static
    variable to I could access it since the menu option is not a part of the button panel itself, so I couldn't get the parent
    Once I did that, I could just call the buttonPanel and do .getComponent(randomIndex) to get the button I wanted and set the text to be
    a star
     */
    // Redesigned
    private static class HintButton implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            if (hintCount > 0 && minesLeftOnGrid.size() != 0 && game.running) {
                Random rn = new Random();
                int randomIndex;
                do {
                    randomIndex = minesLeftOnGrid.get(rn.nextInt(minesLeftOnGrid.size()));
                } while (hintIndex.contains(randomIndex));
                MyJButton newButton = (MyJButton)(buttonPanel.getComponent(randomIndex));
                newButton.setText(HINT_MINE_SYMBOL);
                newButton.setBackground(DEFAULT_HINT_COLOR);
                --hintCount;
                hintButton.setText("Hints: " + hintCount);
                hintIndex.add(randomIndex);
            }
        }
    }

    private static class ResetHighScore implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            try {
                File oldFile = new File("highscore.msw");
                if (oldFile.delete()) {

                    if (!(oldFile.exists() && !oldFile.isDirectory())) {
                        PrintWriter file = new PrintWriter(new FileWriter("highscore.msw"));
                        file.println("9999999");
                        file.close();
                        JOptionPane.showMessageDialog(new JFrame(), "Successfully reset your highscore!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } catch (IOException e) {
                System.out.println("Something went wrong!");
            }
        }
    }

    private class MyListener implements ActionListener
    {
        public void actionPerformed(ActionEvent event)
        {
            if ( running )
            {
                // used to determine if ctrl or alt key was pressed at the time of mouse action
                int mod = event.getModifiers();
                MyJButton mjb = (MyJButton)event.getSource();
                // is the MyJbutton that the mouse action occurred in flagged
                boolean flagged = mjb.getText().equals(MineSweapPart.UNEXPOSED_FLAGGED_MINE_SYMBOL);
                // is the MyJbutton that the mouse action occurred in already exposed
                boolean exposed = mjb.getBackground().equals(CELL_EXPOSED_BACKGROUND_COLOR);
                // flag a cell : ctrl + left click
                if ( !flagged && !exposed && (mod & flagKey) != 0 )
                {
                    mjb.setText(MineSweapPart.UNEXPOSED_FLAGGED_MINE_SYMBOL);
                    --MineSweapPart.guessedMinesLeft;
                    // if the MyJbutton that the mouse action occurred in is a mine
                    // 10 pts
                    if ( mineGrid[mjb.ROW][mjb.COL] == IS_A_MINE_IN_GRID_VALUE ) {
                        --MineSweapPart.actualMinesLeft;
                        int index = (mjb.ROW * MINE_GRID_ROWS) + mjb.COL;
                        minesLeftOnGrid.remove(Integer.valueOf(index));
                        if (DebugMode)
                            System.out.println("Removed " + index + " from the mines left arraylist!");
                        if (MineSweapPart.actualMinesLeft == 0) {
                            String gameScore = "" + GameTime.until(LocalTime.now(), ChronoUnit.SECONDS);
                            String displayMessage = "Good job! You Win!\nYour Score: " + gameScore;
                            try {
                                BufferedReader highScoreFile = new BufferedReader(new FileReader("highscore.msw"));
                                int highScore = Integer.parseInt(highScoreFile.readLine());
                                highScoreFile.close();
                                int currentScore = Integer.parseInt(gameScore);
                                if (currentScore < highScore) {
                                    PrintWriter outFile = new PrintWriter(new FileWriter("highscore.msw"));
                                    outFile.println(currentScore);
                                    outFile.close();
                                    displayMessage = displayMessage + "\n\n\uD83C\uDF89NEW HIGH SCORE!\uD83C\uDF89";
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            JOptionPane.showMessageDialog(null, displayMessage);
                            running = false;
                        }
                    }
                    setTitle("MineSweeper | " + MineSweapPart.guessedMinesLeft +" Mines left");
                }
                // unflag a cell : alt + left click
                else if ( flagged && !exposed && (mod &  unflagKey) != 0 )
                {
                    mjb.setText("");
                    ++MineSweapPart.guessedMinesLeft;
                    // if the MyJbutton that the mouse action occurred in is a mine
                    // 10 pts
                    if ( mineGrid[mjb.ROW][mjb.COL] == IS_A_MINE_IN_GRID_VALUE )
                    {
                        // If flagged an actual mine.
                        int index = (mjb.ROW * MINE_GRID_ROWS) + mjb.COL;
                        minesLeftOnGrid.add(index);
                        if (DebugMode)
                            System.out.println("Added " + index + " back to the mines left ArrayList!");
                        ++MineSweapPart.actualMinesLeft;
                    }
                    setTitle("MineSweeper | " + MineSweapPart.guessedMinesLeft +" Mines left");
                }
                // expose a cell : left click
                else if ( !flagged && !exposed )
                {
                    exposeCell(mjb);
                }
            }
            if (DebugMode) {
                System.out.println("Debug Mode: ");
                System.out.println("\tGuessed Mines: " + guessedMinesLeft);
                System.out.println("\tActual Mines Left: " + actualMinesLeft);
            }
        }

        public void exposeCell(MyJButton mjb)
        {
            if ( !running )
                return;

            // expose this MyJButton
            mjb.setBackground(CELL_EXPOSED_BACKGROUND_COLOR);
            mjb.setForeground(CELL_EXPOSED_FOREGROUND_COLOR_MAP[mineGrid[mjb.ROW][mjb.COL]]);
            mjb.setText(getGridValueStr(mjb.ROW, mjb.COL));
            // if the MyJButton that was just exposed is a mine
            // 20 pts
            if ( mineGrid[mjb.ROW][mjb.COL] == IS_A_MINE_IN_GRID_VALUE )
            {
                // what else do you need to adjust?
                // could the game be over?
                // if the game is over - what else needs to be exposed / highlighted]
                for (int row = 0; row<MINE_GRID_ROWS; ++row) {
                    for (int col = 0; col<MINE_GRID_COLS; ++col) {
                        int index = (row * MINE_GRID_ROWS) + col;
                        if (mineGrid[row][col] == 9) {
                            MyJButton newButton = (MyJButton)(mjb.getParent().getComponent(index));
                            if (!newButton.getText().equals(MineSweapPart.UNEXPOSED_FLAGGED_MINE_SYMBOL)) {
                                newButton.setText(MineSweapPart.EXPOSED_MINE_SYMBOL);
                                newButton.setForeground(Color.RED.darker());
                            }
                        }
                    }
                }
                JOptionPane.showMessageDialog(null, "Sorry You lost!\nYou found " + (MineSweapPart.TOTAL_MINES - MineSweapPart.actualMinesLeft) + " Mines!");
                running = false;
                return;
            }

            // if the MyJButton that was just exposed has no mines in its perimeter
            // 20 pts
            if ( mineGrid[mjb.ROW][mjb.COL] == NO_MINES_IN_PERIMETER_GRID_VALUE )
            {
                /*
                   So the way I wrote my algorithm is as follows
                   So what I do first is I create a nested loop. These loops will basically get a 3x3 grid around the button that was presses.
                   From there, I run the loop to get all the rows and all the columns. So in order to get the information from the button, I get the value of the location on
                   the grid and calculate the index by using a simple equation. (currentRow * TOTAL_ROWS) + currentCol
                   from there I can get the information of the button to be able to tell if it's an empty space or not.
                   The way we do this is by checking if it does not have the text of the Flag and the background color is not set as exposed.
                   If it doesn't, then we rerun exposeCell for each of the parameter cells
                 */
                for(int x = -1; x<2; ++x){ // Loop to get the columns of each so a total of 3 columns.
                    for(int y = -1; y<2; ++y){ // Loop through each row of each column.
                        if(!(x == 0 && y == 0) && (mjb.ROW+x >= 0) && (mjb.ROW+x < MineSweapPart.MINE_GRID_ROWS) && (mjb.COL+y >= 0) && (mjb.COL+y < MineSweapPart.MINE_GRID_COLS)){
                            int index = ((mjb.ROW+x)*MineSweapPart.MINE_GRID_ROWS) + (mjb.COL+y);
                            MyJButton newButton = (MyJButton)(mjb.getParent().getComponent(index));
                            // if the new button is not flagged and not exposed then we rerun this method.
                            if (!newButton.getText().equals(MineSweapPart.UNEXPOSED_FLAGGED_MINE_SYMBOL) && !newButton.getBackground().equals(CELL_EXPOSED_BACKGROUND_COLOR)) {

                                exposeCell(newButton);
                            }
                        } // Close if
                    }
                } // Close for loops
            }
        }
    }
    // nested private class

    static MineSweapPart game;

    public static void main(String[] args) {

        // This is checking to see if the highscore file was generated / wasn't the first time playing the game
        File f= new File("highscore.msw");
        if (!(f.exists() && !f.isDirectory())) {
            try {
                PrintWriter file = new PrintWriter(new FileWriter("highscore.msw"));
                file.println("9999999");
                file.close();
            } catch (IOException e) {
                System.err.println("Something went wrong!\n" + e.getMessage() + "\n" + e.getCause());
            }
        }

        Object[] options = {"Easy", "Medium", "Hard", "Custom", "About"};
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel welcomeText = new JLabel("Welcome to MineSweeper");
        welcomeText.setHorizontalAlignment(JLabel.CENTER);
        welcomeText.setFont(new Font("Comic Sans MS", Font.PLAIN, 20));
        panel.add(welcomeText, BorderLayout.NORTH);
        JLabel secondaryText = new JLabel("Choose a difficulty");
        secondaryText.setHorizontalAlignment(JLabel.CENTER);
        secondaryText.setFont(new Font("Comic Sans MS", Font.PLAIN, 14));
        panel.add(secondaryText, BorderLayout.CENTER);
        JLabel dontSueText = new JLabel("Please don't sue me for calling this MineSweeper");
        dontSueText.setHorizontalAlignment(JLabel.CENTER);
        dontSueText.setFont(new Font("Comic Sans MS", Font.PLAIN, 8));
        panel.add(dontSueText, BorderLayout.CENTER);
        int result = JOptionPane.showOptionDialog(null, panel, "MineSweeper", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
        System.out.println(result);
        if (result > -1 && result < 3) // -1 is called if the press the close button
            game = new MineSweapPart(result, 0, 0, 0);
        else if (result == 3){
            boolean valid = false;
            while (!valid) {
                JPanel customPanel = new JPanel();
                customPanel.setLayout(new BorderLayout());
                JLabel topText = new JLabel("Enter Custom Values");
                topText.setHorizontalAlignment(JLabel.CENTER);
                customPanel.add(topText, BorderLayout.NORTH);
                JTextField cRows = new JTextField("Rows");
                JTextField cCols = new JTextField("Columns");
                JTextField cMines = new JTextField("Mines");
                customPanel.add(cRows, BorderLayout.WEST);
                customPanel.add(cCols, BorderLayout.CENTER);
                customPanel.add(cMines, BorderLayout.EAST);
                Object[] customOptions = {"Submit", "Cancel"};
                int customResult = JOptionPane.showOptionDialog(null, customPanel, "Custom Game", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, customOptions, null);
                System.out.println("Rows: " + cRows.getText() + " Columns: " + cCols.getText() + " Mines: " + cMines.getText());
                if (customResult == 0) {
                    int rows, col, mines;
                    try {
                        rows = Integer.parseInt(cRows.getText());
                        col = Integer.parseInt(cCols.getText());
                        mines = Integer.parseInt(cMines.getText());
                        game = new MineSweapPart(999, rows,col,mines);
                        valid = true;
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(new JFrame(), "Please enter only valid integers!", "Error", JOptionPane.ERROR_MESSAGE);
                        System.err.println("Number Formatting issue. Please try again!\n\t" + e.getMessage());
                    }
                } else {
                    main(null);
                }
            }
        } else if (result == 4) {
            new InformationScreen();
        } else {
            System.out.println("Program Terminated");
            System.exit(0); // This will close the program and prevent it from still running in the background.
        }
    }

}
