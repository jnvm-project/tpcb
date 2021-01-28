package eu.tsp.transactions;

public interface Bank{
  void createAccount(String id) throws IllegalArgumentException;
  int getBalance(String id) throws IllegalArgumentException;
  /* transfer between two accounts, creating them if they do not exist.*/
  void performTransfer(String from, String to, int amount) throws IllegalArgumentException;
  void clear();
  void open();
  void close();
}
