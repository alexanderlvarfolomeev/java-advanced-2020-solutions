set script_folder=%~dp0

call %script_folder%build.cmd

call %script_folder%runRMIregistry.cmd

java -cp _build ru.ifmo.rain.varfolomeev.bank.server.Server