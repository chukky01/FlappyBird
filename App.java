import javax.swing.*;          // Loads Swing GUI toolkit (windows, panels, timers, etc.)
import javax.swing.JFrame;     // Explicit import of the top-level window class (redundant but harmless)

/**
 * Entry point for the Flappy-Bird clone.
 * Creates the main window and starts the game loop.
 */
public class App {
    public static void main(String[] args) throws Exception {

        /* ---- window dimensions ---- */
        int boardWidth = 360;   // playable width in pixels
        int boardHeight = 640;  // playable height in pixels

        /* ---- create the main window ---- */
        JFrame frame = new JFrame("Flappy Bird");     // title-bar text
        // frame.setVisible(true);                    // (commented out) would show empty window too early
        frame.setSize(boardWidth, boardHeight);       // physical size
        frame.setLocationRelativeTo(null);            // centre on screen
        frame.setResizable(false);                    // lock size so user can't stretch it
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // click X → terminate JVM

        /* ---- build & attach the game panel ---- */
        FlappyBird flappyBird = new FlappyBird();     // custom JPanel with game logic/graphics
        frame.add(flappyBird);                        // add panel to window content pane

        /* ---- finalize display ---- */
        frame.pack();                                 // shrink-wrap window around preferred panel size
        flappyBird.requestFocus();                    // ensure keyboard events reach game panel
        frame.setVisible(true);                       // finally show the complete window
    }
}
