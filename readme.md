# TaskService

定时任务与多线程任务管理服务。  
简化定时任务方式，并集中管理定时任务，便于动态增加、关闭定时任务与多线程任务。  
有允许名单与屏蔽名单配置，不修改代码即可对定时任务进行控制。

## 使用

1. 启用任务管理服务

在任意配置类上使用`@EnableTaskService`注解启用任务管理服务

2. 制定定时任务

本项目中的定时任务由`TaskService`统一维护，开发者可以通过以下注解标记定时任务：
- `TaskTip`
  - 标记在类上时，需要此类实现了`Runnable`接口，否则不会生效。
  - 标记在方法上时，需要在此类上添加`TaskTipHead`注解以声明此类中的`TaskTip`方法生效。每个`TaskTipHead`只负责一个类的声明。

例如以下代码就会添加3个延时任务（由`@TaskTipHead`声明`task2`与`task3`任务生效，`TaskTest`类上的`TaskTip`声明此类也是一个定时任务）：

```java
@TaskTipHead
@TaskTip(value = "test", type = TaskType.DELAY, delay = 2000)
public class TaskTest implements Runnable {

    @Override
    public void run() {
        PrintUtils.print(Level.CONFIG, "任务开始");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PrintUtils.print(Level.CONFIG, "任务结束");
    }

    @TaskTip(value = "task2", type = TaskType.DELAY, delay = 1000)
    private String task2() {
        System.out.println("say hello");
        return hello();
    }
    
    private String hello() {
        return "hello";
    }

    @TaskTip(value = "task3", type = TaskType.DELAY, delay = 1000)
    public void task3() {
        PrintUtils.print(Level.CONFIG, "task3 is running!");
    }
}
```

需要注意的是，对于不同类型的任务，所需的注解参数是不同的。
- `CRON`需要`cron()`
- `REPEAT_DELAY`和`REPEAT_RATE`需要`interval()`，可选`delay()`和`unit()`
- `DELAY`需要`delay()`，可选`unit()`
- `auto()`表示了任务是否自动添加到`TaskService`中。

## 添加依赖

1. 添加Jitpack仓库源

maven

```xml
<repositories>
   <repository>
       <id>jitpack.io</id>
       <url>https://jitpack.io</url>
   </repository>
</repositories>
```

2. 添加依赖

maven

```xml
<dependencies>
    <dependency>
        <groupId>com.github.Verlif</groupId>
        <artifactId>task-spring-boot-starter</artifactId>
        <version>2.6.6-1.3</version>
    </dependency>
</dependencies>
```

------

注意，任务表中不允许出现同名任务，否则会添加不进去。可以在日志中发现未能添加的任务。

## 手动执行任务

```java
@Autowired
private TaskService taskService;
private Runnable task = new DemoRunnable();

public void test() {
    // 使用注解名称或默认名称添加可重复任务
    taskService.insert(task);
    // 使用动态名称添加可重复任务
    taskService.insert("name", task);
    // 取消名为[name]的任务
    taskService.cancel("name");
    // 2000毫秒后执行任务
    taskService.delay(task, 2000);
}
```

## 配置

在`application.yml`中通过以下配置限制任务表。被限制的任务是无法自动或手动添加到任务表中的。

```yaml
station:
  # 可重复任务配置
  task:
    # 执行任务的最大线程数，默认20
    maxSize: 2
    # 是否开启正则名称匹配，对allowed与blocked参数生效。默认false
    regex: false
    # 允许的可重复任务列表。格式为yml的列表格式
    allowed:
      - name1
      - name2
    # 屏蔽的可重复任务列表
    blocked:
      - name3
      - name4
```

请注意，`allowed`的优先级高于`blocked`。