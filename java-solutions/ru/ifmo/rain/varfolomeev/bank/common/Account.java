package ru.ifmo.rain.varfolomeev.bank.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    /**
     * Gets account's identifier
     * @return account id
     */
    String getId() throws RemoteException;


    /**
     * Gets money amount on this account
     * @return money amount
     */
    int getAmount() throws RemoteException;

    /**
     * Set money amount on this account
     * @param amount new money amount
     */
    void setAmount(int amount) throws RemoteException;

    /**
     * Add money amount to money on this account
     * @param amount additional money amount
     */
    void addAmount(int amount) throws RemoteException;
}
