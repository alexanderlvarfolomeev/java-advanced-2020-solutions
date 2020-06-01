package ru.ifmo.rain.varfolomeev.bank;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

abstract class AbstractPerson implements Person, Remote, Serializable {
    private final String firstName;
    private final String lastName;
    private final String passportId;
    private final Map<String, Account> accounts;

    AbstractPerson(String firstName, String lastName, String passportId, Map<String, Account> accounts) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportId = passportId;
        this.accounts = accounts;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassportId() {
        return passportId;
    }

    @Override
    public Map<String, Account> getAccounts() {
        return accounts;
    }
}
