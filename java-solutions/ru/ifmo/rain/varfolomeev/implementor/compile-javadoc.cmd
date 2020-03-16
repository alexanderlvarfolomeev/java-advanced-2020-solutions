set impl_package=ru.ifmo.rain.varfolomeev.implementor

set src_folder=..\..\..\..\..
set modules_folder=%src_folder%\..\..\java-advanced-2020\modules
set source-package=%modules_folder%\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor

set link=https://docs.oracle.com/en/java/javase/11/docs/api/
set source-files=%source-package%\Impler.java %source-package%\JarImpler.java %source-package%\ImplerException.java

javadoc -d _javadoc -link %link% -private -author -sourcepath %src_folder% %impl_package% %source-files%