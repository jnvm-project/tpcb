package eu.tsp.transactions;

import eu.tsp.transactions.base.BaseBank;
import eu.tsp.transactions.distributed.DistributedBank;
import eu.tsp.transactions.distributed.JNVMBank;

public class BankFactory{

  public Bank createBaseBank(){
    return new BaseBank();
  }

  public Bank createDistributedBank(boolean isPersisted, int eviction){
    return new DistributedBank(isPersisted, eviction);
  }

  public Bank createJNVMBank(boolean isPersisted, int eviction){
    return new JNVMBank(isPersisted, eviction);
  }

}
