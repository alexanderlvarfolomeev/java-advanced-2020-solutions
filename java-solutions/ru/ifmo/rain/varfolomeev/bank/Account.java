package ru.ifmo.rain.varfolomeev.bank;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    public String getId() throws RemoteException;

    public int getAmount() throws RemoteException;

    public void setAmount(int amount) throws RemoteException;

    default void addAmount(int amount) throws RemoteException {
        setAmount(getAmount() + amount);
    }
}
