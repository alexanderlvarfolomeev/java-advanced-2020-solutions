package ru.ifmo.rain.varfolomeev.bank.server;

import ru.ifmo.rain.varfolomeev.bank.common.Bank;
import ru.ifmo.rain.varfolomeev.bank.common.Account;
import ru.ifmo.rain.varfolomeev.bank.server.accounts.LocalAccount;
import ru.ifmo.rain.varfolomeev.bank.server.accounts.RemoteAccount;
import ru.ifmo.rain.varfolomeev.bank.server.persons.LocalPerson;
import ru.ifmo.rain.varfolomeev.bank.common.Person;
import ru.ifmo.rain.varfolomeev.bank.server.persons.RemotePerson;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RemoteBank implements Bank {
    private final int port;
    private final Map<String, Person> persons;

    public RemoteBank(int port) {
        this.port = port;
        persons = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized Account createAccount(String id) throws RemoteException {
        checkNonNull(id);
        Map.Entry<String, String> ids = getPassportAndAccountId(id);
        Person person = getPersonByPassportId(ids.getKey(), PersonType.REMOTE);
        if (person == null) {
            return null;
        }
        Account account = new RemoteAccount(ids.getValue());
        Account previousAccount = person.getAccounts().putIfAbsent(ids.getValue(), account);
        if (previousAccount == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return previousAccount;
        }
    }

    @Override
    public Account getAccount(String id) throws RemoteException {
        checkNonNull(id);
        Map.Entry<String, String> ids = getPassportAndAccountId(id);
        Person person = persons.get(ids.getKey());
        if (person == null) {
            return null;
        }
        return persons.get(ids.getKey()).getAccounts().get(ids.getValue());
    }

    @Override
    public Person getPersonByPassportId(String passportId, PersonType personType) throws RemoteException {
        checkNonNull(passportId, personType);
        Person person = persons.get(passportId);
        if (person == null) {
            return null;
        }
        return personType == PersonType.REMOTE ? person : new LocalPerson(
                person.getFirstName(), person.getLastName(),
                person.getPassportId(), convertAccountsToLocal(person.getAccounts()));
    }

    private static Map<String, Account> convertAccountsToLocal(Map<String, Account> accounts) {
        return accounts.entrySet().stream().map(entry -> {
            try {
                return Map.entry(entry.getKey(), new LocalAccount(entry.getValue().getId(), entry.getValue().getAmount()));
            } catch (RemoteException e) {
                throw new RuntimeException("Can't create local copy of account", e);
            }
        }).collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public synchronized Person registerPerson(String firstName, String lastName, String passportId) throws RemoteException {
        checkNonNull(firstName, lastName, passportId);
        Person person = new RemotePerson(lastName, firstName, passportId);
        Person previousPerson = persons.putIfAbsent(passportId, person);
        if (previousPerson == null) {
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return previousPerson;
        }
    }

    @Override
    public void close() {
        persons.values().forEach(p -> {
            try {
                p.getAccounts().values().forEach(a -> {
                    try {
                        UnicastRemoteObject.unexportObject(a, true);
                    } catch (NoSuchObjectException ignored) {
                        //
                    }
                });
                UnicastRemoteObject.unexportObject(p, true);
            } catch (RemoteException ignored) {
                //
            }
        });
    }

    private static Map.Entry<String, String> getPassportAndAccountId(String id) {
        String[] idParts = id.split(":");
        if (idParts.length != 2) {
            throw new IllegalArgumentException("Account ID has illegal format");
        }
        return Map.entry(idParts[0], idParts[1]);
    }

    private static void checkNonNull(Object... objects) {
        Arrays.stream(objects).forEach(Objects::requireNonNull);
    }
}
