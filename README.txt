A 5x5 Tic Tac Toe game playable on any local network connection. Features a custom UI (using the standard Java swing library) and a variety of avatars and colors players can use in-game. The Java Runtime Environment is required to run the game (it's likely it's already installed on your computer).

To run download the zip file, unpack it and run the following commands in your command prompt:
```cd [name of the directory the game folder is in]```
```javac GameEngine.java GameBoard.java Server.java GameUI.java```
```java ttt_online.Server ``` 
This will use the computer as a local server in order to communicate with the other players.
Append the number of players you expect to play on the server on your command. You can also append '1' if you don't want to ignore information if the game crashes.
Eg ```java ttt_online.Server 3 1``` == "I want a server for 3 players and I want you to show me any errors that are caused"


To play the game use the following command instead:
```java GameEngine.java``` or  ```java GameEngine```
Use this as many times as the server expects players, from any computer in the local network. 

