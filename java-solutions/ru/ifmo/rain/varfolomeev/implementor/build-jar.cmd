set src_folder=%~dp0..\..\..\..\..
set modules_folder=%src_folder%\..\..\java-advanced-2020\modules
set source_package=%modules_folder%\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor
set source_files=%source_package%\Impler.java %source_package%\JarImpler.java %source_package%\ImplerException.java

javac -d _build -p %src_folder% -sourcepath %src_folder% %source_files% %script_folder%JarImplementor.java

jar -c -f _implementor.jar -e ru/ifmo/rain/varfolomeev/implementor/JarImplementor -C _build .

rmdir _build /s /q