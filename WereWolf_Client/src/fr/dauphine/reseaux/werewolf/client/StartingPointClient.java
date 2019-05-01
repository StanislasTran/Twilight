package fr.dauphine.reseaux.werewolf.client;
import java.io.*;
import java.net.*;

public class StartingPointClient {
	public static void main(String[] args) throws UnknownHostException, IOException{
			Client.BuildMainWindow();
			Client.Initialize();
			Client.BuildLogInWindow();
	}
}
