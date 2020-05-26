package ru.ifmo.rain.varfolomeev.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public class Client {
    private final String firstName;
    private final String lastName;
    private final String passportId;
    private final String accountId;
    private final int amountChange;

    private Client(String firstName, String lastName, String passportId, String accountId, int amountChange) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportId = passportId;
        this.accountId = accountId;
        this.amountChange = amountChange;
    }

    private void run() throws RemoteException, ClientException {
        Bank bank = getBank();
        checkAccount(bank, getAndCheckPerson(bank), String.format("%s:%s", passportId, accountId));
    }

    private Person getAndCheckPerson(Bank bank) throws RemoteException {
        Person person = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        if (person == null) {
            System.out.println("Registering person");
            person = bank.registerPerson(firstName, lastName, passportId);
        } else {
            System.out.println("Person is already registered");
            if (!person.getFirstName().equals(firstName)) {
                System.out.println("Names don't match");
            }
            if (!person.getLastName().equals(lastName)) {
                System.out.println("Last names don't match");
            }
        }
        return person;
    }

    private void checkAccount(Bank bank, Person person, String accountFullId) throws RemoteException {
        Account account = bank.getAccount(accountFullId);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(accountFullId);
        } else {
            System.out.println("Account already exists");
        }
        int oldAmount = person.getAmount(accountId);
        System.out.println("Money: " + oldAmount);
        account.addAmount(amountChange);
        int newAmount = person.getAmount(accountId);
        System.out.println("Money: " + newAmount);
        System.out.println(
                oldAmount + amountChange == newAmount ? "Amount has been changed" : "Amount hasn't been changed");
    }

    private static int getIntArgument(String argumentName, String stringArgument) throws NumberFormatException {
        try {
            return Integer.parseInt(stringArgument);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                    String.format("Can't parse argument '%s'. Found '%s'. %s",
                            argumentName, stringArgument, e.getMessage()));
        }
    }

    private static Bank getBank() throws ClientException {
        try {
            return (Bank) Naming.lookup("//localhost/bank");
        } catch (NotBoundException e) {
            throw new ClientException("Bank is not bound");
        } catch (MalformedURLException e) {
            throw new ClientException("Bank URL is invalid");
        } catch (RemoteException e) {
            throw new ClientException("Can't contact the registry");
        }
    }

private static class ClientException extends Exception {
    public ClientException(String message) {
        super(message);
    }
}

    public static void main(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments can't be null");
        } else if (args.length != 5) {
            System.err.println("Usage: Client <firstName> <lastName> <passportId> <accountId> <amountChange>");
        } else {
            try {
                new Client(args[0], args[1], args[2], args[3], getIntArgument("amountChange", args[4])).run();
            } catch (RemoteException | ClientException | NumberFormatException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
