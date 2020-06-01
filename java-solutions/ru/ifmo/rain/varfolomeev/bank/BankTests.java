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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BankTests {
    private static final int MULTIPLE_TEST_COUNT = 10000;
    private static final String PERSON_FIRST_NAME = "Alexander";
    private static final String PERSON_LAST_NAME = "Varfolomeev";

    private static Bank bank;
    private static Server SERVER = new Server();
    private static Registry registry;


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
        String passportId = "test01";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);

        Person remotePerson = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        Person localPerson = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);

        assertNotNull(remotePerson);
        assertEquals(remotePerson.getFirstName(), PERSON_FIRST_NAME);
        assertEquals(remotePerson.getLastName(), PERSON_LAST_NAME);
        assertEquals(remotePerson.getPassportId(), passportId);

        assertNotNull(localPerson);
        assertEquals(localPerson.getFirstName(), PERSON_FIRST_NAME);
        assertEquals(localPerson.getLastName(), PERSON_LAST_NAME);
        assertEquals(localPerson.getPassportId(), passportId);
    }

    @Test
    public void test02_checkAccountCreation() throws RemoteException {
        String passportId = "test02";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);

        bank.createAccount("test02:1");

        Person remotePerson = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        Person localPerson = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);
        assertNotNull(localPerson.getAccount("1"));
        assertNotNull(remotePerson.getAccount("1"));
    }

    private void runClient(int testNumber, String[] args) {
        System.out.println(String.format("test Client #%d:%n=================================", testNumber));
        Client.main(args);
        System.out.println();
    }

    @Test
    public void test03_testClient() throws RemoteException {
        assertNull(bank.getPersonByPassportId("test03", Bank.PersonType.REMOTE));
        runClient(1, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test03", "1", "100"});
        Person person = bank.getPersonByPassportId("test03", Bank.PersonType.REMOTE);
        assertNotNull(person);
        assertEquals(PERSON_FIRST_NAME, person.getFirstName());
        assertEquals(PERSON_LAST_NAME, person.getLastName());
        assertEquals("test03", person.getPassportId());
        assertNotNull(bank.getAccount("test03:1"));
        assertEquals(100, bank.getAccount("test03:1").getAmount());

        runClient(2, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test03", "1", Integer.toString(Integer.MIN_VALUE)});
        assertEquals(-2147483548, bank.getAccount("test03:1").getAmount());
        runClient(3, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test03", "1", Integer.toString(Integer.MAX_VALUE)});
        assertEquals(99, bank.getAccount("test03:1").getAmount());

        assertNull(bank.getAccount("test03:2"));
        runClient(4, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test03", "2", Integer.toString(Integer.MAX_VALUE)});
        assertNotNull(bank.getAccount("test03:2"));
        assertEquals(Integer.MAX_VALUE, bank.getAccount("test03:2").getAmount());

        assertNull(bank.getAccount("test03:3"));
        runClient(5, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test03", "3", Integer.toString(Integer.MIN_VALUE)});
        assertNotNull(bank.getAccount("test03:3"));
        assertEquals(Integer.MIN_VALUE, bank.getAccount("test03:3").getAmount());

        assertNull(bank.getAccount("test03:4"));
        runClient(6, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test03", "4", Integer.toString(0)});
        assertNotNull(bank.getAccount("test03:4"));
        assertEquals(0, bank.getAccount("test03:4").getAmount());
    }

    @Test
    public void test04_checkDoRemoteSeeChanges() throws RemoteException {
        String passportId = "test04";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);

        Person remotePerson1 = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        Person remotePerson2 = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);

        bank.createAccount(passportId + ":1");
        assertEquals(1, remotePerson1.getAccounts().size());
        assertEquals(1, remotePerson2.getAccounts().size());

        remotePerson1.getAccount("1").setAmount(100);
        assertEquals(remotePerson1.getAccount("1").getAmount(), remotePerson2.getAccount("1").getAmount());
    }

    @Test
    public void test05_checkDoLocalNotSeeNextRemoteChanges() throws RemoteException {
        String passportId = "test05";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);

        Person localPerson1 = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);
        Person localPerson2 = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);

        bank.createAccount(passportId + ":1");
        bank.createAccount(passportId + ":2");

        assertEquals(0, localPerson1.getAccounts().size());
        assertEquals(0, localPerson2.getAccounts().size());

        Person localPerson3 = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);

        assertEquals(0, localPerson1.getAccounts().size());
        assertEquals(2, localPerson3.getAccounts().size());
    }

    @Test
    public void test06_checkRemoteCantSeeLocalChanges() throws RemoteException {
        String passportId = "test06";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);
        Person remotePerson = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);

        bank.createAccount(passportId + ":1");

        Person localPerson = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);

        Account localAccount = localPerson.getAccount("1");
        localAccount.setAmount(localAccount.getAmount() + 100);

        Account remoteAccount = remotePerson.getAccount("1");

        assertEquals(100, localAccount.getAmount());
        assertEquals(0, remoteAccount.getAmount());
    }

    @Test
    public void test07_checkDoLocalSeePreviousRemoteChanges() throws RemoteException {
        String passportId = "test07";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);
        Person remotePerson = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);

        bank.createAccount(passportId + ":1");

        Account remoteAccount = remotePerson.getAccount("1");

        Person localPerson1 = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);

        remoteAccount.setAmount(remoteAccount.getAmount() + 100);

        Person localPerson2 = bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);

        Account localAccount1 = localPerson1.getAccount("1");
        Account localAccount2 = localPerson2.getAccount("1");

        assertEquals(localAccount2.getAmount(), remoteAccount.getAmount());
        assertEquals(localAccount1.getAmount() + 100, localAccount2.getAmount());
    }

    @Test
    public void test08_checkLocalAmountChanges() throws RemoteException {
        String passportId = "test08";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);
        Person remotePerson = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        LocalPerson localPerson = (LocalPerson) bank.getPersonByPassportId(passportId, Bank.PersonType.LOCAL);

        bank.createAccount(passportId + ":1");
        assertNotNull(remotePerson.getAccount("1"));
        assertNull(localPerson.getAccount("1"));

        Account localAccount = localPerson.createAccount("1");
        localAccount.addAmount(100);

        assertEquals(0, remotePerson.getAccount("1").getAmount());
        assertEquals(100, localPerson.getAccount("1").getAmount());

    }

    private void runOperationParallel(IntFunction<Runnable> operation) {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            Phaser phaser = new Phaser(MULTIPLE_TEST_COUNT);
            IntStream.range(0, MULTIPLE_TEST_COUNT).forEach(i -> executorService.submit(() -> {
                try {
                    operation.apply(i).run();
                } finally {
                    phaser.arrive();
                }
            }));
            phaser.awaitAdvance(0);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void test09_checkParallelPersonRegistration() {
        String passportId = "test09";
        runOperationParallel(i -> () -> {
            try {
                bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId + i);
            } catch (RemoteException ignored) {
                //
            }
        });
        IntStream.range(0, MULTIPLE_TEST_COUNT).forEach(i -> {
            try {
                assertNotNull(bank.getPersonByPassportId(passportId + i, Bank.PersonType.LOCAL));
            } catch (RemoteException e) {
                throw new AssertionError(e);
            }
        });
    }

    @Test
    public void test10_checkParallelAccountCreation() throws RemoteException {
        String passportId = "test10";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);
        Person person = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        runOperationParallel(i -> () -> {
            try {
                bank.createAccount(passportId + ":" + i);
            } catch (RemoteException ignored) {
                //
            }
        });
        assertEquals(MULTIPLE_TEST_COUNT, person.getAccounts().size());
    }

    @Test
    public void test11_checkParallelAmountAdding() throws RemoteException {
        String passportId = "test11";
        bank.registerPerson(PERSON_FIRST_NAME, PERSON_LAST_NAME, passportId);
        Person person = bank.getPersonByPassportId(passportId, Bank.PersonType.REMOTE);
        Account account = bank.createAccount(passportId + ":1");
        runOperationParallel(i -> () -> {
            try {
                account.addAmount(1);
            } catch (RemoteException ignored) {
                //
            }
        });
        assertEquals(MULTIPLE_TEST_COUNT, account.getAmount());
    }

    @Test
    public void test12_runClientIncorrectly() {
        runClient(1, null);
        runClient(2, new String[]{null, null, null, null, null});
        runClient(3, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test10", "1", null});
        runClient(4, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test10", "1", "-1_000_000_000_000_000"});
        runClient(5, new String[]{PERSON_FIRST_NAME, PERSON_LAST_NAME, "test10", "1", "abc"});
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
    public void test13_checkIncorrectPersonRegistration() throws NoSuchMethodException {
        testIncorrectParameters(Bank.class.getDeclaredMethod("registerPerson", String.class, String.class, String.class),
                new Object[][]{{null, null, null}, {null, PERSON_LAST_NAME, "test11"}, {PERSON_FIRST_NAME, PERSON_LAST_NAME, null}});
    }

    @Test
    public void test14_checkIncorrectPersonGetting() throws NoSuchMethodException {
        testIncorrectParameters(Bank.class.getDeclaredMethod("getPersonByPassportId", String.class, Bank.PersonType.class),
                new Object[][]{{null, Bank.PersonType.REMOTE}, {"test12", null}});
    }

    @Test
    public void test15_checkIncorrectAccountCreation() throws RemoteException, NoSuchMethodException {
        String passportId = "test13";
        bank.registerPerson(passportId, passportId, passportId);
        testIncorrectParameters(Bank.class.getDeclaredMethod("createAccount", String.class),
                new Object[][]{{null}, {""}, {"1"}, {"1:"}, {"1:1:1"}, {":"}, {"::"}, {"::::::"}});

    }

    @Test
    public void test16_checkIncorrectAccountGetting() throws NoSuchMethodException {
        testIncorrectParameters(Bank.class.getDeclaredMethod("getAccount", String.class),
                new Object[][]{{null}, {""}, {"1"}, {"1:"}, {"1:1:1"}, {":"}, {"::"}, {"::::::"}});
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
