set script_folder=%~dp0

start %script_folder%startServer.cmd

sleep 5

java -cp _build ru.ifmo.rain.varfolomeev.bank.client.Client %*