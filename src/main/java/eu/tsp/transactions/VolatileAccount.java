package eu.tsp.transactions;

import java.io.Serializable;

public class VolatileAccount implements Account, Serializable {
  private int id;
  private int balance;
  private String weight;

  public VolatileAccount(int id, int balance){
    this.id = id;
    this.balance = balance;
    this.weight = null;
  }

  public VolatileAccount(int id, int balance, String weight){
    this.id = id;
    this.balance = balance;
    this.weight = weight;
  }

  public int getId(){
    return this.id;
  }

  public int getBalance(){
    return this.balance;
  }

  public void setBalance(int balance){
    this.balance = balance;
  }

}
