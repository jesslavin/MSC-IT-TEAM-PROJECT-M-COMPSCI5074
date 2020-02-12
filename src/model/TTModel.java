package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

public class TTModel {
	private int playerCount; // model attributes
	private int activePlayerNum;
	private Player activePlayer;
	private String gameWinner;
	private Player roundWinner;
	private int numOfDraws;
	private int numOfRounds;
	private int numOfGames;
	//private boolean writeGameLogsToFile; // passed by TopTrumpsCLIApplication to constructor - controller currently handling this
	private boolean isDraw;
	private Deck deck;
	private ArrayList<Integer> allWonRounds;
	private ArrayList<Player> players;
	private ArrayList<Player> playersToRemove;
	private ArrayList<Player> roundWinners;
	private ArrayList<Card> communalPile;
	private ArrayList<Card> playingTable;
	private ArrayList<Card> winnersCards;
	private int categoryChosen; //instance variable set by argument supplied to compareCards, needed for use in TestLogger
	private HashMap<Player, Integer> playerStats = new HashMap<Player, Integer>(); 	//migrated from compareCards so as to give an instance
    private LogWriter logWriter;																				//variable, which can then be called as a getter

    public TTModel() { // constructor												//for use in TestLogger
		this.setNewGameStates();
	}

	public void setNewGameStates() { // method to initialise and reset all variables at start of new game
		this.deck = new Deck();
		this.players = new ArrayList<Player>();
		this.playersToRemove = new ArrayList<Player>();
		this.roundWinners = new ArrayList<Player>();
		this.communalPile = new ArrayList<Card>();
		this.playingTable = new ArrayList<Card>();
		this.winnersCards = new ArrayList<Card>();
		this.allWonRounds = new ArrayList<Integer>();
		this.playerStats = new HashMap<Player, Integer>();
		//this.logWriter = new LogWriter();
		this.numOfDraws = 0;
		this.numOfRounds = 1; //changed from 0
		this.numOfGames = 0;
		this.isDraw = false;
		this.gameWinner = null;
		this.activePlayer = null;
		this.roundWinner = null;
	}

	public void startGame(int botCount) { // method is called to generate a Player and Bot objects based on the botCount
											// parameter passed from the controller
		System.err.println("botCount= " + botCount); //ONLINE TEST
		this.playerCount = botCount + 1;
		System.err.println("playerCount= " + playerCount); //ONLINE TEST
		this.players.add(new Player("Player1"));

		for (int i = 0; i < botCount; i++) {
			this.players.add(new Bot("Player" + (i + 2) + " (AI)"));
		}
		this.deck.loadDeck(); // calls method in deck object to generate card objects
        this.logWriter = new LogWriter(this.deck);
		//this.logWriter.setDeckOnLoad(this.deck.getCards());
		this.deck.shuffleDeck(); //calls method to shuffle deck
		this.logWriter.setDeckShuffle(this.deck.getCards());
        this.deck.dealCards(this.playerCount, this.players); // calls method to deal cards amongst
		System.err.println("PLAYER SIZE  STARTGAME" + this.players.size()); //ONLINE TEST
        this.logWriter.setPlayersHands(this.players, this.numOfRounds);
	}

	public void startBotGame(int botCount) { // method is called when a bot vs bot game is required, no player objects
												// are instantiated
		this.playerCount = botCount;

		for (int i = 0; i < botCount; i++) {
			this.players.add(new Bot("Player" + (i + 1) + " (AI)"));
		}
		this.deck.loadDeck(); // calls method to read and create card objects
		this.deck.shuffleDeck(); //calls method to shuffle deck
		this.deck.dealCards(this.playerCount, this.players);
		this.logWriter.setPlayersHands(this.players, this.numOfRounds); // added here as TestLogger needs to see hands on initial deal, before play, otherwise null.
	}

	public void selectPlayer() {
        if (this.numOfRounds == 0) {
            this.numOfRounds++;
        }
		if (this.numOfRounds == 1) { // If it is the first round //changed from 0
			Random r = new Random();
			System.err.println("player count select player= " + this.playerCount); //ONLINE TEST
			this.activePlayerNum = r.nextInt(this.playerCount); // select a random player to start first
			System.err.println("ACTIVE PLAYER NUM   " + this.activePlayerNum); //ONLINE TEST
			System.err.println("PLAYER SIZE  SELECT PLAYER" + this.players.size()); //ONLINE TEST
			this.activePlayer = this.players.get(this.activePlayerNum);
			System.err.println("active player is " + this.activePlayer); //ONLINE TEST
		} else if (this.isDraw == true || this.activePlayer.equals(this.roundWinner)) { // if it is a draw from the
																						// previous round or the
																						// previous rounds winner has
																						// won again
			; // do nothing
		} else { // if a different player has won compared to the previous round
			this.activePlayer = this.roundWinner; // set the starting player to be the winner of the last round
		}

	}

	public void playCards(int stat) {
		//this.logWriter.setPlayersHands(this.players, this.numOfRounds);
		this.logWriter.setChosenCategory(stat);
		this.playerStats.clear(); // clears the instance variable at the beginning of each comparison, prior to
		this.logWriter.resetEveryoneValues();						// adding new stats as before
		for (Player p : this.players) {
			this.playerStats.put(p, p.getTopCard().getStats().get(stat));
			this.logWriter.setEveryoneValues(p.getName() + " has the card value: " + p.getTopCard().getStats().get(stat));
			this.playingTable.add(p.getHand().remove(p.getTopCardIndex())); // remove all the players top cards and add
																			// the to an array list
		}
		this.logWriter.setPlayingTable(this.playingTable, this.getDeck().getHeaderNames());
		int maxValueInMap = Collections.max(playerStats.values()); // calculate the highest value from the cards
																	// presented by the players
		for (Entry<Player, Integer> entry : playerStats.entrySet()) { // Iterate through hashmap value to find what
																		// value matches the maxValueInMap, and get the
																		// key of that entry
			if (entry.getValue() == maxValueInMap) {
				this.roundWinners.add(entry.getKey()); // adds all the players with the highest value to a new array
														// list
			}
		}
	}

	public void selectWinners() {
		if (this.roundWinners.size() < 2) { // if there is only one winner
			this.roundWinners.get(0).incrementRoundsWon();
			ArrayList<Card> winnersCards = new ArrayList<Card>();
			this.roundWinner = this.roundWinners.get(0); // set the round winning variable to the winning players name
			this.logWriter.setRoundWinner(this.roundWinner.getName());
			this.isDraw = false; // not a draw
			this.winnersCards.addAll(this.communalPile);
			this.winnersCards.addAll(this.playingTable);
			Collections.shuffle(winnersCards); // randomise return cards
			this.roundWinners.get(0).getHand().addAll(0, this.winnersCards); // add all the cards from the communal pile and
																		// playing table to back of winning player's
																		// hand in a random order
			this.logWriter.setPlayersHands(this.players, this.numOfRounds); //moved here as testlogger needs to see this at the end of a game loop.
			this.playingTable.clear(); // clear all array lists for new round
			this.communalPile.clear();
			this.logWriter.setCommunalPile(communalPile);
			this.winnersCards.clear();
		} else { // if there are more than two winners (draw)
			this.numOfDraws++;
			this.isDraw = true;
			this.roundWinner = null;
			this.logWriter.setRoundWinner(null); //added due to bug where LogWriter never learnt of draws.
			this.communalPile.addAll(this.playingTable); // add all cards to communal pile array list
			this.logWriter.setCommunalPile(this.communalPile);
			this.playingTable.clear();
			this.logWriter.setPlayersHands(this.players, this.numOfRounds);////moved here as testlogger needs to see this at the end of a game loop.
		}
		this.numOfRounds++;  //moved from selectPlayer
		this.roundWinners.clear();
	}

	public boolean hasWon() { // checker method called at the end of every round
		for (Player p : this.players) {
			if ((p.getHand().size() + this.communalPile.size()) >= this.deck.getNumOfCards()) { // does any of the
																								// playershave all the cards?
				this.gameWinner = p.getName(); // won the game
				this.numOfGames++; // increment number of games
				updateWonRounds();
				return true;
			} else if (p.getHand().isEmpty()) {
				this.playersToRemove.add(p);
			}
		}
		players.removeAll(playersToRemove);
		playersToRemove.clear();
		return false;
	}

	public void updateWonRounds() { // method updates variable to count all the rounds that have been won by all
									// players
		for (Player p : this.players) {
			int eachWonRounds = p.getRoundsWon();
			this.allWonRounds.add(eachWonRounds);
		}
	}

	// Getter and Setter Methods
	public ArrayList<Player> getPlayers() {
		return this.players;
	}

	public Player getActivePlayer() {
		System.err.println("the active player is gettter" + this.activePlayer); //ONLINE TEST
		return this.activePlayer;
	}

	public int getNumOfDraws() {
		return this.numOfDraws;
	}

	public String getGameWinner() {
		return this.gameWinner;
	}

	public String getRoundWinnerName() {
		return this.roundWinner.getName();
	}

	public int getNumOfGames() {
		return this.numOfGames;
	}

	public int getNumOfRounds() {
		return this.numOfRounds;
	}

	public ArrayList<Integer> getAllWonRounds() {
		return this.allWonRounds;
	}

	// new methods needed for use when logging.
	public Deck getDeck() {
		return this.deck;
	}

    public LogWriter getlogWriter() {
        return logWriter;
    }

	public ArrayList<Card> getCommunalPile()
	{
		return this.communalPile;
	}

	public ArrayList<Card> getPlayingTable()
	{
		return this.playingTable;
	}
	
	public HashMap<Player, Integer> getPlayerStats()
	{
		return this.playerStats;
	}

	public int getCategoryChosen() {
		return this.categoryChosen;
	}
	

}