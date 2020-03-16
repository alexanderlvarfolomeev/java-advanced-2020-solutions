set impl_package="ru.ifmo.rain.varfolomeev.implementor"
set korneev_impl_package="info.kgeorgiy.java.advanced.implementor"
set src_folder="../../../../.."

javadoc -d _javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api/ -private -author -sourcepath %src_folder% %korneev_impl_package% %impl_package%