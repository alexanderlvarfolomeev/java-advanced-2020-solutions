package ru.ifmo.rain.varfolomeev.bank;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.NoSuchElementException;

public class LocalPerson implements Serializable, Person {
    private final String firstName;
    private final String lastName;
    private final String passportId;
    private final Map<String, Account> accounts;

    public LocalPerson(String firstName, String lastName, String passportId, Map<String, Account> accounts) {
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

    @Override
    public void setAmount(String accountId, int amount) throws RemoteException {
        getAccountIfExists(accountId).setAmount(amount);
    }

    @Override
    public void addAmount(String accountId, int amount) throws RemoteException {
        getAccountIfExists(accountId).addAmount(amount);
    }

    @Override
    public int getAmount(String accountId) throws RemoteException {
        return getAccountIfExists(accountId).getAmount();
    }

    private Account getAccountIfExists(String accountId) {
        Account account = getAccounts().get(accountId);
        if (account == null) {
            throw new NoSuchElementException("There is no account with such ID");
        }
        return account;
    }
}
