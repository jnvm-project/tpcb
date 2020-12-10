package eu.tsp.transactions;


public interface Account {

  public int getId();
  public int getBalance();
  public void setBalance(int balance);

  public static Account createAccount(boolean jnvm, int id, int balance) {
    return (jnvm) ? new OffHeapAccount(id, balance)
                  : new VolatileAccount(id, balance);
  }

}
