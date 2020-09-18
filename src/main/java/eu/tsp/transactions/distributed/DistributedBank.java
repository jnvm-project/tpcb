package eu.tsp.transactions.distributed;

import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;

import javax.transaction.TransactionManager;
import javax.transaction.RollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.xa.XAException;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;

import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;

import eu.tsp.transactions.Bank;
import eu.tsp.transactions.Account;

public class DistributedBank implements Bank{

  private DefaultCacheManager cacheManager;
  private ConcurrentMap<Integer,Account> accounts;

  public DistributedBank(){
    GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
    gbuilder.transport().addProperty("configurationFile", "jgroups.xml");
    ConfigurationBuilder builder = new ConfigurationBuilder();
    builder.clustering().cacheMode(CacheMode.DIST_SYNC);
    builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.PESSIMISTIC);
    cacheManager = new DefaultCacheManager(gbuilder.build(),builder.build());
    accounts = cacheManager.getCache();
  }

  @Override
  public void createAccount(int id) throws IllegalArgumentException{
    if (this.accounts.containsKey(id)) {
      throw new IllegalArgumentException("account already existing: "+id);
    }
    accounts.put(id, new Account(id,0));
  }

  @Override
  public int getBalance(int id) throws IllegalArgumentException{
    if (!this.accounts.containsKey(id)) {
      throw new IllegalArgumentException("account not existing: "+id);
    }
    Account account = accounts.get(id);
    return account.getBalance();
  }

  @Override
  public void performTransfer(int from, int to, int amount){ 
    if (!this.accounts.containsKey(from)) {
      throw new IllegalArgumentException("account not existing: "+from);
    }
    
    if (!this.accounts.containsKey(to)) {
      throw new IllegalArgumentException("account not existing: "+to);
    }

    boolean retry=false;
    do{
      try{
	TransactionManager tm = ((Cache)accounts).getAdvancedCache().getTransactionManager();
	tm.begin();
	Account fromAccount = accounts.get(from);
	Account toAccount = accounts.get(to);    
	fromAccount.setBalance(fromAccount.getBalance()-amount);
	toAccount.setBalance(toAccount.getBalance()+amount);
	accounts.put(fromAccount.getId(),fromAccount);
	accounts.put(toAccount.getId(),toAccount);
	tm.commit();
      }catch(Throwable e){
	retry=true;
      }
    }while(retry);
  }

  @Override
  public void clear(){
    this.accounts.clear();
  }
  
  @Override
  public void open(){
    this.cacheManager.start();
  }

  @Override
  public void close(){
    this.cacheManager.stop();
  }
   
}
