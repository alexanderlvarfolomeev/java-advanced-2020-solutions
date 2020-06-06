package ru.ifmo.rain.varfolomeev.bank.server;

import ru.ifmo.rain.varfolomeev.bank.common.Bank;

import java.io.Closeable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server implements Closeable {
    private final static int DEFAULT_PORT = 8888;
    private Bank bank;

    public void start() {
        if (bank != null) {
            throw new IllegalStateException("Bank is already bound");
        }
        bank = new RemoteBank(DEFAULT_PORT);
        System.out.println("Starting Server.");
        try {
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            Naming.rebind("//localhost/bank", bank);
            System.out.println("Server started");
        } catch (RemoteException e) {
            System.out.println("Cannot export bank: " + e.getMessage());
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }

    public Bank getBank() {
        return bank;
    }

    public void close() {
        System.out.println("Closing Server.");
        try {
            bank.close();
        } catch (RemoteException ignored) {
            //
        }

        try {
            Naming.unbind("//localhost/bank");
        } catch (RemoteException e) {
            System.out.println("Registry could not be contacted");
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL");
        }
        try {
            UnicastRemoteObject.unexportObject(bank, true);
        } catch (NoSuchObjectException e) {
            System.out.println("Bank is not bound");
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
