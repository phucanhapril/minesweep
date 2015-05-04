package edu.brown.cs.pdtran.minesweep.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.brown.cs.pdtran.minesweep.board.Board;
import edu.brown.cs.pdtran.minesweep.games.BoardData;
import edu.brown.cs.pdtran.minesweep.metagame.Team;
import edu.brown.cs.pdtran.minesweep.move.Move;
import edu.brown.cs.pdtran.minesweep.setup.Gamer;
import edu.brown.cs.pdtran.minesweep.setup.TeamFormation;
import edu.brown.cs.pdtran.minesweep.tile.Tile;
import edu.brown.cs.pdtran.minesweep.types.MoveResponse;

/**
 * Represents a Team of Players that works together in a game of Minesweep.
 * @author Clayton
 */
public class PlayerTeam extends Team implements BoardData {

  private ConcurrentMap<String, GamePlayer> players;
  private int score;
  private int lives;
  private Boolean isWinner;
  private Boolean isLoser;
  private List<Board> boards;
  private int boardIndex;
  private List<AIPlayer> aiPlayers;
  private List<String> humanPlayers;
  private List<Tile> flaggedTiles;

  /**
   * Creates a new Team to last through a game.
   * @param tf A TeamFormation object that contains the information
   *        necessary to create a team.
   * @param lives The total number of lives for the team and its players.
   * @param boards A List of Boards that the team may use.
   */
  public PlayerTeam(TeamFormation tf, int lives, List<Board> boards) {
    super(tf.getName());
    this.lives = lives;
    this.boards = boards;
    boardIndex = 0;
    players = new ConcurrentHashMap<String, GamePlayer>();
    aiPlayers = new ArrayList<AIPlayer>();
    humanPlayers = tf.getHumans();
    for (Map.Entry<String, Gamer> entry : tf.getPlayers().entrySet()) {
      players.put(entry.getKey(), entry.getValue()
          .toGamePlayer(this, aiPlayers));
    }

    flaggedTiles = new ArrayList<>();

    score = 0;
    isWinner = false;
    isLoser = false;
  }



  @Override
  public Board getCurrentBoard() {
    return boards.get(boardIndex);
  }

  public Boolean nextBoard() {
    boardIndex++;
    return boardIndex < boards.size();
  }

  /**
   * Searches for the id for a player and removes that player from the team
   * and from play.
   * @param id A unique string for a player on the team.
   */
  public void removePlayer(String id) {
    players.remove(id);
  }

  /**
   * Gets the team's score.
   * @return An integer representing the total score of all the players on
   *         a team.
   */
  public int getScore() {
    return score;
  }

  /**
   * Updates the team's score by iterating through all the players and
   * adding their individual scores.
   */
  public void updateScore() {
    int newScore = 0;
    for (GamePlayer p : players.values()) {
      newScore += p.getScore();
    }
    score = newScore;
  }

  /**
   * Subtracts one life from the team when they land on a mine.
   */
  public void loseLife() {
    lives--;
    if (lives == 0) {
      setIsLoser();
    }
  }

  /**
   * Gets the number of lives remaining.
   * @return An int representing the number of lives left.
   */
  public int getLives() {
    return lives;
  }

  /**
   * Makes the team into the winning team and ends gameplay for all of the
   * its Players.
   */
  public void setIsWinner() {
    isWinner = true;
    for (GamePlayer p : players.values()) {
      p.endPlay();
    }
  }

  /**
   * Checks if the team is the winning team.
   * @return A Boolean that is true if the team is the winner and false if
   *         the team is the loser or if the game is not over.
   */
  public Boolean getIsWinner() {
    return isWinner;
  }

  /**
   * Makes the team into the winning team and ends gameplay for all of the
   * its Players.
   */
  public void setIsLoser() {
    isLoser = true;
    for (GamePlayer p : players.values()) {
      p.endPlay();
    }
  }

  /**
   * Checks if the team is the losing team.
   * @return A Boolean that is true if the team is the loser and false if
   *         the team is the winner or if the game is not over.
   */
  public Boolean getIsLoser() {
    return isLoser;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, GamePlayer> getPlayers() {
    return players;
  }

  @Override
  public List<String> getHumans() {
    return humanPlayers;
  }

  @Override
  public List<AIPlayer> getAis() {
    return aiPlayers;
  }

  public JsonElement getBoardInfo() {
    JsonObject teamData = new JsonObject();
    teamData.add("board", getCurrentBoard().toJson());

    JsonArray flags = new JsonArray();
    for (Tile flagged : flaggedTiles) {
      JsonObject flag = new JsonObject();
      flag.addProperty("y", flagged.getRow());
      flag.addProperty("x", flagged.getColumn());
      flags.add(flag);
    }
    teamData.add("flags", flags);

    return teamData;
  }

  public MoveResponse makeMove(Move m) {
    Board currentBoard = getCurrentBoard();
    switch (m.getMoveType()) {
      case CHECK:
        return currentBoard.makeMove(m.getXCoord(), m.getYCoord());
      case FLAG:
        if (currentBoard.isWithinBoard(m.getXCoord(), m.getYCoord())) {
          Tile tile = currentBoard.getTile(m.getXCoord(), m.getYCoord());
          if (!tile.hasBeenVisited()) {
            if (flaggedTiles.contains(tile)) {
              flaggedTiles.remove(tile);
            } else {
              flaggedTiles.add(tile);
            }
            return MoveResponse.FLAG;
          }
        }
        return MoveResponse.INVALID;
      default:
        return MoveResponse.INVALID;
    }
  }

  public List<Tile> getFlaggedTiles() {
    return flaggedTiles;
  }
}
