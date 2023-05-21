import java.net.Socket;
import java.util.List;
import java.util.concurrent.Future;

public class OngoingGames {
  private static Game game;
  private static List<User> players;
  private static Future<Socket> gameResult;
  private static Boolean ranked;

  public OngoingGames(Game game, List<User> players, Future<Socket> gameResult, Boolean ranked) {
    this.game = game;
    this.players = players;
    this.gameResult = gameResult;
    this.ranked = ranked;
  }

  public static Game getGame() {
    return game;
  }

  public static List<User> getPlayers() {
    return players;
  }

  public static Future<Socket> getGameResult() {
    return gameResult;
  }

  public static Boolean getRanked() {
    return ranked;
  }
}
