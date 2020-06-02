package ru.ifmo.rain.varfolomeev.bank.common.accounts;

import java.io.Serializable;
import java.rmi.Remote;

abstract class AbstractAccount implements Account, Remote, Serializable {
    private final String id;
    private int amount;

    AbstractAccount(String id, int amount) {
        this.amount = amount;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        return amount;
    }

    public synchronized void setAmount(int amount) {
        this.amount = amount;
    }

    public synchronized void addAmount(int amount) {
        setAmount(getAmount() + amount);
    }
}
