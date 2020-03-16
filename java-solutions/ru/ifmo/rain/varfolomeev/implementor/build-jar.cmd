set src_folder="../../../../.."

javac -d _build -p %src_folder% -sourcepath %src_folder% JarImplementor.java

jar -c -f _implementor.jar -e ru/ifmo/rain/varfolomeev/implementor/JarImplementor -C _build .

Rem rmdir _build /s /q