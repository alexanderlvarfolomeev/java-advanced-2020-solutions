package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Map;
import java.util.NoSuchElementException;

public class RemotePerson extends UnicastRemoteObject implements Person, Remote {
    private final String firstName;
    private final String lastName;
    private final String passportId;
    private final Map<String, Account> accounts;

    public RemotePerson(String lastName, String firstName, String passportId, Map<String, Account> accounts) throws RemoteException {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportId = passportId;
        this.accounts = accounts;
    }


    @Override
    public String getFirstName() throws RemoteException {
        return firstName;
    }

    @Override
    public String getLastName() throws RemoteException {
        return lastName;
    }

    @Override
    public String getPassportId() throws RemoteException {
        return passportId;
    }

    @Override
    public Map<String, Account> getAccounts() throws RemoteException {
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

    private Account getAccountIfExists(String accountId) throws RemoteException {
        Account account = getAccounts().get(accountId);
        if (account == null) {
            throw new NoSuchElementException("There is no account with such ID");
        }
        return account;
    }

}
