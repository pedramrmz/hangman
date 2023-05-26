
module game {
    exports hangman;
}
//////////////////////
package hangman;

        import java.io.*;
        import java.util.Scanner;

        public class FileManager {

        private String address;

        public FileManager(String address) {
        this.address = address;
        }

        public String read() {
        StringBuilder data = new StringBuilder();
        try {
        File file = new File(address);

        Scanner myReader = new Scanner(file);
        while (myReader.hasNextLine()) {
        data.append(myReader.nextLine()).append("\n");
        }
        myReader.close();
        } catch (FileNotFoundException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
        }
        return data.toString();
        }

        public void save(String data) {
        try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(address));
        writer.write(data);
        writer.close();
        } catch (IOException e) {
        e.printStackTrace();
        }

        }
        }
//////////////////////////////////////
        package hangman;

        import java.util.ArrayList;
        import java.util.List;

        public class Game {

        private static char[] allowedChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', ' '};
        private static char hiddenChar = '-';

        private String word;
        private String currentWord;
        private List<Character> suggestedCharList;
        private GameState gameState;
        private boolean isHintUsed;


        public Game(String word) {
        this.word = word;
        this.suggestedCharList = new ArrayList<>();
        this.fillCurrentWord();
        this.gameState = GameState.inProgress;
        }

        public GameState getGameState() {
        return gameState;
        }

        public boolean suggestNewChar(Character suggest) {
        if (suggestedCharList.contains(suggest))
        return false;
        suggestedCharList.add(suggest);
        this.fillCurrentWord();
        if (currentWord.equals(word)) {
        this.gameState = GameState.win;
        } else if (getCurrentMistakeCount() >= getMaxMistakeCount())
        this.gameState = GameState.lost;
        return true;
        }

        public String getCurrentWord() {
        return currentWord;
        }

        public String getWord() {
        return word;
        }

        public int getCurrentMistakeCount() {
        List<Character> correctedCharList = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
        if (currentWord.charAt(i) == word.charAt(i))
        correctedCharList.add(word.charAt(i));
        }
        List<Character> wrongSuggestedList = new ArrayList<>();
        for (Character suggested : suggestedCharList) {
        if (!correctedCharList.contains(suggested))
        wrongSuggestedList.add(suggested);
        }
        return (word.length() > 9 ? (wrongSuggestedList.size() / 2) : wrongSuggestedList.size());
        }

        private int getMaxMistakeCount() {
        return word.length() > 9 ? 14 : 7;
        }

        private void fillCurrentWord() {
        currentWord = String.valueOf(word.toCharArray());
        for (char ch : allowedChars) {
        if (suggestedCharList.stream().noneMatch(character -> character.equals(ch)))
        currentWord = currentWord.replace(ch, hiddenChar);
        }
        }

        public boolean isHintUsed() {
        return isHintUsed;
        }

        public String getPreviousSuggestedText() {
        if (suggestedCharList.size() == 0)
        return "nothing is suggested";

        StringBuilder builder = new StringBuilder();
        builder.append("previous suggestion:");
        for (Character character : suggestedCharList) {
        builder.append(character);
        builder.append('|');
        }
        return builder.toString();
        }

        public void useHint() {
        List<Character> nominatedCharList = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
        if (currentWord.charAt(i) != word.charAt(i))
        nominatedCharList.add(word.charAt(i));
        }
        isHintUsed = true;
        suggestNewChar(nominatedCharList.get((int) (Math.random() * nominatedCharList.size())));
        }
        }
//////////////////////////////////////////////////////
        package hangman;

        import java.util.List;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

        public class GameManager {

        private Screen screen;
        private Game currentGame;
        private List<Player> allPlayers;
        private Player currentPlayer;
        private PlayerRepository playerRepository;
        private WordRepository wordRepository;

        public GameManager() {
        screen = new Screen();
        playerRepository = new PlayerRepository();
        wordRepository = new WordRepository();
        wordRepository.ReadFromFile();
        allPlayers = playerRepository.ReadFromFile();
        }

        public void start() {
        screen.clearScreen();
        currentPlayer = null;
        switch (screen.readInteger("1. sign up / 2. login")) {
        case 1://signUp
        this.signUp();
        break;

        case 2://login
        this.login();
        break;
        }
        this.startGame();

        }

        private void startGame() {
        switch (screen.readInteger("1. start game / 2. show leader board")) {
        case 1://start game
        currentGame = new Game(wordRepository.getRandomWord());
        while (currentGame.getGameState() == GameState.inProgress) {
        screen.clearScreen();
        screen.writeString("current word: " + currentGame.getCurrentWord());
        screen.writeString(currentGame.getPreviousSuggestedText());
        screen.writeHumanImage(currentGame.getCurrentMistakeCount());

        Character suggested = screen.readCharacter("suggest a char(for hit press ?):");
        if (suggested.equals('?')) {
        if (currentGame.isHintUsed())
        screen.writeString("hint already used.");
        else if (currentPlayer.getScore() < 10)
        screen.writeString("you don't have enough score to use hint.");
        else {
        currentGame.useHint();
        currentPlayer.setScore(currentPlayer.getScore() - 10);
        playerRepository.writeToFile(allPlayers);
        }
        } else {
        boolean accepted = currentGame.suggestNewChar(suggested);
        if (!accepted)
        screen.writeString("character is already suggested");
        }
        }
        screen.writeString("final answer: " + currentGame.getWord());
        if (currentGame.getGameState() == GameState.win) {
        screen.writeString("you won the game");
        currentPlayer.setScore(currentPlayer.getScore() + 5);
        playerRepository.writeToFile(allPlayers);
        screen.readToContinue();
        } else {
        screen.writeString("you lose the game");
        screen.readToContinue();
        }
        this.start();
        break;

        case 2://show leader board
        this.showLeaderBoard();
        this.startGame();
        break;
        }
        }

        private void showLeaderBoard() {
        allPlayers.sort((o1, o2) -> o2.getScore() - o1.getScore());
        for (Player player : allPlayers) {
        screen.writeString(padRightDash(player.getUsername()) + player.getScore());
        }
        }

        private String padRightDash(String inputString) {
        if (inputString.length() >= 30) {
        return inputString;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(inputString);
        while (sb.length() < 30) {
        sb.append('-');
        }
        return sb.toString();
        }

        private void login() {
        String username = screen.readString("username: ");
        String password = screen.readPassword("password: ");
        currentPlayer = getCurrentPlayer(username, password);
        if (currentPlayer == null) {
        screen.writeString("username and password not match, please sign up now");
        this.signUp();
        }
        }

        private void signUp() {
        String username, password;
        while (true) {
        username = screen.readString("new username: ");
        if (!isUsernameTaken(username))
        break;
        screen.writeString("username already taken, try another one");
        }
        while (true) {
        password = screen.readPassword("new password: ");
        if (isPasswordValid(password))
        break;
        screen.writeString("password is weak, try another one");
        }
        Player player = new Player(username, password, 0);
        allPlayers.add(player);
        currentPlayer = player;
        playerRepository.writeToFile(allPlayers);

        }

        private boolean isUsernameTaken(String username) {
        return allPlayers.stream().anyMatch(player -> player.getUsername().equals(username));
        }

        private boolean isPasswordValid(String password) {
        Pattern pattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$");
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
        }

        private Player getCurrentPlayer(String username, String password) {
        for (Player player : allPlayers) {
        if (player.getPassword().equals(password) && player.getUsername().equals(username))
        return player;
        }
        return null;
        }
        }
////////////////////////////////////////////////////////////////
        package hangman;

        public enum GameState {
        inProgress,
        win,
        lost
        }
////////////////////////////////////////////////////////////////////
        package hangman;

        public class Main {

        public static void main(String[] args) {
        new GameManager().start();
        }
        }
//////////////////////////////////////////////
        package hangman;

        public class Player {

        private String username;
        private String password;
        private int score;

        public Player(String username, String password, int score) {
        this.username = username;
        this.password = password;
        this.score = score;
        }

        public String getUsername() {
        return username;
        }

        public void setUsername(String username) {
        this.username = username;
        }

        public String getPassword() {
        return password;
        }

        public void setPassword(String password) {
        this.password = password;
        }

        public int getScore() {
        return score;
        }

        public void setScore(int score) {
        this.score = score;
        }
        }
/////////////////////////////////////////////////////////////////
        package hangman;

        import java.util.ArrayList;
        import java.util.List;

        public class PlayerRepository {


        private FileManager fileManager;

        public PlayerRepository() {
        fileManager = new FileManager("./data/players.txt");

        }

        public List<Player> ReadFromFile() {

        String data = fileManager.read();
        List<Player> result = new ArrayList<>();
        if (data.equals(""))
        return result;
        String[] dataArray = data.split("\n");

        for (String dt : dataArray) {
        String[] items = dt.split(",");
        result.add(new Player(items[0], items[1], Integer.valueOf(items[2])));
        }
        return result;
        }

        public void writeToFile(List<Player> allPlayers) {
        StringBuilder data = new StringBuilder();
        for (Player player : allPlayers) {
        data.append(player.getUsername());
        data.append(",");
        data.append(player.getPassword());
        data.append(",");
        data.append(player.getScore());
        data.append("\n");
        }
        fileManager.save(data.toString());
        }
        }
//////////////////////////////////////////////////////////
        package hangman;

        import java.io.Console;
        import java.io.IOException;
        import java.util.Scanner;

        public class Screen {


        public void clearScreen() {
        try {
        if (System.getProperty("os.name").contains("Windows"))
        new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        else
        Runtime.getRuntime().exec("clear");
        } catch (IOException | InterruptedException ex) {
        ex.printStackTrace();
        }
        }

        public int readInteger(String str) {
        System.out.println(str);
        return new Scanner(System.in).nextInt();
        }

        public Character readCharacter(String str) {
        System.out.print(str);
        return new Scanner(System.in).next().charAt(0);
        }

        public String readString(String str) {
        System.out.println(str);
        return new Scanner(System.in).next();
        }

        public String readToContinue() {
        System.out.println("press any key to continue");
        return new Scanner(System.in).nextLine();
        }

        public String readPassword(String str) {
        Console console = System.console();
        if (console == null) {
        return readString(str);
        }
        char[] passwordArray = console.readPassword(str);
        return String.valueOf(passwordArray);
        }

        public void writeString(String str) {
        System.out.println(str);
        }

        public void writeHumanImage(int humanState) {


        switch (humanState) {
        case 0:
        System.out.println("----");
        System.out.println("|");
        System.out.println("|");
        System.out.println("|");
        System.out.println("|");
        System.out.println("|");
        break;
        case 1:
        System.out.println("----");
        System.out.println("|  |");
        System.out.println("|");
        System.out.println("|");
        System.out.println("|");
        System.out.println("|");
        break;
        case 2:
        System.out.println("----");
        System.out.println("|  |");
        System.out.println("|  O");
        System.out.println("|");
        System.out.println("|");
        System.out.println("|");
        break;
        case 3:
        System.out.println("----");
        System.out.println("|  |");
        System.out.println("|  O");
        System.out.println("| /");
        System.out.println("|");
        System.out.println("|");
        break;
        case 4:
        System.out.println("----");
        System.out.println("|  |");
        System.out.println("|  O");
        System.out.println("| /|");
        System.out.println("|");
        System.out.println("|");
        break;
        case 5:
        System.out.println("----");
        System.out.println("|  |");
        System.out.println("|  O");
        System.out.println("| /|\\");
        System.out.println("|");
        System.out.println("|");
        break;
        case 6:
        System.out.println("----");
        System.out.println("|  |");
        System.out.println("|  O");
        System.out.println("| /|\\");
        System.out.println("| / ");
        System.out.println("|");
        break;
        case 7:
        System.out.println("----");
        System.out.println("|  |");
        System.out.println("|  O");
        System.out.println("| /|\\");
        System.out.println("| / \\");
        System.out.println("|");
        break;
        }
        }
        }
///////////////////////////////////////////////////////////
        package hangman;

        public class WordRepository {

        private String[] words;

        private FileManager fileManager;

        public WordRepository() {
        fileManager = new FileManager("./data/words.txt");

        }

        public void ReadFromFile(){
        String data = fileManager.read();
        words = data.split("\n");
        }

        public String getRandomWord() {
        return words[(int) (Math.random() * words.length)];
        }
        }

