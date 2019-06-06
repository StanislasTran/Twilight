#Jeu du loup-garou

##Features
* Nombre de joueur illimité (nombre de loup-garous en fonction du nombre de joueurs, 25% minimum)
* Roles implementés: Villageois, Loup-Garou, Sorcière

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

##Commands

* '/start' to start the game
* '/vote NOM' to vote during the night for werewolves

##Team Members

*Louis FONTAINE*  
*Stanislas TRAN*    
*Victor CHEN*

