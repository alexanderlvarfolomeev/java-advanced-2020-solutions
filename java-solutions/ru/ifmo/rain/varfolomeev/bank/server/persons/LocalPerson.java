package ru.ifmo.rain.varfolomeev.bank.server.persons;

import ru.ifmo.rain.varfolomeev.bank.common.Account;
import ru.ifmo.rain.varfolomeev.bank.server.accounts.RemoteAccount;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Objects;

public class LocalPerson extends AbstractPerson {

    public LocalPerson(String firstName, String lastName, String passportId, Map<String, Account> accounts) {
        super(firstName, lastName, passportId, accounts);
    }

    public Account createAccount(String accountId) {
        Objects.requireNonNull(accountId);
        Account account = new RemoteAccount(accountId);
        Account previousAccount = getAccounts().putIfAbsent(accountId, account);
        return Objects.requireNonNullElse(previousAccount, account);
    }

    public void setAmount(String accountId, int amount) throws RemoteException {
        getAccount(accountId).setAmount(amount);
    }

    public void addAmount(String accountId, int amount) throws RemoteException {
        getAccount(accountId).addAmount(amount);
    }

    public int getAmount(String accountId) throws RemoteException {
        return getAccount(accountId).getAmount();
    }
}
