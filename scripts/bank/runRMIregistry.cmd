call %script_folder%build.cmd
rmiregistry -J-Djava.rmi.server.codebase=file:%cd%\_build\
