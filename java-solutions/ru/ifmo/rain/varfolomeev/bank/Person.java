package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.NoSuchElementException;

public interface Person extends Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassportId() throws RemoteException;

    Map<String, Account> getAccounts() throws RemoteException;

    default Account getAccount(String accountId) throws RemoteException {
        Account account = getAccounts().get(accountId);
        if (account == null) {
            throw new NoSuchElementException("There is no account with such ID");
        }
        return account;
    }
}
