set script_folder=%~dp0
set src_folder=%script_folder%..\..\..\..\..
set lib_folder=%src_folder%\..\..\java-advanced-2020\lib

javac -d _build -cp %lib_folder%\junit-4.11.jar -p %src_folder% %script_folder%*.java

java  -cp _build;%lib_folder%\junit-4.11.jar ru.ifmo.rain.varfolomeev.bank.BankTests

rmdir _build /s /q

exit %errorlevel%