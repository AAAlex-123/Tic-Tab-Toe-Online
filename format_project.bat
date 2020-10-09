rem create directories
mkdir src\ttt_online
mkdir bin
mkdir scripts
mkdir executables

rem move files around
move *.java src\ttt_online
move *.exe executables
move *.jar executables

rem create .bat files
echo javac -d ..\bin  --release 8 ..\src\ttt_online\*.java > scripts\compile.bat
echo java -cp ..\bin ttt_online.GameEngine > scripts\run_client.bat
echo java -cp ..\bin ttt_online.GameServer > scripts\run_server_game.bat
echo java -cp ..\bin ttt_online.ChatServer > scripts\run_server_chat.bat

rem create TODO file
echo Nothing left to do. You're set! > TODO.txt

rem delete this file
rem del format_project.bat
