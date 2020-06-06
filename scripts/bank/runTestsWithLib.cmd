set script_folder=%~dp0

call %script_folder%build.cmd

java  -cp _build;%lib_folder%\*.jar org.junit.runner.JUnitCore ru.ifmo.rain.varfolomeev.bank.tests.BankTests

start %script_folder%delete_build.cmd

exit %errorlevel%