package ru.ifmo.rain.varfolomeev.bank;

class RemoteAccount implements Account {
    private final String id;
    private int amount;

    RemoteAccount(String id) {
        amount = 0;
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
