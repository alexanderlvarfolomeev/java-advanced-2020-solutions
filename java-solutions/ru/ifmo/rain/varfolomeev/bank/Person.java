package ru.ifmo.rain.varfolomeev.bank;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.NoSuchElementException;

public interface Person extends Remote {
    public String getFirstName() throws RemoteException;

    public String getLastName() throws RemoteException;

    public String getPassportId() throws RemoteException;

    public Map<String, Account> getAccounts() throws RemoteException;

    public void setAmount(String accountId, int amount) throws RemoteException;

    public void addAmount(String accountId, int amount) throws RemoteException;

    public int getAmount(String accountId) throws RemoteException;
}
