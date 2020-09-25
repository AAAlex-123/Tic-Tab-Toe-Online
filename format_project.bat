rem create directories
mkdir src\ttt_online
mkdir bin
mkdir scripts
move *.java src\ttt_online

rem create .bat files
echo javac -d ..\bin  --release 14 ..\src\ttt_online\*.java > scripts\compile.bat
echo java -cp ..\bin ttt_online.GameEngine > scripts\run_client.bat
echo java -cp ..\bin ttt_online.GameServer > scripts\run_server_game.bat
echo java -cp ..\bin ttt_online.ChatServer > scripts\run_server_chat.bat

rem create TODO file
echo find the Java version by running `java -version` and replace that number in the scripts\compile.bat --release flag > TODO.txt

rem delete this file
rem del format_project.bat
