set script_folder=%~dp0
set src_folder=%script_folder%..\..\java-solutions
set lib_folder=%src_folder%\..\..\java-advanced-2020\lib
set bank_folder=%src_folder%\ru\ifmo\rain\varfolomeev\bank\
set java_sources=%bank_folder%tests\*.java %bank_folder%client\*.java %bank_folder%server\*.java %bank_folder%common\*.java %bank_folder%server\persons\*.java %bank_folder%server\accounts\*.java

javac -d _build -cp %lib_folder%\* -p %src_folder% %java_sources%
