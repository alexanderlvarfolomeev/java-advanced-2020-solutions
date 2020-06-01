package ru.ifmo.rain.varfolomeev.bank;

import java.rmi.RemoteException;

class RemoteAccount extends AbstractAccount {

    RemoteAccount(String id) {
        super(id, 0);
    }
}
