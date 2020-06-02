package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Person extends Remote {

    /**
     * gets person first name
     * @return first name
     */
    String getFirstName() throws RemoteException;

    /**
     * gets person last name
     * @return last name
     */
    String getLastName() throws RemoteException;

    /**
     * gets person passport identifier
     * @return passport identifier
     */
    String getPassportId() throws RemoteException;

    /**
     * gets {@link Map} from account identifiers to accounts of this person
     * @return person accounts
     */
    Map<String, Account> getAccounts() throws RemoteException;

    /**
     * gets account with certain identifier
     * @param accountId account identifier
     * @return account with given identifier of {@code null} if there is no such account
     */
    default Account getAccount(String accountId) throws RemoteException {
        return getAccounts().get(accountId);
    }
}
