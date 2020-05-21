package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    // Создает счет
    public Account createAccount(String id)
            throws RemoteException;

    // Возвращает счет
    public Account getAccount(String id)
            throws RemoteException;
}