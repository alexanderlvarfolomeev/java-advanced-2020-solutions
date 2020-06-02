package ru.ifmo.rain.varfolomeev.bank.common.persons;

import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson {
    public RemotePerson(String firstName, String lastName, String passportId) {
        super(lastName, firstName, passportId, new ConcurrentHashMap<>());
    }
}
