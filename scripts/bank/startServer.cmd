set script_folder=%~dp0

start %script_folder%runRMIregistry.cmd

java -cp _build ru.ifmo.rain.varfolomeev.bank.server.Server