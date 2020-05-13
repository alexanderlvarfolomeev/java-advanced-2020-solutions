package ru.ifmo.rain.varfolomeev.hello;

class HelloUDPService {
    static int getIntArgument(String argumentName, String stringArgument) throws NumberFormatException {
        try {
            return Integer.parseInt(stringArgument);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                    String.format("Can't parse argument '%s'. Found '%s'. %s",
                            argumentName, stringArgument, e.getMessage()));
        }
    }
}
