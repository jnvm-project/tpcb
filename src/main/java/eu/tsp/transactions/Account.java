package eu.tsp.transactions;

import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;

public interface Account {

  public int getId();
  public int getBalance();
  public void setBalance(int balance);

  public static Account createAccount(boolean jnvm, int id, int balance) {
    Random random = ThreadLocalRandom.current();
    byte[] r = new byte[128];
    random.nextBytes(r);
    String s = new String(r);
    return (jnvm) ? new OffHeapAccount(id, balance, s)
                  : new VolatileAccount(id, balance, s);
  }

}
