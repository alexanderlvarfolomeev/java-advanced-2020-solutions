package ru.ifmo.rain.varfolomeev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Produces code implementing class or interface specified by provided <var>token</var>.
 *
 * @author Alexander Varfolomeev
 */
class Implementor {
    /**
     * Line separator in the current file system.
     */
    private final static String LINE_SEPARATOR = System.lineSeparator();
    /**
     * Space for Java source file.
     */
    private final static String SPACE = " ";
    /**
     * Intend for Java source file.
     */
    private final static String TAB = "    ";
    /**
     * Delimiter between arguments of parameters for Java source file.
     */
    private final static String DELIMITER = ", ";
    /**
     * Opening parentheses for Java source file.
     */
    private final static String OPENING_PARENTHESES = "(";
    /**
     * Closing parentheses for Java source file.
     */
    private final static String CLOSING_PARENTHESES = ")";
    /**
     * Empty string which is used to represent the absence of some part of code in Java source file.
     */
    private final static String EMPTY_STRING = "";
    /**
     * Semicolon for Java source file.
     */
    private final static char SEMICOLON = ';';
    /**
     * Opening bracket for Java source file.
     */
    private final static char OPENING_BRACKET = '{';
    /**
     * Closing bracket for Java source file.
     */
    private final static char CLOSING_BRACKET = '}';

    /**
     * Extension for Java source files.
     */
    final static String JAVA_EXTENSION = ".java";
    /**
     * Extension for Java compiled files.
     */
    final static String CLASS_EXTENSION = ".class";

    /**
     * Type token representing class to implement.
     */
    private final Class<?> token;
    /**
     * Java source file path where class code will be written.
     */
    private final Path javaPath;

    /**
     * Wrapper for correct storage {@link Method} in {@link HashSet}.
     */
    private static class MethodWrapper {
        /**
         * The wrapped method.
         */
        private final Method method;

        /**
         * Creates {@link MethodWrapper} for <var>method</var> parameter.
         *
         * @param method {@link Method} to be wrapped
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * {@link #method} field getter.
         *
         * @return {@link #method}
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Compares this {@link MethodWrapper} and <var>object</var> for equality. MethodWrappers are equal if wrapped
         * methods have equal names, return types and parameter types.
         *
         * @param object {@link Object} to compare
         * @return {@code true} if MethodWrappers are equal or {@code false} else
         */
        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Implementor.MethodWrapper)) {
                return false;
            }
            Implementor.MethodWrapper other = (Implementor.MethodWrapper) object;

            return method.getName().equals(other.method.getName())
                    && method.getReturnType().equals(other.method.getReturnType())
                    && Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes());
        }

        /**
         * Calculates {@link MethodWrapper} hashcode in such way that equal MethodWrappers have the same hashcode.
         *
         * @return {@link MethodWrapper} hashcode
         */
        @Override
        public int hashCode() {
            return method.getName().hashCode()
                    ^ method.getReturnType().hashCode()
                    ^ Arrays.hashCode(method.getParameterTypes());
        }
    }

    /**
     * {@link BufferedWriter} with Unicode characters support.
     */
    private static class UnicodeBufferedWriter extends BufferedWriter {

        /**
         * Creates instance with specified character-output stream
         *
         * @param out {@link BufferedWriter} to be wrapped
         * @see BufferedWriter#BufferedWriter(Writer)
         */
        public UnicodeBufferedWriter(Writer out) {
            super(out);
        }

        /**
         * Writes a string.
         *
         * @param string {@link String} to be written
         * @throws IOException if I/O error occurs in {@link BufferedWriter#write(String)}
         */
        @Override
        public void write(String string) throws IOException {
            super.write(toUnicode(string));
        }

        /**
         * Converts UTF-8 {@link String} to Unicode format.
         *
         * @param string {@link String} to convert
         * @return {@link String} in Unicode format
         */
        private static String toUnicode(String string) {
            StringBuilder builder = new StringBuilder();
            for (char c : string.toCharArray()) {
                if (c >= 128) {
                    builder.append(String.format("\\u%04X", (int) c));
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }
    }

    /**
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException if <var>token</var> and <var>root</var> doesn't pass the validation
     * @see #validateInput(Class, Path)
     */
    Implementor(Class<?> token, Path root) throws ImplerException {
        validateInput(token, root);
        this.token = token;
        this.javaPath = getFullPath(token, root, JAVA_EXTENSION);
    }

    /**
     * Implements {@link #token} class and writes it in {@link #javaPath}.
     *
     * @throws ImplerException if
     *                         <ul>
     *                             <li>{@link #token} class is not an interface and contains only private constructors</li>
     *                             <li>Error occurs during I/O operations with {@link #javaPath}</li>
     *                         </ul>
     */
    void run() throws ImplerException {
        createParentDirectory();
        try (BufferedWriter writer = new UnicodeBufferedWriter(Files.newBufferedWriter(javaPath))) {
            writer.write(getPackage());
            writer.write(getClassHeader());
            if (!token.isInterface()) {
                writer.write(implementConstructors());
            }
            String implementedMethods = implementMethods();
            if (!implementedMethods.isEmpty()) {
                writer.newLine();
            }
            writer.write(implementedMethods);
            writer.write(CLOSING_BRACKET);
            writer.newLine();
        } catch (IOException e) {
            throw new ImplerException("Can't write to output file", e);
        }
    }

    /**
     * Creates parent directory for {@link #javaPath}.
     *
     * @throws ImplerException if error occurs during directory creation.
     */
    private void createParentDirectory() throws ImplerException {
        Path outputParent = javaPath.getParent();
        if (outputParent != null) {
            try {
                Files.createDirectories(outputParent);
            } catch (IOException e) {
                throw new ImplerException("Can't create output file package", e);
            }
        }
    }

    /**
     * Calculates {@link Path} of <var>token</var> file location.
     *
     * @param token     {@link Class} which file location is calculated
     * @param root      {@link Path} to root directory
     * @param extension file extension
     * @return {@link Path} which represents <var>token</var> file
     */
    static Path getFullPath(Class<?> token, Path root, String extension) {
        return root.resolve(token.getPackageName()
                .replace('.', File.separatorChar)).resolve(getClassName(token) + extension);
    }

    /**
     * Returns {@link String} representation of {@link #token} package name.
     *
     * @return package name
     */
    private String getPackage() {
        StringBuilder builder = new StringBuilder();
        String pack = token.getPackage().getName();
        if (!pack.isEmpty()) {
            builder
                    .append("package")
                    .append(SPACE)
                    .append(pack)
                    .append(SEMICOLON)
                    .append(LINE_SEPARATOR)
                    .append(LINE_SEPARATOR);
        }
        return builder.toString();
    }

    /**
     * Returns {@link String} representation of {@link #token} class header.
     *
     * @return class header
     */
    private String getClassHeader() {
        return "public" +
                SPACE +
                "class" +
                SPACE +
                getClassName(token) +
                SPACE +
                (token.isInterface() ? "implements" : "extends") +
                SPACE +
                token.getCanonicalName() +
                SPACE +
                OPENING_BRACKET +
                LINE_SEPARATOR;
    }

    /**
     * Returns {@link Class#getSimpleName()} of generated class.
     *
     * @param token {@link Class} to implement
     * @return generated class simple name
     */
    private static String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Returns {@link String} representation of all implemented constructors.
     *
     * @return implemented constructors
     * @throws ImplerException if {@link #token} contains only private constructors
     */
    private String implementConstructors() throws ImplerException {
        Constructor<?>[] constructors = Arrays
                .stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .toArray(Constructor<?>[]::new);
        validate(constructors.length == 0, "There is only private constructors in class");
        return Arrays
                .stream(constructors)
                .map(this::getExecutable)
                .collect(Collectors.joining(LINE_SEPARATOR + LINE_SEPARATOR));
    }

    /**
     * Returns {@link String} representation of all implemented methods.
     *
     * @return implemented methods
     */
    private String implementMethods() {
        Set<Implementor.MethodWrapper> methods = new HashSet<>();
        getMethods(token, methods);
        return methods
                .stream()
                .map(Implementor.MethodWrapper::getMethod)
                .filter(method -> Modifier.isAbstract(method.getModifiers()) && !method.isDefault())
                .map(this::getExecutable)
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

    /**
     * Recursively gets declared methods of <var>superToken</var> and puts them in <var>methods</var>.
     *
     * @param superToken superclass of {@link #token}
     * @param methods    {@link Set} of {@link #token} methods
     */
    private void getMethods(Class<?> superToken, Set<Implementor.MethodWrapper> methods) {
        if (superToken == null) return;
        Arrays.stream(superToken.getDeclaredMethods()).map(Implementor.MethodWrapper::new).collect(Collectors.toCollection(() -> methods));
        getMethods(superToken.getSuperclass(), methods);
        for (Class<?> tokenInterface : superToken.getInterfaces()) {
            getMethods(tokenInterface, methods);
        }
    }

    /**
     * Returns {@link String} representation of <var>executable</var> code.
     *
     * @param executable {@link Executable} to implement
     * @return code of <var>executable</var> implementation
     */
    private StringBuilder getExecutable(Executable executable) {
        String name;
        String body;
        if (executable instanceof Method) {
            Method method = (Method) executable;
            name = method.getGenericReturnType().getTypeName().replace('$', '.') + SPACE + method.getName();
            if (method.getReturnType().equals(void.class)) {
                return getExecutable(executable, name, EMPTY_STRING);
            }
            body = "return " + getDefault(method.getReturnType());
        } else {
            Constructor<?> constructor = (Constructor<?>) executable;
            name = getClassName(token);
            body = "super" + getParameters(constructor, false);
        }
        return getExecutable(
                executable,
                name,
                LINE_SEPARATOR + TAB + TAB + body + SEMICOLON + LINE_SEPARATOR + TAB);
    }

    /**
     * Returns {@link String} representation of <var>executable</var> code with not empty body.
     *
     * @param executable     {@link Executable} to implement
     * @param executableName <var>executable</var> name
     * @param body           <var>executable</var> body
     * @return code of <var>executable</var> implementation
     * @see #getExecutable(Executable)
     */
    private StringBuilder getExecutable(Executable executable, String executableName, String body) {
        return new StringBuilder()
                .append(executable.getAnnotation(Deprecated.class) == null ? EMPTY_STRING : "@Deprecated" + LINE_SEPARATOR)
                .append(TAB)
                .append(getModifiers(executable))
                .append(SPACE)
                .append(getTypeVariables(executable))
                .append(executableName)
                .append(getParameters(executable, true))
                .append(SPACE)
                .append(getExceptions(executable))
                .append(OPENING_BRACKET)
                .append(body)
                .append(CLOSING_BRACKET)
                .append(LINE_SEPARATOR);
    }

    /**
     * Returns {@link String} representation of default value of <var>type</var>
     *
     * @param type {@link Class} which default value is returned
     * @return default value
     */
    private static String getDefault(Class<?> type) {
        return type.equals(boolean.class) ? "false"
                : type.isPrimitive() ? "0" : "null";
    }

    /**
     * Returns {@link String} representation of modifiers of implemented <var>executable</var>.
     *
     * @param executable {@link Executable} to implement
     * @return <var>executable</var> modifiers
     */
    private static String getModifiers(Executable executable) {
        return Modifier.toString(executable.getModifiers()
                & ~Modifier.ABSTRACT
                & ~Modifier.TRANSIENT
                & ~Modifier.NATIVE);
    }

    /**
     * Returns {@link String} representation of type variables of implemented <var>executable</var>.
     *
     * @param executable {@link Executable} to implement
     * @return type variables
     */
    private String getTypeVariables(Executable executable) {
        TypeVariable<?>[] typeParameters = executable.getTypeParameters();
        return typeParameters.length == 0 ? EMPTY_STRING :
                Arrays.stream(typeParameters)
                        .map(Implementor::typeVarBounds)
                        .collect(Collectors.joining(", ", "<", "> "));
    }

    /**
     * Returns {@link String} representation of type variable with its bounds if they exists.
     *
     * @param typeVariable type variable
     * @return type variable and bounds
     */
    private static String typeVarBounds(TypeVariable<?> typeVariable) {
        Type[] bounds = typeVariable.getBounds();
        if (bounds.length == 1 && bounds[0].equals(Object.class)) {
            return typeVariable.getName();
        } else {
            return typeVariable.getName() + " extends "
                    + Arrays.stream(bounds)
                    .map(Type::getTypeName)
                    .collect(Collectors.joining(" & "));
        }
    }

    /**
     * Returns {@link String} representation of <var>executable</var>'s parameters, optionally with types.
     *
     * @param executable {@link Executable} which parameters will be returned
     * @param isTyped    {@code true} if types are needed or {@code false} else
     * @return <var>executable</var>'s parameters
     */
    private static String getParameters(Executable executable, boolean isTyped) {
        Parameter[] parameters = executable.getParameters();
        return Arrays
                .stream(parameters)
                .map(parameter -> getParameter(parameter, isTyped))
                .collect(Collectors.joining(DELIMITER, OPENING_PARENTHESES, CLOSING_PARENTHESES));
    }

    /**
     * Returns {@link String} representation of <var>parameter</var>, optionally with types.
     *
     * @param parameter {@link Parameter} to convert
     * @param isTyped   {@code true} if type is needed or {@code false} else
     * @return parameter
     */
    private static String getParameter(Parameter parameter, boolean isTyped) {
        return (isTyped ? parameter.getParameterizedType().getTypeName() + SPACE : EMPTY_STRING).replace('$', '.') + parameter.getName();
    }

    /**
     * Returns {@link String} representation of <var>executable</var>'s exceptions.
     *
     * @param executable {@link Executable} which exceptions are returned
     * @return {@link String} that contains <var>executable</var>'s exceptions
     */
    private static String getExceptions(Executable executable) {
        Type[] exceptions = executable.getGenericExceptionTypes();
        return exceptions.length == 0 || (executable instanceof Method) ? EMPTY_STRING
                : "throws" + Arrays.stream(exceptions).map(Type::getTypeName)
                .collect(Collectors.joining(DELIMITER, SPACE, SPACE)).replace('$', '.');
    }

    /**
     * Validation of statement which result is <var>predicate</var>
     *
     * @param predicate predicate which shouldn't be {@code true}
     * @param message   message of {@link ImplerException}
     * @throws ImplerException if predicate is {@code true}
     */
    static void validate(boolean predicate, String message) throws ImplerException {
        if (predicate) {
            throw new ImplerException(message);
        }
    }

    /**
     * Validation of {@link #Implementor(Class, Path)} arguments.
     *
     * @param token type token to validate
     * @param root  root directory to validate
     * @throws ImplerException if
     *                         <ul>
     *                             <li><var>token</var> or <var>root</var> is {@code null}</li>
     *                             <li>Class represented by <var>token</var> can't be implemented for some reasons.
     *                             The certain reason will be pointed in Exception message</li>
     *                         </ul>
     */
    private static void validateInput(Class<?> token, Path root) throws ImplerException {
        validate(token == null, "Type token can't be null");
        validate(root == null, "Output directory path can't be null");
        validate(Modifier.isPrivate(token.getModifiers()), "Can't implement private interface or class");
        validate(token.isPrimitive() || token.isArray()
                        || token == Enum.class || Modifier.isFinal(token.getModifiers()),
                "Incorrect type token");
    }
}
