package ru.ifmo.rain.varfolomeev.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {

    static void doSmth() throws RemoteException {
        Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        Account account = bank.getAccount("geo");
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount("geo");
        } else {
            System.out.println("Account already exists");
        }

        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + 100);
        System.out.println("Money: " + account.getAmount());
    }
}
