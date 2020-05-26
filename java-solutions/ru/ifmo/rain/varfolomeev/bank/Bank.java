package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Bank extends Remote {
    // Создает счет
    public Account createAccount(String id)
            throws RemoteException;

    // Возвращает счет
    public Account getAccount(String id)
            throws RemoteException;

    public Person getPersonByPassportId(String passportId, PersonType personType) throws RemoteException;

    public Person registerPerson(String firstName, String lastName, String passportId) throws RemoteException;

    public void close() throws RemoteException;

    public enum PersonType {
        LOCAL, REMOTE
    }
}