package ru.ifmo.rain.varfolomeev.bank;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

class RemoteAccount extends UnicastRemoteObject implements Account, Remote {
    private final String id;
    private int amount;

    RemoteAccount(String id) throws RemoteException {
        super();
        amount = 0;
        this.id = id;
    }

    public String getId() throws RemoteException {
        return id;
    }

    public int getAmount() throws RemoteException {
        return amount;
    }

    public void setAmount(int amount) throws RemoteException {
        this.amount = amount;
    }
}
