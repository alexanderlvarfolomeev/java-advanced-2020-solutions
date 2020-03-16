javac -d _build -p .;artifacts;lib; --module-source-path . -m "ru.ifmo.rain.varfolomeev.implementor"

jar -c -f _implementor.jar -e ru/ifmo/rain/varfolomeev/implementor/JarImplementor --module-version 1.0 -C _build\ru.ifmo.rain.varfolomeev.implementor .

rmdir _build /s /q