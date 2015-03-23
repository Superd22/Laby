import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 *  Classe principale, initialisation du modèle, et mise en place du contrôleur
 *  et de la vue.
 */

public class Laby {

    public static void main(String[] args) {

	// Initialisation du schéma MVC.
	LModel laby = new LModel(10, 12);
	JFrame frame = new JFrame();
	LController controller = new LController(laby, frame);
	LView view = new LView(laby, frame);
	// -- Schéma MVC initialisé.

	// Configuration de la fenêtre graphique.
	frame.setTitle("Laby");
	frame.pack();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.setVisible(true);
	// -- Fenêtre graphique configurée.
	
	// Configuration du labyrinthe.
	for (int i=0; i<10; i++) {
	    laby.putWall(i, 0);
	    laby.putWall(i, 11);
	}
	for (int j=0; j<12; j++) {
	    laby.putWall(0, j);
	    laby.putWall(9, j);
	}
	for (int i=0; i<7; i++) {
	    laby.putWall(i, 4);
	}
	for (int i=8; i<10; i++) {
	    laby.putWall(i, 4);
	}
	for (int j=4; j<7; j++) {
	    laby.putWall(3, j);
	}
	for (int j=8; j<12; j++) {
	    laby.putWall(3, j);
	}
	laby.putExit(0, 1);
	laby.putHero(1,6);
	laby.putMonster(6, 8);
	laby.putMonster(4, 2);
	// Quatrième section de configuration du labyrinthe
	// laby.putOpenDoor(3, 7);
	// laby.putClosedDoor(7, 4);
	// -- Fin de la quatrième section
	// -- Labyrinthe configuré.

    }

}

/**
 * Quelques exceptions pour les touches ou déplacements invalides.
 */

class EndGame extends Exception {}
class NotADirectionException extends Exception {}
class NotPassableException extends Exception {
    private Cell cell;
    public NotPassableException(Cell c) {
	this.cell = c;
    }
    public boolean becauseOfHero() {
	return (cell.getCreature() != null && cell.getCreature() instanceof Hero);
    }
    public boolean becauseOfMonster() {
	return (cell.getCreature() != null && cell.getCreature() instanceof Monster);
    }
}

/**
 *  Le labyrinthe proprement dit.
 */

class LModel extends Observable {
    // Un labyrinthe a : une hauteur, une largeur, un tableau de cellules,
    // un héros et une liste de monstres.
    private int h, l;
    private Cell[][] laby;
    private Hero hero = null;
    private ArrayList<Monster> monsters;

    public Cell get(int i, int j) { return laby[i][j]; }
    public int  getH()            { return h; }
    public int  getL()            { return l; }

    // Construction d'un labyrinthe aux dimensions données.
    // À l'origine, il n'y a ni héros ni monstre, et toutes les cases
    // sont libres.
    public LModel(int h, int l) {
	this.h = h;
	this.l = l;
	laby = new Cell[h][l];
	monsters = new ArrayList<Monster>();
	for (int i=0; i<h; i++) {
	    for (int j=0; j<l; j++) {
		laby[i][j] = new Cell(this, i, j);
	    }
	}
    }

    // Méthodes pour les déplacements et actions du héros.
    // Déplacement d'une case dans une direction, puis notification de la vue.
    public void heroMove(Direction dir) throws NotPassableException {
	hero.move(dir);
	setChanged();
	notifyObservers();
    }
    // Destruction du héros.
    public void killHero() { this.hero = null; }
    // -- Fin des méthodes du héros.

    // Méthodes pour le déplacement des monstres.
    public void monstersMove() throws EndGame {
	for (Monster monster : monsters) {
	    try {
		monster.move(Direction.random());
		setChanged();
		notifyObservers();
	    }
	    catch (NotPassableException e) { 
		if (e.becauseOfHero()) {
		    throw new EndGame();
		} else {
		    System.out.println("Monster cannot pass");
		}
	    }
	    catch (java.lang.ArrayIndexOutOfBoundsException e) {
		System.out.println("Monster out");
	    }
	}
    }
    // -- Fin du déplacement des monstres.

    // Méthodes pour la configuration du labyrinthe.
    public void putExit(int i, int j) {
	laby[i][j] = new Exit(this, i, j);
    }
    public void putWall(int i, int j) {
	laby[i][j] = new Wall(this, i, j);
    }
    public void putHero(int i, int j) {
	if (this.hero == null) {
	    hero = new Hero(laby[i][j]);
	}
    }
    public void putMonster(int i, int j) {
	monsters.add(new Monster(laby[i][j]));
    }
    // public void putOpenDoor(int i, int j) {
    // 	laby[i][j] = Door.openDoorFactory(this, i, j);
    // }
    // public void putClosedDoor(int i, int j) {
    // 	laby[i][j] = Door.closedDoorFactory(this, i, j);
    // }
    // -- Fin des méthodes de configuration.

}

/**
 * La vue principale du labyrinthe, qui affiche l'ensemble de la structure
 * et ses occupants.
 */

class LView extends JComponent implements Observer {
    // La vue mémorise une référence au labyrinthe et �  la fenêtre graphique.
    // On enregistre aussi la dimension et le côté de chaque case en pixels.
    private static final int SCALE = 40;
    private LModel laby;
    private JFrame frame;
    private Dimension dim;

    // Constructeur, où la vue s'enregistre comme un élément de la fenêtre
    // graphique et comme un observateur des modifications du labyrinthe.
    public LView(LModel laby, JFrame frame) {
	this.laby = laby;
	this.frame = frame;
	this.dim = new Dimension(SCALE*laby.getL(),SCALE*laby.getH());
	this.setPreferredSize(dim);
	laby.addObserver(this);
	frame.add(this);
    }

    // Méthode de mise �  jour pour réagir aux modification du modèle. 
    public void update(Observable o, Object arg) {
	repaint();
    }

    // Méthode d'affichage du labyrinthe.
    public void paintComponent(Graphics g) {
	Graphics2D g2 = (Graphics2D)g;
	for (int i=0; i<laby.getH(); i++) {
	    for (int j=0; j<laby.getL(); j++) {
		this.laby.get(i,j).paintCell(g2,j*SCALE,i*SCALE,SCALE);
	    }
	}
    }
}

/**
 * Le contrôleur des entrées du clavier. Il réagit aussi �  la souris pour
 * récupérer le focus.
 */

class LController extends JComponent implements KeyListener, MouseListener {
    // Le contrôleur garde une référence au labyrinthe.
    private LModel laby;

    // Constructeur : le contrôleur s'enregistre comme un récepteur des entrées
    // clavier et souris, et comme un élément graphique (nécessaire pour
    // récupérer le focus et les entrées).
    public LController(LModel laby, JFrame frame) {
	this.laby = laby;
	addKeyListener(this);
	addMouseListener(this);
	setFocusable(true);
	frame.add(this);
    }

    // Méthode qui récupère l'entrée clavier et appelle l'action correspondante
    // du héros. Si l'action du héros est valide, alors les monstres sont aussi
    // déplacés.
    public void keyTyped(KeyEvent e) {
	Direction dir;
	try {
	    try {
		switch (e.getKeyChar()) {
		case 'q':  dir = Direction.WEST; break;
		case 'd':  dir = Direction.EAST; break; 
		case 'z':  dir = Direction.NORTH; break;
		case 's':  dir = Direction.SOUTH; break;
		case 'e':  dir = Direction.CENTER; break;
		default: throw new NotADirectionException();
		}
		laby.heroMove(dir);
		laby.monstersMove();
	    }
	    // Si mouvement impossible ou touche invalide, rien ne se passe.
	    catch (NotPassableException ex) { 
		if (ex.becauseOfMonster()) {
		    throw new EndGame();
		} else {
		    System.out.println("Cannot pass");
		}
	    }
	    catch (NotADirectionException ex) {
		System.out.println("Not a direction");
	    }
	    // Si sortie du labyrinthe, fin de partie.
	    catch (java.lang.ArrayIndexOutOfBoundsException ex) {
		System.out.println("Gagné !"); laby.killHero();
	    }
	}
	// Si attrapé par un monstre, fin de partie.
	catch (EndGame ex) { System.out.println("Perdu !"); laby.killHero(); }
    }
    // -- Fin de l'action du clavier.

    // Récupère le focus quand on clique dans la fenêtre graphique.
    public void mouseClicked(MouseEvent e) {
	requestFocusInWindow();
    }
    // Pas de réaction aux autres stimuli.
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {} 
}

/**
 * À partir d'ici : les classes auxiliaires. Thèmes couverts dans l'ordre :
 * Les créatures du labyrinthe (héros et monstres).
 * Les directions.
 * Les cases (cases libres, murs, portes).
 */

/**
 * Classe générale des occupants du labyrinthes, avec une méthode de déplacement
 * et une méthode de dessin.
 */

abstract class Creature {
    protected Cell cell;
    public void paintCreature(Graphics2D g2, int leftX, int topY, int scale) {
	Ellipse2D ellipse = new Ellipse2D.Double(leftX, topY, scale, scale);
	g2.setPaint(this.getColor());
	g2.fill(ellipse);
    }
    abstract Color getColor();
    public Creature(Cell c) {
	this.cell = c;
	cell.addCreature(this);
    }
    public void move(Direction dir) throws NotPassableException {
	if (dir != Direction.CENTER) { 
	    Cell nextCell = cell.getNeighbour(dir);
	    if (nextCell.isPassable()) {
		cell.removeCreature();
		nextCell.addCreature(this);
		this.cell = nextCell;
	    } else {
		throw new NotPassableException(nextCell);
	    }
	}
    }
}

/**
 * Le héros possède une méthode supplémentaire pour agir.
 */

class Hero extends Creature {
    public Color getColor() { return Color.BLUE; }
    public Hero(Cell c) { super(c); }
    public void action(Direction dir) {
	Cell actCell = cell.getNeighbour(dir);
	if (actCell instanceof Door) {
	    ((Door)actCell).changeState();
	}
    }
}

class Monster extends Creature {
    public Color getColor() { return Color.GRAY; }
    public Monster(Cell c) { super(c); }
}


/**
 * Directions cardinales, et équivalents en différences de coordonnées.
 */

enum Direction {
    NORTH (-1, 0),
    SOUTH ( 1, 0),
    EAST  ( 0, 1),
    WEST  ( 0,-1),
    NE    (-1, 1),
    NW    (-1,-1),
    SE    ( 1, 1),
    SW    ( 1,-1),
    CENTER( 0, 0);

    private final int dI, dJ;
    private Direction(int di, int dj) { this.dI = di; this.dJ = dj; }
    public int dI() { return dI; }
    public int dJ() { return dJ; }
    public static Direction random() {
	Direction dir;
	switch((int)(10*Math.random())) {
	case 0: dir = NORTH; break;
	case 1: dir = SOUTH; break;
	case 2: dir = EAST;  break;
	case 3: dir = WEST;  break;
	case 4: dir = NE;    break;
	case 5: dir = NW;    break;
	case 6: dir = SE;    break;
	case 7: dir = SW;    break;
	default: dir = CENTER; break;
	}
	return dir;
    }
}

/**
 * Cases principales du labyrinthe.
 */

class Cell {
    // Une case peut contenir une créature. On maintient une référence vers
    // le labyrinthe et les coordonnées.
    private Creature creature = null;
    private final LModel laby;
    private final int i, j;

    public Cell(LModel laby, int i, int j) {
	this.laby = laby;
	this.i = i;
	this.j = j;
    }

    // Une case peut ou non être traversée par les créatures.
    public boolean isPassable() { return (creature==null); }
    public Cell getNeighbour(Direction dir) {
	return this.laby.get(i+dir.dI(), j+dir.dJ());
    }

    // Gérer l'occupant d'une case.
    public void addCreature(Creature c) { this.creature = c; }
    public void removeCreature() { this.creature = null; }
    public Creature getCreature() { return this.creature; }

    // Partie dessin.
    public void paintCell(Graphics2D g2, int leftX, int topY, int scale) {
	Rectangle2D rect = new Rectangle2D.Double(leftX, topY, scale, scale);
	g2.setPaint(this.getColor());
	g2.fill(rect);
	if (this.creature != null) {
	    this.creature.paintCreature(g2, leftX, topY, scale);
	}
    }
    public Color getColor() { return Color.WHITE; }

}

/**
 * Cases spéciales : sorties, murs, et portes.
 */

class Exit extends Cell {
    public Color getColor() { return Color.BLUE; }
    public Exit(LModel laby, int i, int j) { super(laby, i, j); }
}

class Wall extends Cell {
    public Color getColor() { return Color.BLACK; }
    public Wall(LModel laby, int i, int j) { super(laby, i, j); }
    public boolean isPassable() { return false; }
}
