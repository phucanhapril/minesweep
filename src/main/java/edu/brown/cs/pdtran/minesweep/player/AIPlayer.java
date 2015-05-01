package edu.brown.cs.pdtran.minesweep.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.brown.cs.pdtran.minesweep.board.Board;
import edu.brown.cs.pdtran.minesweep.games.BoardData;
import edu.brown.cs.pdtran.minesweep.tile.Tile;
import edu.brown.cs.pdtran.minesweep.types.AiDifficulty;
import edu.brown.cs.pdtran.minesweep.types.PlayerType;

/**
 * This class represents the AI that controls what moves the AI makes and
 * how it releases those moves.
 * @author Clayton Sanford
 */
public class AIPlayer extends GamePlayer {

  private List<MovePossibility> certainMine;
  private List<MovePossibility> certainNotMine;
  private List<MovePossibility> uncertain;
  private List<MovePossibility> usedUncertain;
  private List<MovePossibility> usedNotMine;
  private int difficulty;
  private int moveTime;
  private double mistakeProbability;
  private BoardData boardData;
  private int totalUncertain = 0;
  private int totalMine = 0;
  private int totalNotMine = 0;
  private static final double BASE_TIME = 5;
  private static final double TIME_MULTIPLIER = 25;
  private static final int MAX_DIFFICULTY = 10;
  private static final double MISTAKE_MULTIPLIER = .01;
  private static final double FLAG_PROBABILITY = .3;
  private static final double RANDOM_SUBTRACTOR = .5;
  private static final double CUTOFF_PROBABILITY = .5;
  private static final int EASY = 1;
  private static final int MEDIUM = 5;
  private static final int HARD = 9;


  /**
   * Creates an AIPlayer with a username and an AI difficulty. This version
   * will be used primarily for testing.
   * @param username A string unique to that player.
   * @param difficulty An integer from 1 to 10 with 10 being the most
   *        difficult.
   * @param data The BoardData object used by the AI to determine good
   *        moves to make.
   */
  public AIPlayer(String username, AiDifficulty difficultyEnum, BoardData data) {
    super(username);
    if (difficultyEnum == AiDifficulty.EASY) {
      difficulty = EASY;
    } else if (difficultyEnum == AiDifficulty.MEDIUM) {
      difficulty = MEDIUM;
    } else {
      difficulty = HARD;
    }
    this.boardData = data;
    // generateMovePossibilities();
    moveTime = (int) (BASE_TIME - difficulty * TIME_MULTIPLIER);
    mistakeProbability = (MAX_DIFFICULTY - difficulty) * MISTAKE_MULTIPLIER;

    usedUncertain = new ArrayList<>();
    usedNotMine = new ArrayList<>();
  }

  // TODO CHANGE JAVADOCS

  /**
   * Specifies how the AI thread makes moves in real time. The AI waits a
   * specified amount of time with a degree of randomness and then makes a
   * move. By random number, the AI will either touch a space without a
   * mine, place a flag, or make a mistake. The cycle repeats until the
   * game is over.
   */
  public Move getMove() {
    // while (canPlay) {
    // try {
    // int moveTimeRandomness =
    // (int) Math.round((Math.random() - RANDOM_SUBTRACTOR) * moveTime);

    // Thread.sleep(moveTime + moveTimeRandomness)
    generateMovePossibilities();
    double moveChoice = Math.random();
    if (moveChoice < mistakeProbability) {
      return randomTile();
    } else if (moveChoice < mistakeProbability + FLAG_PROBABILITY) {
      // return setFlag();
      return checkTile();
      // return randomTile();
    } else {
      return checkTile();
      // return randomTile();
    }

    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }
    // }
  }

  private void generateMovePossibilities() {
    certainMine = new ArrayList<>();
    certainNotMine = new ArrayList<>();
    uncertain = new ArrayList<>();
    List<MineBlock> blocks = new ArrayList<>();
    Board board = boardData.getCurrentBoard();
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
              blockFromTile(currentTile.getAdjacentBombs(), adjacentTiles);
          Boolean needsCheck = true;
          while (needsCheck) {
            needsCheck = false;
            MineBlock remove = null;
            for (MineBlock mb2 : blocks) {
              if (mb2.contains(mb1)) {
                mb2.subtract(mb1);
                needsCheck = true;
                if (mb2.getTiles().isEmpty()) {
                  remove = mb2;
                  break;
                }
              } else if (mb1.contains(mb2)) {
                mb1.subtract(mb2);
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
    for (MineBlock mb : blocks) {
      // System.out.println("Block:" + mb.getNumMines() + " / "
      // + mb.getTiles().size());
      double probability = (double) mb.getNumMines() / mb.getTiles().size();


      for (Tile adj : mb.getTiles()) {
        if (!adj.hasBeenVisited()) {
          MovePossibility mp = new MovePossibility(adj, probability);
          // System.out.println(mp.getXCoord() + ", " + mp.getYCoord() +
          // ": "
          // + mp.getMineProbability());
          if (probability == 0) {
            certainNotMine.add(mp);
          } else if (probability == 1) {
            certainMine.add(mp);
          } else {
            boolean contained = false;
            // for (int i = 0; i < uncertain.size(); i++) {
            // MovePossibility uncertainMp = uncertain.get(i);
            // if (uncertainMp.getXCoord() == mp.getXCoord()
            // && uncertainMp.getYCoord() == mp.getYCoord()) {
            // contained = true;
            // if (uncertainMp.getMineProbability() < probability) {
            // uncertain.remove(uncertainMp);
            // uncertain.add(mp);
            // }
            // }
            // }
            // if (!contained) {
            uncertain.add(mp);
            // }
          }
        }
      }
    }

    // for (int i = 0; i < totalUncertain; i++) {
    // uncertain.remove(0);
    // }
    // for (int i = 0; i < totalNotMine; i++) {
    // certainNotMine.remove(0);
    // }
    // for (int i = 0; i < totalMine; i++) {
    // certainMine.remove(0);
    // }
    for (MovePossibility m : usedUncertain) {
      uncertain.remove(m);
      System.out.println(uncertain.size());
    }
    for (MovePossibility m : usedNotMine) {
      certainNotMine.remove(m);
      System.out.println(certainNotMine.size());
    }
    System.out.print("Mines: ");
    for (MovePossibility m : certainMine) {
      System.out.print(m.getXCoord() + ", " + m.getYCoord() + ": "
          + m.getMineProbability() + "||");
    }
    System.out.print("\n");
    System.out.print("Not mines: ");
    for (MovePossibility m : certainNotMine) {
      System.out.print(m.getXCoord() + ", " + m.getYCoord() + ": "
          + m.getMineProbability() + "||");
    }
    System.out.print("\n");
    System.out.print("Uncertain: ");
    for (MovePossibility m : uncertain) {
      System.out.print(m.getXCoord() + ", " + m.getYCoord() + ": "
          + m.getMineProbability() + "||");
    }
    System.out.print("\n");
  }

  private MineBlock blockFromTile(int totalSurrounding, List<Tile> adjacent) {
    Set<Tile> setTiles = new HashSet<>();
    for (Tile t : adjacent) {
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
      totalNotMine++;
      // likliest = certainNotMine.get(0);
      // certainNotMine.remove(0);
      likliest = certainNotMine.get(certainNotMine.size() - 1);
      usedNotMine.add(likliest);
    } else {
      double probability = 1;
      MovePossibility currentMove = null;
      for (MovePossibility m : uncertain) {
        // System.out.println(m.getXCoord() + ", " + m.getYCoord() + ": "
        // + m.getMineProbability());
        if (m.getMineProbability() < probability
            && !m.getTile().hasBeenVisited()) {
          currentMove = m;
          probability = m.getMineProbability();
          System.out.println(m.getXCoord() + ", " + m.getYCoord() + ": "
              + m.getMineProbability());

        }
      }
      if (!(currentMove == null) && probability <= CUTOFF_PROBABILITY) {
        likliest = currentMove;
        totalUncertain++;
        usedUncertain.add(currentMove);
      } else {
        return randomTile();
      }
    }
    return new CheckTile(likliest.getXCoord(), likliest.getYCoord());
  }

  private Move randomTile() {
    totalUncertain++;
    MovePossibility randomCheck = null;
    if (!uncertain.isEmpty()) {
      randomCheck = uncertain.get(0);
      uncertain.remove(0);
    }
    usedUncertain.add(randomCheck);
    return new CheckTile(randomCheck.getXCoord(), randomCheck.getYCoord());
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

  /**
   * Gets the list of MovePossibilities that are certainly mines.
   * @return the list of MovePossibilities that are certainly mines
   */
  public List<MovePossibility> getCertainMine() {
    return certainMine;
  }

  /**
   * Gets the list of MovePossibilities that are certainly not mines.
   * @return the list of MovePossibilities that are certainly not mines
   */
  public List<MovePossibility> getCertainNotMine() {
    return certainNotMine;
  }

  /**
   * Gets the list of MovePossibilities that may or may not contain mines.
   * @return the list of MovePossibilities that may or may not contain
   *         mines.
   */
  public List<MovePossibility> getUncertain() {
    return uncertain;
  }

  @Override
  public PlayerType getType() {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Gets the difficulty of the AI.
   * @return An integer representing the AI's difficulty.
   */
  public int getDifficulty() {
    return difficulty;
  }

  /**
   * Gets whether the AI can play.
   * @return True if the AI can make moves.
   */
  public Boolean getCanPlay() {
    return canPlay;
  }

}
