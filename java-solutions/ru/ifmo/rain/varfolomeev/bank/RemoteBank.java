package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

class RemoteBank implements Bank {
    private final Map<String, Person> persons;

    public RemoteBank() {
        persons = new ConcurrentHashMap<>();
    }

    @Override
    public Account createAccount(String id) throws RemoteException {
        checkNonNull(id);
        Account account = getAccount(id);
        if (account != null) {
            throw new UnsupportedOperationException("Account already exists");
        }
        account = new RemoteAccount(id);
        Map.Entry<String, String> ids = getPassportAndAccountId(id);
        persons.get(ids.getKey()).getAccounts().put(ids.getValue(), account);
        return account;
    }

    @Override
    public Account getAccount(String id) throws RemoteException {
        checkNonNull(id);
        Map.Entry<String, String> ids = getPassportAndAccountId(id);
        Person person = persons.get(ids.getKey());
        if (person == null) {
            throw new NoSuchElementException("Can't find the person: " + ids.getKey());
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
    public Person registerPerson(String firstName, String lastName, String passportId) throws RemoteException {
        checkNonNull(firstName, lastName, passportId);
        if (getPersonByPassportId(passportId, PersonType.REMOTE) != null) {
            throw new UnsupportedOperationException("Person is already registered");
        }
        try {
            Person person = new RemotePerson(lastName, firstName, passportId, new ConcurrentHashMap<>());
            persons.put(passportId, person);
            return person;
        } catch (RemoteException e) {
            throw new RemoteException("Unable to export new RemotePerson instance", e);
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
