package ru.ifmo.rain.varfolomeev.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount {

    LocalAccount(String id) {
        super(id, 0);
    }

    LocalAccount(String id, int amount) {
        super(id, amount);

    }
}
