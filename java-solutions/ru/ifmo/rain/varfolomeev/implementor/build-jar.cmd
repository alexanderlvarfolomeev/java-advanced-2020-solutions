set src_folder=..\..\..\..\..
set modules_folder=%src_folder%\..\..\java-advanced-2020\modules
set source-package=%modules_folder%\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor
set source-files=%source-package%\Impler.java %source-package%\JarImpler.java %source-package%\ImplerException.java

javac -d _build -p %src_folder% -sourcepath %src_folder% %source-files% JarImplementor.java

jar -c -f _implementor.jar -e ru/ifmo/rain/varfolomeev/implementor/JarImplementor -C _build .

rmdir _build /s /q