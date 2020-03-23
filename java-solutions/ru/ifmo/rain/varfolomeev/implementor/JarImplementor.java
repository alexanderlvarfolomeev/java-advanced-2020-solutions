package ru.ifmo.rain.varfolomeev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Implementation of {@link JarImpler} interface.
 *
 * @author Alexander Varfolomeev
 */
public class JarImplementor extends Implementor implements JarImpler {

    /**
     * Produces {@code .jar} file implementing class or interface specified by provided <var>token</var>.
     * <p>
     * Creates temporary directory to store {@code .java} and {@code .class} files.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target {@code .jar} file.
     * @throws ImplerException if
     *                         <ul>
     *                             <li><var>jarFile</var> is {@code null}</li>
     *                             <li>Temporary directory can't be created</li>
     *                             <li>Error occurs during invocation of {@link #implement(Class, Path)} method</li>
     *                             <li>{@link JavaCompiler} can't compile the class</li>
     *                             <li>Error occurs during I/O operations with jar archive</li>
     *                         </ul>
     * @see JarImpler#implementJar(Class, Path)
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        SourceImplementor.validate(jarFile == null, "jar file path can't be null");
        Path tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory("implementor_temp");
        } catch (IOException e) {
            throw new ImplerException("Can't create temp directory", e);
        }
        tempDirectory.toFile().deleteOnExit();
        implement(token, tempDirectory);
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        String[] args;
        try {
            args = new String[]{
                    "-cp",
                    tempDirectory.toString() + File.pathSeparator + Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                    SourceImplementor.getFullPath(token, tempDirectory, SourceImplementor.JAVA_EXTENSION).toString()
            };
        } catch (URISyntaxException e) {
            throw new ImplerException(e);
        }
        SourceImplementor.validate(compiler == null, "Could not find java compiler");
        SourceImplementor.validate(compiler.run(null, null, null, args) != 0, "Can't compile java file: " + Arrays.toString(args));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            outputStream.putNextEntry(new ZipEntry(SourceImplementor.getFullPath(token, Path.of(""), SourceImplementor.CLASS_EXTENSION).toString().replace('\\', '/')));
            Files.copy(SourceImplementor.getFullPath(token, tempDirectory, SourceImplementor.CLASS_EXTENSION), outputStream);
        } catch (IOException e) {
            throw new ImplerException("Can't write to jar file", e);
        }
    }

    /**
     * The function which invokes {@link #implement(Class, Path)} or {@link #implementJar(Class, Path)} method with received arguments.
     * The invoked method depends on if key {@code -jar} is provided.
     * <p>
     * Received arguments are {@code <classname> <output directory>} or {@code -jar <classname> <output jar file>}.
     *
     * @param args received arguments
     * @throws ImplerException if
     *                         <ul>
     *                            <li>Application gets invalid arguments</li>
     *                             <li>The type token argument represents nonexistent class</li>
     *                             <li>The path argument is incorrect path</li>
     *                             <li>Error occurs during {@link #implement(Class, Path)} or {@link #implementJar(Class, Path)} invocation</li>
     *                         </ul>
     */
    public static void main(String[] args) throws ImplerException {
        SourceImplementor.validate(args == null || args.length != 2 && !(args.length == 3 && "-jar".equals(args[0])),
                "Usage: java Implementor <classname> <output directory> or java Implementor -jar <classname> <output jarfile>");
        try {
            Class<?> token = Class.forName(args[args.length - 2]);
            Path path = Path.of(args[args.length - 1]);
            JarImpler implementor = new JarImplementor();
            if (args.length == 2) {
                implementor.implement(token, path);
            } else {
                implementor.implementJar(token, path);
            }
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Class is not found", e);
        } catch (InvalidPathException e) {
            throw new ImplerException(String.format("Output %s is incorrect path", args.length == 2 ? "directory" : "jar file"), e);
        }
    }
}
