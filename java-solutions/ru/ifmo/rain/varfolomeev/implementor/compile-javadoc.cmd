set impl_package=ru.ifmo.rain.varfolomeev.implementor

set src_folder=%~dp0..\..\..\..\..
set modules_folder=%src_folder%\..\..\java-advanced-2020\modules
set source_package=%modules_folder%\info.kgeorgiy.java.advanced.implementor\info\kgeorgiy\java\advanced\implementor

set link=https://docs.oracle.com/en/java/javase/11/docs/api/
set source_files=%source_package%\Impler.java %source_package%\JarImpler.java %source_package%\ImplerException.java

javadoc -d _javadoc -link %link% -private -author -sourcepath %src_folder% %impl_package% %source_files%