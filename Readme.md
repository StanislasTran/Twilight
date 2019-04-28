#ChatRoom (6 Weeks Core Java Project)

##Features
* Group Chat
* Private Chat
* Theme Changer Client GUI

##How to run
1. Import **ChatRoomFinalServer** and **ChatRoomFinalClient** Projects in Eclipse.
2. Firstly run the **StartingPointServer.java** in src folder of ChatRoomFinalServer Project.
3. Make sure the Server GUI is running and it is displaying "Waiting for clients at ServerSocket[addr=0.0.0.0/0.0.0.0,port=0,localport=5555]".
4. Now run the **StartingPointClient.java** in src folder of ChatRoomFinalClient Project.
5. Enter the username in the dialog box and press Enter. Client is connected to the server.
6. For multiple clients just do 4th step.

##Description
**Server GUI** : Initially Server GUI is showing the message "Waiting for clients at ServerSocket[addr=0.0.0.0/0.0.0.0,port=0,localport=5555]" and as soon as first client is connected, his username along with his IP is shown as online (since we're using it on localhost hence in our case all clients IP will be (127.0.0.1) same). And whenever Client left the ChatRoom by closing his Client GUI, server GUI will show him offline with his username along with IP.

**Client GUI** : Firstly there is a dialog box prompting for the username. After you entered your username main Client GUI get connected with the Server showing Online Status. Client GUI consists of GUI Theme Changer(Upper Right Hand Corner), Clients Online, Chat History. Message written in the message input area is send to all clients and shown in the chat history along with the sender's client name. In order to do Private Chat, double click on the Client to whom you want to do private chat.   

>**NOTE:** Since no database is used in Server Side. Hence whenever we restart the app, no previous chat is show on the Client Side.

##Team Members
*Laveena*  
*Poonam Kaundal*  
*Rahul Jain*  
*Reet Awwsum*

##Platform

Coded in Linux, Mac OSX, Windows 7  
Compiled in Mac OSX  
Checked on Linux, Mac OSX, Windows 7  

>**NOTE:** Currently working on localhost (127.0.0.1)
