package cn.chain33.jvm.interfaces;

public interface Account {
    public boolean execFrozen(String from, long amount);
    public boolean execActive(String from, long amount);
    public boolean execTransfer(String from, String to, long amount);
    //从自己冻结得钱包里面转移
    public boolean execTransferForzen(String from,String to,long amount);
    //从活跃地址A转移到B下,再冻结
    public boolean execTransferFromActiveToForzen(String from,String to,long amount);
}
