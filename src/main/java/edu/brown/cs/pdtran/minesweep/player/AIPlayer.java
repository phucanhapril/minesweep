package edu.brown.cs.pdtran.minesweep.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;

import edu.brown.cs.pdtran.minesweep.board.Board;
import edu.brown.cs.pdtran.minesweep.board.DefaultBoard;
import edu.brown.cs.pdtran.minesweep.setup.AIGamer;
import edu.brown.cs.pdtran.minesweep.tile.Tile;

/**
 * This class represents the AI that controls what moves the AI makes and how
 * it releases those moves.
 * @author Clayton Sanford
 *
 */
public class AIPlayer implements Player {

  private String username;
  private int score;
  private List<MovePossibility> certainMine;
  private List<MovePossibility> certainNotMine;
  private List<MovePossibility> uncertain;
  private int difficulty;
  private int moveTime;
  private double mistakeProbability;
  private Boolean canPlay;
  private DefaultBoard board;
  
  private static final double BASE_TIME = 5;
  private static final double TIME_MULTIPLIER = 25;
  private static final int MAX_DIFFICULTY = 10;
  private static final double MISTAKE_MULTIPLIER = .01;
  private static final double FLAG_PROBABILITY = .3;
  
  /**
   * Creates an AIPlayer with a username and a difficulty. This version will
   * be used primarily for testing.
   * @param username A string unique to that player.
   * @param difficulty An integer from 1 to 10 with 10 being the most
   * difficult.
   * @param board The Board object used by the AI to determine good moves to
   * make.
   */
  public AIPlayer(String username, int difficulty, DefaultBoard board) {
    this.username = username;
    score = 0;
    this.board = board;
    generateMovePossibilities();
    this.difficulty = difficulty;
    moveTime = (int) (BASE_TIME - difficulty * TIME_MULTIPLIER);
    mistakeProbability = (MAX_DIFFICULTY - difficulty) * MISTAKE_MULTIPLIER;
  }
  
  /**
   * Uses an AIGamer to produce an AIPlayer.
   * @param g An AIGamer produced for the purpose of being used in only the
   * game setup. The actual AIPlayer has the characteristics needed to make
   * moves.
   * @param board The Board object used by the AI to determine good moves to
   * make.
   */
  public AIPlayer(AIGamer g, DefaultBoard board) {
    username = g.getUserName();
    score = 0;
    generateMovePossibilities();
    difficulty = g.getDifficulty();
    this.board = board;
  }

  @Override
  /**
   * Gets the username corresponding to the player.
   * @return A unique string for a player's username.
   */
  public String getUsername() {
    return username;
  }

  @Override
  /**
   * Gets the score corresponding to the player.
   * @return An integer representing the player's score.
   */
  public int getScore() {
    return score;
  }

  @Override
  /**
   * Increments the score by an entered value.
   * @param change An integer to be added to the score. Negative if points are
   * to be lost.
   */
  public void changeScore(int change) {
    score += change;
  }

  @Override
  /**
   * Sends a Move to the network to be processed and implemented on the board.
   * @param move A Move to be sent.
   */
  public void makeMove(Move move) {
    // TODO Auto-generated method stub
  }
  
  private void generateMovePossibilities() {
    certainMine = new ArrayList<>();
    certainNotMine = new ArrayList<>();
    uncertain = new ArrayList<>();
    List<MineBlock> blocks = new ArrayList<>();
    int width = board.getWidth();
    int height = board.getHeight();

    // Creates a MineBlock for all undiscovered mines.
    Set<Tile> undiscovered = new HashSet<>();
    int bombCount = board.getBombCount();
    for (int w = 0; w < width; w++) {
      for (int h = 0; h < height; h++) {
        Tile currentTile = board.getTile(h, w);
        if (!currentTile.hasBeenVisited()) {
          undiscovered.add(currentTile);
        } else if (currentTile.isBomb()) {
          bombCount--;
        }
      }
    }
    blocks.add(new MineBlock(undiscovered, bombCount));

    // Creates a MineBlock for every discovered tile with neighboring mines
    for (int w = 0; w < width; w++) {
      for (int h = 0; h < height; h++) {
        Tile currentTile = board.getTile(h, w);
        if (currentTile.hasBeenVisited() && currentTile.getAdjacentBombs() > 0) {
          List<Tile> adjacentTiles = board.getAdjacentTiles(h, w);
          adjacentTiles.remove(currentTile);
          MineBlock mb1 =
              blockFromTile(currentTile.getAdjacentBombs(),
                  board.getAdjacentTiles(h, w));
          /*System.out.println(mb1.getNumMines());
          for (Tile adj: mb1.getTiles()) {
            System.out.println(adj.getRow() + " " + adj.getColumn());
          }
          System.out.println(" ");*/          
          Boolean needsCheck = true;
          while (needsCheck == true) {
            needsCheck = false;
            MineBlock remove = null;
            for (MineBlock mb2: blocks) {
              if (mb2.contains(mb1)) {
                mb2.subtract(mb1);                
                /*System.out.println("Subtract FROM");
                System.out.println(mb2.getNumMines());
                for (Tile adj: mb2.getTiles()) {
                  System.out.println(adj.getRow() + " " + adj.getColumn());
                }
                System.out.println(" ");*/                
                needsCheck = true;
                if (mb2.getTiles().isEmpty()) {
                  remove = mb2;
                  break;
                }
              } else if (mb1.contains(mb2)) {
                mb1.subtract(mb2);               
                /*System.out.println("Subtract");
                System.out.println(mb1.getNumMines());
                for (Tile adj: mb1.getTiles()) {
                  System.out.println(adj.getRow() + " " + adj.getColumn());
                }
                System.out.println(" ");*/               
                if (mb1.getTiles().isEmpty()) {
                  break;
                }
                needsCheck = true;
              }
            }
            blocks.remove(remove);
          }
          blocks.add(mb1);
        }
      }
    }

    // Converts MineBlocks to MovePossibilities
    for (MineBlock mb: blocks) {
      /*System.out.println(mb.getNumMines());
      for (Tile adj: mb.getTiles()) {
        System.out.println(adj.getRow() + " " + adj.getColumn());
      }*/
      double probability = (double) mb.getNumMines() / mb.getTiles().size();   
      for (Tile adj: mb.getTiles()) {
        MovePossibility mp = new MovePossibility(adj, probability);
        if (probability == 0) {
          certainNotMine.add(mp);
        } else if (probability == 1) {
          certainMine.add(mp);
        } else {
          boolean contained = false;
          for (MovePossibility uncertainMp: uncertain) {
            if (uncertainMp.getXCoord() == mp.getXCoord()
                && uncertainMp.getYCoord() == mp.getYCoord()) {
              contained = true;
              if (uncertainMp.getMineProbability() < probability) {
                uncertain.remove(uncertainMp);
                uncertain.add(mp);
              }
            }
          }
          if (!contained) {
            uncertain.add(mp);
          }
        }
      }
    }
  } 

  private MineBlock blockFromTile(int totalSurrounding, List<Tile> adjacent) {
    Set<Tile> setTiles = new HashSet<>();
    for (Tile t: adjacent) {
      if (!t.hasBeenVisited()) {
        setTiles.add(t);
      }
    }
    return new MineBlock(setTiles, totalSurrounding);
  }

  private Move setFlag() {
    MovePossibility flag = null;
    if (!certainMine.isEmpty()) {
      flag = certainMine.get(0);
      certainMine.remove(0);
      return new FlagTile(flag.getXCoord(), flag.getYCoord());
    } else {
      return checkTile();
    }   
  }

  private Move checkTile() {
    MovePossibility likliest = null;
    if (!certainNotMine.isEmpty()) {
      likliest = certainNotMine.get(0);
      certainNotMine.remove(0);
    } else {
      double probability = 1;
      MovePossibility currentMove = null;
      for (MovePossibility m: uncertain) {
        if (m.getMineProbability() < probability) {
          currentMove = m;
          probability = m.getMineProbability();
        }
      }
      if (!(currentMove == null) && probability <= .5) {
        likliest = currentMove;
      } else {
        // Put random undiscovered move here
      }
    }
    return new CheckTile(likliest.getXCoord(), likliest.getYCoord());
  }
  
  private Move randomTile() {
    MovePossibility randomCheck = null;
    if (!uncertain.isEmpty()) {
      randomCheck = uncertain.get(0);
      uncertain.remove(0);
    } else {
      // Put random undiscovered move here
    }
    return new CheckTile(randomCheck.getXCoord(), randomCheck.getYCoord());
  }
  
  public void play() {
    while (canPlay) {
      try {
        Thread.sleep(moveTime);
        generateMovePossibilities();
        double moveChoice = Math.random();
        if (moveChoice < mistakeProbability) {
          makeMove(randomTile());
        } else if (moveChoice < mistakeProbability + FLAG_PROBABILITY) {
          makeMove(setFlag());
        } else {
          makeMove(checkTile());
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
  @Override
  /**
   * Sets canPlay to true, meaning that the Player can make Moves.
   */
  public void beginPlay() {
    canPlay = true;
    
  }

  @Override
  /**
   * Sets canPlay to false, meaning that the Player cannot make Moves.
   */
  public void endPlay() {
    canPlay = false;
  }
  
  public List<MovePossibility> getCertainMine() {
    return certainMine;
  }
  
  public List<MovePossibility> getCertainNotMine() {
    return certainNotMine;
  }
  
  public List<MovePossibility> getUncertain() {
    return uncertain;
  }
  
}
