package ru.ifmo.rain.varfolomeev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Implementation of {@link Impler} interface.
 *
 * @author Alexander Varfolomeev
 */
public class Implementor implements Impler {

    /**
     * Produces code implementing class or interface specified by provided <var>token</var>.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if
     *                         <ul>
     *                             <li><var>token</var> and <var>root</var> don't pass validation in {@link SourceImplementor#SourceImplementor(Class, Path)}</li>
     *                             <li>Error occurs during implementation in {@link SourceImplementor#run()}</li>
     *                         </ul>
     * @see Impler#implement(Class, Path)
     */
    public void implement(Class<?> token, Path root) throws ImplerException {
        new SourceImplementor(token, root).run();
    }

    /**
     * The function which invokes {@link #implement(Class, Path)} method with received arguments.
     * Received arguments are {@code <classname> <output directory>}.
     *
     * @param args received arguments
     * @throws ImplerException if
     *                          <ul>
     *                             <li>Application gets invalid arguments</li>
     *                             <li>The first argument represents nonexistent class</li>
     *                             <li>The second argument is incorrect path</li>
     *                             <li>Error occurs during {@link #implement(Class, Path)} invocation</li>
     *                         </ul>
     */
    public static void main(String[] args) throws ImplerException {
        SourceImplementor.validate(args == null || args.length != 2,
                "Usage: java Implementor <classname> <output directory>");
        try {
            new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Invalid class name", e);
        } catch (InvalidPathException e) {
            throw new ImplerException("Output directory is incorrect path", e);
        }
    }
}
