import java.awt.*;               // Graphics, Color, Font, Dimension, Image, etc.
import java.awt.event.*;        // KeyListener, ActionListener, KeyEvent, ActionEvent
import java.util.ArrayList;     // Dynamic list to hold on-screen pipes
import java.util.Random;        // Utility to randomise pipe heights
import javax.swing.*;           // Swing widgets (JPanel, Timer, ImageIcon, etc.)

/**
 * Main game panel that runs the Flappy-Bird clone.
 * Handles rendering, physics, user input and game loop.
 */
public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    /* -------------------- BOARD CONSTANTS -------------------- */
    int boardWidth = 360;   // playing area width (pixels)
    int boardHeight = 640;  // playing area height (pixels)

    /* -------------------- IMAGE ASSETS -------------------- */
    Image backgroundImg;   // sky background
    Image birdImg;         // flappy bird sprite
    Image topPipeImg;      // upper pipe image
    Image bottomPipeImg;   // lower pipe image

    /* -------------------- BIRD PROPERTIES -------------------- */
    int birdX = boardWidth / 8;      // starting horizontal position
    int birdY = boardHeight / 2;     // starting vertical position
    int birdWidth = 34;              // collision box width
    int birdHeight = 24;             // collision box height

    /** Simple data container that bundles bird geometry + sprite. */
    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;                     // sprite to draw

        Bird(Image img) { this.img = img; }
    }

    /* -------------------- PIPE PROPERTIES -------------------- */
    int pipeX = boardWidth;            // pipes spawn just off the right edge
    int pipeY = 0;                     // top edge of pipe (will be offset randomly)
    int pipeWidth = 64;                // pixel width of pipe image
    int pipeHeight = 512;              // pixel height of pipe image

    /** Simple data container for one pipe (top OR bottom). */
    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;        // true when bird has crossed this pipe

        Pipe(Image img) { this.img = img; }
    }

    /* -------------------- GAME PHYSICS / STATE -------------------- */
    Bird bird;                         // player object
    int velocityX = -4;                // horizontal speed of pipes (negative = left)
    int velocityY = 0;                 // vertical speed of bird (updated by gravity & flaps)
    int gravity = 1;                   // pixels per frame acceleration downward

    ArrayList<Pipe> pipes;             // list of on-screen pipes
    Random random = new Random();      // used to randomise gap height

    Timer gameLoop;                    // 60 FPS update timer
    Timer placePipesTimer;             // spawns new pipe pair every 1.5 s
    boolean gameOver = false;          // flag set on collision
    double score = 0;                  // increments each time bird passes a pipe pair

    /* -------------------- CONSTRUCTOR -------------------- */
    FlappyBird() {
        // preferred size for JPanel; JFrame will respect this when pack() is called
        setPreferredSize(new Dimension(boardWidth, boardHeight));

        setFocusable(true);            // allow panel to receive key events
        addKeyListener(this);          // register ourselves for key callbacks

        // load image assets from project root (must be on classpath)
        backgroundImg = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        birdImg       = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        topPipeImg    = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        bird = new Bird(birdImg);      // create player
        pipes = new ArrayList<>();     // empty pipe list to start

        // timer that repeatedly calls placePipes() every 1.5 seconds
        placePipesTimer = new Timer(1500, e -> placePipes());
        placePipesTimer.start();

        // 60 FPS game loop: calls actionPerformed() repeatedly
        gameLoop = new Timer(1000 / 60, this);
        gameLoop.start();
    }

    /* -------------------- PIPE SPAWNING -------------------- */
    /** Creates a new top + bottom pipe pair with a randomly positioned gap. */
    public void placePipes() {
        // randomise top pipe's top-left y so gap moves up/down each time
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;   // vertical size of gap bird must fly through

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    /* -------------------- RENDERING -------------------- */
    /** Swing calls this automatically when we call repaint(). */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);   // let JPanel do its default clearing
        draw(g);                   // our custom painting
    }

    /** Draws background, bird, pipes and score on every frame. */
    public void draw(Graphics g) {
        // draw sky background (scaled to panel size)
        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);

        // draw bird sprite
        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

        // draw every pipe in list
        for (Pipe pipe : pipes) {
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // draw score (white, 32 pt Arial)
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + (int) score, 10, 35);
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }
    }

    /* -------------------- PHYSICS & COLLISIONS -------------------- */
    /** Updates bird position, pipe positions, and checks for collisions each frame. */
    public void move() {
        // apply gravity to vertical velocity, then move bird
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);   // can't go above screen top

        // scroll pipes left and test for pass / collision
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;   // negative value moves pipe left

            // if bird has passed this pipe, award 0.5 score (top + bottom = 1 point)
            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                pipe.passed = true;
                score += 0.5;
            }

            // simple AABB collision test
            if (collision(bird, pipe)) {
                gameOver = true;
            }
        }

        // bird fell off bottom of screen
        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    /** Axis-aligned bounding box collision between bird and a pipe. */
    public boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y + b.height &&
               a.y + a.height > b.y;
    }

    /* -------------------- GAME LOOP CALLBACK -------------------- */
    /** Called by Swing Timer every 16 ms (~60 FPS). */
    @Override
    public void actionPerformed(ActionEvent e) {
        move();                // update physics
        repaint();             // request Swing to paint again

        // stop timers when game ends
        if (gameOver) {
            placePipesTimer.stop();
            gameLoop.stop();
        }
    }

    /* -------------------- KEYBOARD INPUT -------------------- */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -25;   // flap: instant upward boost
            if (gameOver) {    // space also restarts game
                // reset bird position & physics
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                score = 0;
                gameOver = false;
                // restart timers
                gameLoop.start();
                placePipesTimer.start();
            }
        }
    }

    // Unused KeyListener methods still required by interface
    @Override public void keyTyped(KeyEvent e) { }
    @Override public void keyReleased(KeyEvent e) { }
}
