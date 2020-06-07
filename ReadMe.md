# EJoker

---

### 缘起

本人不才，通过java的技术栈，把enode的整体设计抄来过来。思路、框架设计都来自汤雪华ENode(C#)，并在基础层使用了大量java自身的特性。


> ENode是一个开源的应用开发框架，为开发人员提供了一套完整的基于DDD+CQRS+ES+(in-memory)+EDA架构风格的解决方案。
> 
> 汤雪华博客: http://www.cnblogs.com/netfocus
> 
> Enode: https://github.com/tangxuehua/enode

---

### 执念

- Java 没有官方的协程方案也没有async/await语法糖，所以EJoker底层异步模型使用两个方案 `伪异步` 以及 `Quasar fiber`
- 没使用第三方IoC框架和组件，只因当初转行入java，撸了SpringContext，自己实现IoC算交一份作业吧。
- 底层组件继续优化，工作中也用到，当然，是思路，用上已有的代码是为了省时间
- 希望持续优化和升级

### 吐槽
	
伪异步方式由EJoker内部提供线程池，提交任务时检查当前线程是否为线程池中的线程，或者任务队列是否有排队任务，如果满足条件，则由提交者线程直接执行当前提交的任务。伪异步的代码实现其实很傻逼的

Quasar协程为第三方java协程方案，中文资料不太懂，慎用。Quasar协程下写代码有一些细节需要注意 详细看`http://docs.paralleluniverse.co/quasar/#suspendable-libraries`。 Quasar下出的问题千奇百怪，日志输出犹如天书，目测是JVM底层知识缺乏的原因。

默认获得就是 `伪异步` 版本，测试的时候， `伪异步` 性能非常优秀。lsquasar纯粹为了做验证异步功能的，期望待到官家真出了异步，就换上他们的语法糖。

> > 需要看看怎么得到quasar版本的，可以看看demo 的配置
> > https://github.com/kimffy24/EJoker_demo/blob/master/pom.xml
> > 这里，当变量useQuasar=1时，会启动对应的ant任务，不作细说