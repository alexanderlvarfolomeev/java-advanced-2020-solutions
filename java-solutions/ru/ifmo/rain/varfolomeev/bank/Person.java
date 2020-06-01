package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Person extends Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassportId() throws RemoteException;

    Map<String, Account> getAccounts() throws RemoteException;

    default Account getAccount(String accountId) throws RemoteException {
        return getAccounts().get(accountId);
    }
}
