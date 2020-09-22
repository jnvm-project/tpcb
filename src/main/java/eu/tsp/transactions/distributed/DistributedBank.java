package eu.tsp.transactions.distributed;

import eu.tsp.transactions.Account;
import eu.tsp.transactions.Bank;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionType;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.transaction.TransactionManager;
import java.util.concurrent.ConcurrentMap;

public class DistributedBank implements Bank{

    private static final String STORAGE_PATH = "/tmp/bank";
    private static final Logger LOG = LoggerFactory.getLogger(DistributedBank.class);

    private DefaultCacheManager cacheManager;
    private ConcurrentMap<Integer,Account> accounts;

    public DistributedBank(boolean isPersisted, int eviction){
        GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
        gbuilder.transport().addProperty("configurationFile", "jgroups.xml");
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.DIST_SYNC);
        builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.PESSIMISTIC);

        // persistence
        if (isPersisted) {
            SingleFileStoreConfigurationBuilder storeConfigurationBuilder= builder.persistence().addSingleFileStore();
            storeConfigurationBuilder
                    .location(STORAGE_PATH)
                    .persistence().passivation(false); // write-through
            // cache eviction
            if (eviction>0) {
                LOG.info("Eviction size set to "+ eviction);
                builder.memory()
                        .evictionStrategy(EvictionStrategy.REMOVE)
                        .evictionType(EvictionType.COUNT)
                        .storageType(StorageType.OBJECT)
                        .size(eviction);
            }
        }

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
            createAccount(from);
        }

        if (!this.accounts.containsKey(to)) {
            createAccount(to);
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
