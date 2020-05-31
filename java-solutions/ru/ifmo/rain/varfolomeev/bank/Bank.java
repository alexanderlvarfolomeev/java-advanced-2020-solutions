package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Bank extends Remote {
    // Создает счет
    Account createAccount(String id)
            throws RemoteException;

    // Возвращает счет
    Account getAccount(String id)
            throws RemoteException;

    Person getPersonByPassportId(String passportId, PersonType personType) throws RemoteException;

    Person registerPerson(String firstName, String lastName, String passportId) throws RemoteException;

    void close() throws RemoteException;

    enum PersonType {
        LOCAL, REMOTE
    }
}