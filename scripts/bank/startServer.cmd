set script_folder=%~dp0

call %script_folder%build.cmd

java -cp _build ru.ifmo.rain.varfolomeev.bank.server.Server
