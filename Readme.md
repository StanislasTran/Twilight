#Jeu du loup-garou

##Features
* Unlimited players (number of werewolves 25% of the player number minimum)
* Implemented roles: Villager, Werewolf, Witch

##How to run
1. Import **WereWolf_Client** and **WereWolf_Server** Projects in Eclipse.
2. Firstly run the **StartingPointServer.java** in src folder of WereWolf_Server Project.
3. Make sure the Server GUI is running and it is displaying "Waiting for clients at ServerSocket[addr=0.0.0.0/0.0.0.0,port=0,localport=5555]".
4. Now run the **StartingPointClient.java** in src folder of WereWolf_Client Project.
5. Enter the username in the dialog box and press Enter. Client is connected to the server.
6. For multiple clients just do 4th step.

##How to run tests
1. In Eclipse, go to **Client.java** and modify **TEST** variable to **true**.
2. Use **run.bat** to start multiple Clients, modify the batch depending the number of clients you need.

##How to play in LAN on different computers
1. In Eclipse, go to **Client.java** and modify **localhost** variable to **false**.
2. In Eclipse, go to **Client.java** and modify **ipServer** variable to your iPv4 or iPv6 (CMD -> ipconfig).
3. Launch Server, then Client on different computers, connected on the same Internet Network.
4. To play on different Internet Network, please use Hamachi to simulate a LAN (VPN).

##Commands

* '/createRoom NAME_ROOM NUMBER_OF_PLAYERS_MAX' to create a new room.
* '/join NAME_ROOM' to join a game room.

* '/start' to start the game.
* '/vote NAME' for werewolves to vote during the night , and for villagers to vote during the day.
* '/witch_save yes/no' for witch to save the killed villager.
* '/witch_kill NAME' for witch to kill a player.

[NEW FEATURE] Double click on one of the player name to put these commands on the textbox automatically (following the status of the game)


##Team Members

*Louis FONTAINE*  
*Stanislas TRAN*    
*Victor CHEN*

