package ru.ifmo.rain.varfolomeev.bank;

import java.io.Serializable;

public class LocalAccount implements Serializable, Account {
    private final String id;
    private int amount;

    LocalAccount(String id) {
        this(id, 0);
    }

    LocalAccount(String id, int amount) {
        this.amount = amount;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
