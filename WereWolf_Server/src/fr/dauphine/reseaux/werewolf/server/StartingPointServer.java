package fr.dauphine.reseaux.werewolf.server;
import java.io.*;

public class StartingPointServer {
	
	public static void main(String[] args) throws IOException, ClassNotFoundException{
		//Port No. = 5555 | message to all (@EE@|msg) | private msg (@username:msg) | Available clients (!)
		Server server = new Server(5555);
		server.waitingForClients();
	}
}
