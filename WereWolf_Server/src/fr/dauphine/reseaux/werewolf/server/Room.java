package fr.dauphine.reseaux.werewolf.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

///!!!!\\\ gérer la déconnexion d'un user dans une room
public class Room {
	private String name;
	private Set<String> users;
	private String host;
	private int maxSize;

	public Room(String name, String user, int maxSize) {
		if (maxSize < 2) {
			throw new IllegalArgumentException("not enough player");
		}
		this.maxSize = maxSize;
		this.name = Objects.requireNonNull(name);
		this.users = new HashSet<>();
		this.users.add(Objects.requireNonNull(user));
		this.host=user;

	}

	public String getName() {
		return this.name;
	}


	public int size() {
		return this.users.size();
	}

	public String userKey() {
		return "!" + users;
	}

	public String addUsers(String user) {
		if (users.size() < maxSize) {
			return "FULL";
		}
		this.users.add(user);
		return "UserAdded";

	}
	

}
