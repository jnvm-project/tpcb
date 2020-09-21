package eu.tsp.transactions;

public interface Bank{
  void createAccount(int id) throws IllegalArgumentException;
  int getBalance(int id) throws IllegalArgumentException;
  /* transfer between two accounts, creating them if they do not exist.*/
  void performTransfer(int from, int to, int amount) throws IllegalArgumentException;
  void clear();
  void open();
  void close();
}
