package ru.ifmo.rain.varfolomeev.bank.common.accounts;

public class LocalAccount extends AbstractAccount {

    public LocalAccount(String id) {
        super(id, 0);
    }

    public LocalAccount(String id, int amount) {
        super(id, amount);

    }
}
