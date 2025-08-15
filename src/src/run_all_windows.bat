@echo off
REM Script para rodar LeaderServer, dois WorkerNode e ClientApp no Windows

REM Caminho onde estão os .class
set OUT_DIR=out\production\src

REM Configurações
set MCAST_GROUP=230.0.0.0
set MCAST_PORT=4446
set AUTH_PORT=7000
set NODE1=1,localhost,1099,5000
set NODE2=2,localhost,2099,6001
set ALL_NODES=%NODE1%;%NODE2%
set USER=user123
set PASS=pass123

REM Iniciar LeaderServer
echo Iniciando LeaderServer...
start "LeaderServer" cmd /k java -cp %OUT_DIR% leader.LeaderServer %MCAST_GROUP% %MCAST_PORT% %AUTH_PORT% 1 --nodes "%ALL_NODES%"
timeout /t 2 >nul

REM Iniciar WorkerNode 1
echo Iniciando WorkerNode 1...
start "WorkerNode1" cmd /k java -cp %OUT_DIR% node.WorkerNode 1 localhost 1099 5000 6000 "%ALL_NODES%"
timeout /t 1 >nul

REM Iniciar WorkerNode 2
echo Iniciando WorkerNode 2...
start "WorkerNode2" cmd /k java -cp %OUT_DIR% node.WorkerNode 2 localhost 2099 6001 7001 "%ALL_NODES%"
timeout /t 1 >nul

REM Iniciar ClientApp
echo Iniciando ClientApp...
start "ClientApp" cmd /k java -cp %OUT_DIR% client.ClientApp %MCAST_GROUP% %MCAST_PORT% localhost %AUTH_PORT% %USER% %PASS%

echo Todos os processos foram iniciados em janelas separadas.
pause