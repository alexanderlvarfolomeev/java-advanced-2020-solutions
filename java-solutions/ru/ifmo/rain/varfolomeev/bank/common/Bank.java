package ru.ifmo.rain.varfolomeev.bank.common;

import java.io.Closeable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote, Closeable {
    /**
     * Creates a new account with specified identifier if it doesn't already exists.
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id)
            throws RemoteException;

    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id)
            throws RemoteException;

    /**
     *
     * @param passportId passport identifier of person to find
     * @param personType {@link PersonType} that shows type of returning person
     * @return person with given passport identifier or {@code null} if there is no such person
     */
    Person getPersonByPassportId(String passportId, PersonType personType) throws RemoteException;

    /**
     * Registers new person with specified first name, last name and passport identifier
     * if it doesn't already exist
     * @param firstName the person first name
     * @param lastName the person last name
     * @param passportId the person passport identifier
     * @return registered person or person with the same passport identifier if it already exists
     */
    Person registerPerson(String firstName, String lastName, String passportId) throws RemoteException;

    /**
     * Closes bank
     */
    void close() throws RemoteException;

    enum PersonType {
        LOCAL, REMOTE
    }
}