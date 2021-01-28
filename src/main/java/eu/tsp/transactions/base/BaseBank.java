package eu.tsp.transactions.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

import eu.tsp.transactions.Bank;
import eu.tsp.transactions.Account;

public class BaseBank implements Bank{
  private Map<String,Account> accounts;

  public BaseBank(){
    this.accounts = new ConcurrentHashMap<>();
  }

  @Override
  public void createAccount(String id) throws IllegalArgumentException{
    if (this.accounts.containsKey(id)) {
      throw new IllegalArgumentException("account already existing: "+id);
    }
    accounts.put(id, Account.createAccount(false,Integer.parseInt(id),0));
  }

  @Override
  public int getBalance(String id) throws IllegalArgumentException{
    if (!this.accounts.containsKey(id)) {
      throw new IllegalArgumentException("account not existing: "+id);
    }
    Account account = accounts.get(id);
    return account.getBalance();
  }

  @Override
  public void performTransfer(String from, String to, int amount){
    if (!this.accounts.containsKey(from)) {
      createAccount(from);
    }

    if (!this.accounts.containsKey(to)) {
      createAccount(to);
    }

    Account fromAccount = accounts.get(from);
    Account toAccount = accounts.get(to);

    fromAccount.setBalance(fromAccount.getBalance()-amount);
    toAccount.setBalance(toAccount.getBalance()+amount);
  }

  @Override
  public void clear(){
    this.accounts.clear();
  }

  @Override
  public void open(){}

  @Override
  public void close(){}

}
