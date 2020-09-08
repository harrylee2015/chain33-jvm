package cn.chain33.jvm.interfaces;

//存储、加载数据 接口
public interface Storage<T> {
    T loadData();
    boolean saveData();
}
