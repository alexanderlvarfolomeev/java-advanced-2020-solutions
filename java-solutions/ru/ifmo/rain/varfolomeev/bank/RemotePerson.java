package ru.ifmo.rain.varfolomeev.bank;

import java.util.concurrent.ConcurrentHashMap;

public class RemotePerson extends AbstractPerson {
    RemotePerson(String firstName, String lastName, String passportId) {
        super(lastName, firstName, passportId, new ConcurrentHashMap<>());
    }
}
