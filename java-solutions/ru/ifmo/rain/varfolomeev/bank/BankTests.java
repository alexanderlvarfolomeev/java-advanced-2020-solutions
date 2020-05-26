package ru.ifmo.rain.varfolomeev.bank;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BankTests {
    private static Bank bank;
    private static Server SERVER = new Server();
    private static Registry registry;

    private static final int MULTIPLE_TEST_COUNT = 1000;


    @BeforeClass
    public static void beforeClass() {
        try {
            registry = LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            throw new AssertionError("Unable to create registry", e);
        }
        SERVER.start();
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (NotBoundException e) {
            throw new AssertionError("Bank is not bound", e);
        } catch (MalformedURLException e) {
            throw new AssertionError("Bank URL is invalid", e);
        } catch (RemoteException e) {
            throw new AssertionError("Can't contact the registry", e);
        }
    }

    @AfterClass
    public static void afterClass() {
        SERVER.close();
        try {
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (NoSuchObjectException e) {
            System.out.println("Registry is not exported");
        }
    }

    @Test
    public void test01_checkPersonRegistration() throws RemoteException {
        String passport = "test01";
        String firstName = "Alexander";
        String lastName = "Varfolomeev";
        bank.registerPerson(firstName, lastName, passport);

        Person remote = bank.getPersonByPassportId(passport, Bank.PersonType.REMOTE);
        Person local = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);

        assertNotNull(remote);
        assertEquals(remote.getFirstName(), firstName);
        assertEquals(remote.getLastName(), lastName);
        assertEquals(remote.getPassportId(), passport);

        assertNotNull(local);
        assertEquals(local.getFirstName(), firstName);
        assertEquals(local.getLastName(), lastName);
        assertEquals(local.getPassportId(), passport);
    }

    @Test
    public void test02_checkAccountCreation() throws RemoteException {
        String passport = "test02";
        bank.registerPerson(passport, passport, passport);

        bank.createAccount("test02:1");

        Person remote = bank.getPersonByPassportId(passport, Bank.PersonType.REMOTE);
        Person local = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);
        assertNotNull(local.getAccounts().get("1"));
        assertNotNull(remote.getAccounts().get("1"));
    }

    @Test
    public void test03_runClient() {
        System.out.println(String.format("run Client:%n================================="));
        Client.main(new String[]{"Alexander", "Varfolomeev", "test03", "1", "100"});
    }

    @Test
    public void test04_checkDoRemoteSeeChanges() throws RemoteException {
        String passport = "test04";
        bank.registerPerson(passport, passport, passport);

        Person remote1 = bank.getPersonByPassportId(passport, Bank.PersonType.REMOTE);
        Person remote2 = bank.getPersonByPassportId(passport, Bank.PersonType.REMOTE);

        bank.createAccount(passport + ":1");

        assertEquals(1, remote1.getAccounts().size());
        assertEquals(1, remote2.getAccounts().size());
        assertEquals(remote1.getAccounts().get("1").getId(), remote2.getAccounts().get("1").getId());
    }

    @Test
    public void test05_checkDoLocalNotSeeNextRemoteChanges() throws RemoteException {
        String passport = "test05";
        bank.registerPerson(passport, passport, passport);

        Person local1 = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);
        Person local2 = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);

        bank.createAccount(passport + ":1");
        bank.createAccount(passport + ":2");

        assertEquals(0, local1.getAccounts().size());
        assertEquals(0, local2.getAccounts().size());

        Person local3 = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);

        assertEquals(0, local1.getAccounts().size());
        assertEquals(2, local3.getAccounts().size());
    }

    @Test
    public void test06_checkRemoteCantSeeLocalChanges() throws RemoteException {
        String passport = "test06";
        bank.registerPerson(passport, passport, passport);
        Person remote = bank.getPersonByPassportId(passport, Bank.PersonType.REMOTE);

        bank.createAccount(passport + ":1");

        Person local = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);

        Account localAccount = local.getAccounts().get("1");
        localAccount.setAmount(localAccount.getAmount() + 100);

        Account remoteAccount = remote.getAccounts().get("1");

        assertEquals(100, localAccount.getAmount());
        assertEquals(0, remoteAccount.getAmount());
    }

    @Test
    public void test07_checkDoLocalSeePreviousRemoteChanges() throws RemoteException {
        String passport = "test07";
        bank.registerPerson(passport, passport, passport);
        Person remote = bank.getPersonByPassportId(passport, Bank.PersonType.REMOTE);

        bank.createAccount(passport + ":1");

        Account remoteAccount = remote.getAccounts().get("1");

        Person local1 = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);

        remoteAccount.setAmount(remoteAccount.getAmount() + 100);

        Person local2 = bank.getPersonByPassportId(passport, Bank.PersonType.LOCAL);

        Account localAccount1 = local1.getAccounts().get("1");
        Account localAccount2 = local2.getAccounts().get("1");

        assertEquals(localAccount2.getAmount(), remoteAccount.getAmount());
        assertEquals(localAccount1.getAmount() + 100, localAccount2.getAmount());
    }

    @Test
    public void test08_checkParallelAccountCreation() throws RemoteException {
        String passportId = "test08";
        bank.registerPerson(passportId, passportId, passportId);
        Person person = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        try {
            Phaser phaser = new Phaser(MULTIPLE_TEST_COUNT);
            IntStream.range(0, MULTIPLE_TEST_COUNT).forEach(i -> executorService.submit(() -> {
                try {
                    bank.createAccount(passportId + ":" + i);
                } catch (RemoteException e) {
                    throw new AssertionError(e);
                } finally {
                    phaser.arrive();
                }
            }));
            phaser.arriveAndAwaitAdvance();
            assertEquals(MULTIPLE_TEST_COUNT, person.getAccounts().size());
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void test09_checkParallelPersonRegistration() {
        String passportId = "test09";
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        try {
            Phaser phaser = new Phaser(MULTIPLE_TEST_COUNT);
            IntStream.range(0, MULTIPLE_TEST_COUNT).forEach(i -> executorService.submit(() -> {
                try {
                    String currentPassportId = passportId + i;
                    bank.registerPerson(currentPassportId, currentPassportId, currentPassportId);
                } catch (RemoteException e) {
                    throw new AssertionError(e);
                } finally {
                    phaser.arrive();
                }
            }));
            phaser.arriveAndAwaitAdvance();
            IntStream.range(0, MULTIPLE_TEST_COUNT).forEach(i -> {
                try {
                    assertNotNull(bank.getPersonByPassportId(passportId + i, Bank.PersonType.LOCAL));
                } catch (RemoteException e) {
                    throw new AssertionError(e);
                }
            });
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void test10_runClientIncorrectly() {
        Client.main(null);
        Client.main(new String[]{null, null, null, null, null});
        Client.main(new String[]{"Alexander", "Varfolomeev", "test10", "1", null});
        Client.main(new String[]{"Alexander", "Varfolomeev", "test10", "1", "-1_000_000_000_000_000"});
        Client.main(new String[]{"Alexander", "Varfolomeev", "test10", "1", "abc"});
    }

    private void testIncorrectParameters(Method method, Object[][] parameters) {
        assertTrue(Arrays.stream(parameters).allMatch(os -> {
            try {
                method.invoke(bank, os);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof RemoteException) {
                    throw new AssertionError(e.getTargetException());
                }
                return true;
            }
            return false;
        }));
    }

    @Test
    public void test11_checkIncorrectPersonRegistration() throws NoSuchMethodException {
        testIncorrectParameters(Bank.class.getDeclaredMethod("registerPerson", String.class, String.class, String.class),
                new Object[][]{{null, null, null}, {null, "Varfolomeev", "test11"}, {"Alexander", "Varfolomeev", null}});
    }

    @Test
    public void test12_checkIncorrectPersonGetting() throws NoSuchMethodException {
        testIncorrectParameters(Bank.class.getDeclaredMethod("getPersonByPassportId", String.class, Bank.PersonType.class),
                new Object[][]{{null, Bank.PersonType.REMOTE}, {"test12", null}});
    }

    @Test
    public void test13_checkIncorrectAccountCreation() throws RemoteException, NoSuchMethodException {
        String passportId = "test13";
        bank.registerPerson(passportId, passportId, passportId);
        testIncorrectParameters(Bank.class.getDeclaredMethod("createAccount", String.class),
                new Object[][]{{null}, {""}, {"1"}, {"1:1:1"}, {":"}, {"::"}, {"::::::"}});

    }

    @Test
    public void test14_checkIncorrectAccountGetting() throws NoSuchMethodException {
        testIncorrectParameters(Bank.class.getDeclaredMethod("getAccount", String.class),
                new Object[][]{{null}, {""}, {"1"}, {"1:1:1"}, {":"}, {"::"}, {"::::::"}});
    }

    private void test() {
        Arrays.stream(BankTests.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Test.class)).forEach(method -> {
            try {
                method.invoke(this);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        });
    }

    public static void main(String[] args) {
        try {
            beforeClass();
            new BankTests().test();
        } finally {
            afterClass();

        }
    }
}
