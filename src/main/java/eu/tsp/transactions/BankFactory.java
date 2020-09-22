package eu.tsp.transactions;

import eu.tsp.transactions.base.BaseBank;
import eu.tsp.transactions.distributed.DistributedBank;

public class BankFactory{

  public Bank createBaseBank(){
    return new BaseBank();
  }

  public Bank createDistributedBank(boolean isPersisted, int eviction){
    return new DistributedBank(isPersisted, eviction);
  }

}
