package ru.ifmo.rain.varfolomeev.bank;

import java.util.HashMap;
import java.util.Map;

class RemoteBank implements Bank {
    private final Map<String, Account> accounts;

    RemoteBank() {
        accounts = new HashMap<>();
    }

    public Account createAccount(String id) {
        Account account = new RemoteAccount(id);
        accounts.put(id, account);
        return account;
    }
    public Account getAccount(String id) {
        return accounts.get(id);
    }
}
