set script_folder=%~dp0

call %script_folder%build.cmd

java  -cp _build;%lib_folder%\junit-4.11.jar;%lib_folder%\hamcrest-core-1.3.jar org.junit.runner.JUnitCore ru.ifmo.rain.varfolomeev.bank.BankTests

start %script_folder%delete_build.cmd

exit %errorlevel%