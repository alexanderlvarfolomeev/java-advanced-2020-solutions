package ru.ifmo.rain.varfolomeev.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private final static int DEFAULT_PORT = 8080;

    public void start() {
        Bank bank = new RemoteBank();
        try {
            UnicastRemoteObject.exportObject(bank, DEFAULT_PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (RemoteException e) {
            System.out.println("Cannot export object: " +
                    e.getMessage());
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }
}
