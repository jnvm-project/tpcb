package eu.tsp.transactions.distributed;

import eu.tsp.transactions.Account;
import eu.tsp.transactions.OffHeapAccount;
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
import org.infinispan.eviction.EvictionType;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.transaction.TransactionManager;
import java.util.concurrent.ConcurrentMap;
import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;

public class JNVMBank implements Bank {

    private static final Logger LOG = LoggerFactory.getLogger(JNVMBank.class);

    private DefaultCacheManager cacheManager;
    private ConcurrentMap<OffHeapString,OffHeapAccount> accounts;
    private boolean isPersisted;

    public JNVMBank(boolean isPersisted, int eviction){
        GlobalConfigurationBuilder gbuilder = (new GlobalConfigurationBuilder()).nonClusteredDefault();
        gbuilder.defaultCacheName("bank");
/*
        gbuilder.serialization()
            .marshaller(new JavaSerializationMarshaller())
            .whiteList()
            .addRegexps("eu.tsp.transactions.Account");
*/
//        gbuilder.transport().addProperty("configurationFile", "jgroups.xml");
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.LOCAL);
        builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL).lockingMode(LockingMode.PESSIMISTIC);

        // persistence
        if (isPersisted) {

            LOG.info("Persistence on");
            builder.persistence()
                .addStore(eu.telecomsudparis.jnvm.infinispan.persistence.configuration.JNVMStoreConfigurationBuilder.class)
                .preload(true)
                .fetchPersistentState(true)
                .purgeOnStartup(false)
                .shared(false);

            LOG.info("Write-through activated");
            builder.persistence()
                .passivation(false); // write-through

            // cache eviction
            if (eviction>0) {
                LOG.info("Eviction size set to "+ eviction);
                builder.memory()
//                        .evictionStrategy(EvictionStrategy.REMOVE)
                        .evictionType(EvictionType.COUNT)
//                         .storageType(StorageType.OBJECT)
                        .size(eviction);
            }
        }

        isPersisted = isPersisted;
        OffHeap.finishInit();
        cacheManager = new DefaultCacheManager(gbuilder.build(),builder.build());
        accounts = cacheManager.getCache();
        ((Cache) accounts).start();
    }

    @Override
    public void createAccount(String id) throws IllegalArgumentException{
        if (this.accounts.containsKey(id)) {
            throw new IllegalArgumentException("account already existing: "+id);
        }
        accounts.put(new OffHeapString(id), (OffHeapAccount) Account.createAccount(true,Integer.parseInt(id),0));
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
    public void performTransfer(String from, String to, int amount) throws IllegalArgumentException{
        if (!this.accounts.containsKey(from)) {
            if (isPersisted) {
                throw new IllegalArgumentException("account not existing: "+from);
            } else {
                createAccount(from);
            }
        }

        if (!this.accounts.containsKey(to)) {
            if (isPersisted) {
                throw new IllegalArgumentException("account not existing: "+to);
            } else {
                createAccount(to);
            }
        }

        boolean retry=false;
        do{
            try{
                OffHeap.startRecording();
                TransactionManager tm = ((Cache)accounts).getAdvancedCache().getTransactionManager();
                tm.begin();
                Account fromAccount = accounts.get(from);
                Account toAccount = accounts.get(to);
                fromAccount.setBalance(fromAccount.getBalance()-amount);
                toAccount.setBalance(toAccount.getBalance()+amount);
                //accounts.put(fromAccount.getId(),fromAccount);
                //accounts.put(toAccount.getId(),toAccount);
                tm.commit();
                OffHeap.stopRecording();
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
