package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    // Идентификатор
    public String getId() throws RemoteException;

    // Количество денег
    public int getAmount() throws RemoteException;

    // Изменить количество денег
    public void setAmount(int amount)
            throws RemoteException;
}
