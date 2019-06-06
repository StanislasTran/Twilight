package fr.dauphine.reseaux.werewolf.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.dauphine.reseaux.werewolf.server.gameObjects.Role;
import fr.dauphine.reseaux.werewolf.server.gameObjects.Status;

/**
 * 
 * Salle de jeu, contient toutes les informations du jeu. Chaque room est
 * indépendante des autres, aucune variable n'est en commun
 *
 */
public class Room {

	// GENERAL ROOM

	private String name;
	private String host;
	private int maxSize;

	// DONNEES DU JEU

	/**
	 * Statut du jeu
	 */
	private Status status;

	private Set<String> users;

	/**
	 * Role per player in this room
	 */
	private Map<String, Role> roleMap = new HashMap<>();
	/**
	 * Vote per player in this room
	 */
	private Map<String, String> voteMap = new HashMap<>();
	/**
	 * Role of the player playing (wolf, seer, witch, ...)
	 */
	private Role roleTurn;

	private Boolean playerSaved = false;

	/**
	 * not used
	 */
	private Boolean seerPower = false; // seer = voyante ; // gerer si la voyante a utilisï¿½ son pouvoir
	/**
	 * not used
	 */
	private Boolean witchPower = false; // witch = sorciere ; // gerer si la sorciï¿½re a utilisï¿½ son pouvoir

	private Boolean witchSavePower = true;
	private Boolean witchKillPower = true;
	/**
	 * The player the witch want to kill
	 */
	private String playerWitchToKill = "";

	private ArrayList<String> playersAlive = new ArrayList<>();
	private LinkedList<String> playersDead = new LinkedList<>();

	// CONSTRUCTEUR

	public Room(String name, String user, int maxSize) {
		if (maxSize < 2) {
			throw new IllegalArgumentException("not enough player");
		}
		this.maxSize = maxSize;
		this.name = Objects.requireNonNull(name);
		this.users = new HashSet<>();
		this.users.add(Objects.requireNonNull(user));
		this.host = user;
		this.status = Status.WAITING;

	}

	/*
	 * 
	 * PARTIE 1 | METHODES PRINCIPALES
	 * 
	 */

	/**
	 * Envoie la liste des joueurs connectés dans la room, avec '!' comme en-tête
	 */
	public String userKey() {
		return "!" + users;
	}

	public String addUsers(String user) {
		System.out.println("on entre dans le add");
		if (users.size() == maxSize) {

			return "FULL";
		}
		this.users.add(user);
		System.out.println(user + "added success");
		return "UserAdded";

	}

	/*
	 * 
	 * PARTIE 2 | GETTERS / SETTERS
	 * 
	 */

	public String getName() {
		return this.name;
	}

	public int size() {
		return this.users.size();
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	public Set<String> getUsers() {
		return this.users;
	}

	public Map<String, Role> getRoleMap() {
		return roleMap;
	}

	public void setRoleMap(Map<String, Role> roleMap) {
		this.roleMap = roleMap;
	}

	public Map<String, String> getVoteMap() {
		return voteMap;
	}

	public void setVoteMap(Map<String, String> voteMap) {
		this.voteMap = voteMap;
	}

	public Role getRoleTurn() {
		return roleTurn;
	}

	public void setRoleTurn(Role roleTurn) {
		this.roleTurn = roleTurn;
	}

	public Boolean getPlayerSaved() {
		return playerSaved;
	}

	public void setPlayerSaved(Boolean playerSaved) {
		this.playerSaved = playerSaved;
	}

	public Boolean getSeerPower() {
		return seerPower;
	}

	public void setSeerPower(Boolean seerPower) {
		this.seerPower = seerPower;
	}

	public Boolean getWitchPower() {
		return witchPower;
	}

	public void setWitchPower(Boolean witchPower) {
		this.witchPower = witchPower;
	}

	public Boolean getWitchSavePower() {
		return witchSavePower;
	}

	public void setWitchSavePower(Boolean witchSavePower) {
		this.witchSavePower = witchSavePower;
	}

	public Boolean getWitchKillPower() {
		return witchKillPower;
	}

	public void setWitchKillPower(Boolean witchKillPower) {
		this.witchKillPower = witchKillPower;
	}

	public String getPlayerWitchToKill() {
		return playerWitchToKill;
	}

	public void setPlayerWitchToKill(String playerWitchToKill) {
		this.playerWitchToKill = playerWitchToKill;
	}

	public ArrayList<String> getPlayersAlive() {
		return playersAlive;
	}

	public void setPlayersAlive(ArrayList<String> playersAlive) {
		this.playersAlive = playersAlive;
	}

	public LinkedList<String> getPlayersDead() {
		return playersDead;
	}

	public void setPlayersDead(LinkedList<String> playersDead) {
		this.playersDead = playersDead;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setUsers(Set<String> users) {
		this.users = users;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Status getStatus() {
		return this.status;
	}

}
