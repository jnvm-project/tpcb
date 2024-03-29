package eu.tsp.transactions;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapByteArray;
import eu.telecomsudparis.jnvm.offheap.MemoryBlockHandle;

public class OffHeapAccount extends OffHeapObjectHandle implements Account {

  private static final long CLASS_ID = OffHeap.Klass.registerUserKlass(OffHeapAccount.class);

  /* PMEM Layout :
   *  | Index | Offset | Bytes | Name    |
   *  |-------+--------+-------+---------|
   *  | 0     | 0      | 4     | id      |
   *  | 1     | 4      | 4     | balance |
   *  | 2     | 8      | 8     | weight  |
   *  end: 8 bytes
   */
  private static final long[] offsets = { 0L, 4L, 8L };
  private static final long SIZE = 2 * Integer.SIZE + Long.SIZE;

  public OffHeapAccount(int id, int balance){
    super();
    setIntegerField(offsets[0], id);
    setIntegerField(offsets[1], balance);
  }

  public OffHeapAccount(int id, int balance, byte[] weight){
    super();
    setIntegerField(offsets[0], id);
    setIntegerField(offsets[1], balance);
    setHandleField(offsets[2], new OffHeapByteArray(weight));
  }

  public OffHeapAccount(long offset){
    super(offset);
  }

  public OffHeapAccount(MemoryBlockHandle block){
    this(block.getOffset());
  }

  public OffHeapAccount(Void v, long offset){
    this(offset);
  }

  public long size() { return SIZE; }
  public long classId() { return CLASS_ID; }
  public void descend() {
    OffHeap.gcMark(OffHeap.baseAddr() + getLongField(offsets[2]));
  }

  public int getId(){
    return getIntegerField(offsets[0]);
  }

  public int getBalance(){
    return getIntegerField(offsets[1]);
  }

  public void setBalance(int balance){
    setIntegerField(offsets[1], balance);
  }

}
