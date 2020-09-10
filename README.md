# chain33-jvm 

说明：这是一个在chain33中使用java进行智能合约开发的实验性项目

## 交易执行流程

   1. 交易过来,先解析交易,获取合约名,调用方法及参数
   2. exector模块启动jvm,加载合约相应的jar包,调用合约中相应的方法
   
   - [x]如何调用jar包中相应的方法，是通过main函数还是其他容器方式？
   
   

## 查询请求执行流程
   1. rpc请求过来,解析请求,获取合约名,获取查询方法及参数
   2. exector模块启动jvm,加载合约相应的jar包,调用合约中相应的查询方法

## 合约示例
- [java开发的guess猜数字游戏合约](src/main/java/cn/chain33/jvm/dapp/guess/README.md)