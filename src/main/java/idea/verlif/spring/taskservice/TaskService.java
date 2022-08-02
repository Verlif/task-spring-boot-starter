package idea.verlif.spring.taskservice;

import idea.verlif.spring.taskservice.anno.TaskTip;
import idea.verlif.spring.taskservice.anno.TaskTipHead;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 任务服务。<br/>
 * 任务服务会在启动时加载，在启动结束后开始执行。
 *
 * @author Verlif
 * @version 1.0
 * @date 2021/12/21 11:46
 */
@Component
public class TaskService implements ApplicationRunner {

    private final static Logger LOGGER = LogManager.getLogger(TaskService.class);

    /**
     * 待添加的任务表
     */
    private final ConcurrentHashMap<String, TaskRunnableItem> taskMap;
    /**
     * 可重复任务Map，用于控制任务进度
     */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> futureMap;
    /**
     * 是否在添加时直接执行定时任务
     */
    private boolean ready;

    @Resource(name = "schedulerForTask")
    private ThreadPoolTaskScheduler schedule;
    private final TaskConfig config;

    private final ApplicationContext context;

    public TaskService(
            @Autowired ApplicationContext context,
            @Autowired TaskConfig taskConfig
    ) {
        this.config = taskConfig;
        this.context = context;

        taskMap = new ConcurrentHashMap<>();
        futureMap = new ConcurrentHashMap<>();
        ready = false;

        Map<String, Object> map = context.getBeansWithAnnotation(TaskTip.class);
        for (Object value : map.values()) {
            if (value instanceof Runnable) {
                TaskTip tip = value.getClass().getAnnotation(TaskTip.class);
                if (tip.auto()) {
                    insert((Runnable) value, tip);
                }
            } else {
                LOGGER.warn("Class [{}] is not a runnable object, it can not been loaded!", value.getClass().getName());
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 获取公开定时方法
        Map<String, Object> beans = context.getBeansWithAnnotation(TaskTipHead.class);
        for (Object bean : beans.values()) {
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method method : methods) {
                TaskTip tip = method.getAnnotation(TaskTip.class);
                if (tip != null && tip.auto()) {
                    insert(bean, method, tip);
                }
            }
        }
        synchronized (taskMap) {
            for (TaskRunnableItem item : taskMap.values()) {
                schedule(null, item.getTarget(), item.getTip());
            }
            taskMap.clear();

            StringBuilder sb = new StringBuilder();
            Enumeration<String> enumeration = futureMap.keys();
            while (enumeration.hasMoreElements()) {
                sb.append(enumeration.nextElement()).append(", ");
            }
            if (sb.length() > 0) {
                LOGGER.info("Already loaded repeatable tasks with {} - {}", futureMap.size(), sb.substring(0, sb.length() - 2));
            }
            ready = true;
        }
    }

    /**
     * 获取任务调度器
     *
     * @return 服务内部的调度器
     */
    public ThreadPoolTaskScheduler getSchedule() {
        return schedule;
    }

    /**
     * 添加定时任务 <br/>
     * 添加的任务需要带有{@link TaskTip}注解
     *
     * @param name     任务名称
     * @param runnable 任务对象
     */
    public synchronized void insert(String name, Runnable runnable) {
        Class<? extends Runnable> cl = runnable.getClass();
        TaskTip tip = cl.getAnnotation(TaskTip.class);
        insert(name, runnable, tip);
    }

    /**
     * 添加定时任务 <br/>
     * 添加的任务需要带有{@link TaskTip}注解
     *
     * @param runnable 任务对象
     */
    public synchronized void insert(Runnable runnable) {
        Class<? extends Runnable> cl = runnable.getClass();
        TaskTip tip = cl.getAnnotation(TaskTip.class);
        insert(cl.getSimpleName(), runnable, tip);
    }

    /**
     * 添加定时任务 <br/>
     *
     * @param runnable 任务对象
     * @param tip      任务参数
     */
    public synchronized void insert(Runnable runnable, TaskTip tip) {
        insert(runnable.getClass().getSimpleName(), runnable, tip);
    }

    /**
     * 添加定时任务 <br/>
     *
     * @param runnable 任务对象
     * @param tip      任务参数
     */
    public synchronized void insert(String name, Runnable runnable, TaskTip tip) {
        Class<? extends Runnable> cl = runnable.getClass();
        if (tip == null) {
            LOGGER.warn("Runnable [{}] lack annotation '@TaskTip', it can not been loaded!", cl.getName());
        } else {
            synchronized (taskMap) {
                if (ready) {
                    schedule(null, runnable, tip);
                } else {
                    taskMap.put(name, new TaskRunnableItem(runnable, tip));
                }
            }
        }
    }

    /**
     * 添加定时任务 <br/>
     * 添加的任务需要带有{@link TaskTip}注解
     *
     * @param bean   方法的目标对象
     * @param method 任务方法
     * @param tip    任务参数
     */
    public synchronized void insert(Object bean, Method method, TaskTip tip) {
        if (tip == null) {
            LOGGER.warn("Runnable [{}] lack annotation '@TaskTip', it can not been loaded!", method.getName());
        } else {
            synchronized (taskMap) {
                Runnable runnable = new ScheduledMethodRunnable(bean, method);
                if (ready) {
                    schedule(null, runnable, tip);
                } else {
                    taskMap.put(method.getName(), new TaskRunnableItem(runnable, tip));
                }
            }
        }
    }

    /**
     * 执行任务
     *
     * @param runnable 任务对象
     */
    public void execute(Runnable runnable) {
        schedule.execute(runnable);
    }

    /**
     * 定时执行单次任务
     *
     * @param runnable  任务对象
     * @param startTime 任务开始时间
     */
    public void execute(Runnable runnable, Date startTime) {
        schedule.schedule(runnable, startTime);
    }

    /**
     * 延时执行任务
     *
     * @param runnable 任务对象
     * @param delay    延时时间（单位毫秒）
     */
    public void delay(Runnable runnable, long delay) {
        schedule.schedule(runnable, new Date(System.currentTimeMillis() + delay));
    }

    /**
     * 延时执行任务
     *
     * @param runnable 任务对象
     * @param delay    延时时间
     * @param unit     延时时间单位
     */
    public void delay(Runnable runnable, long delay, TimeUnit unit) {
        delay(runnable, unit.toMillis(delay));
    }

    /**
     * 取消定时任务
     *
     * @param name 任务名称
     * @return 是否取消成功
     */
    public synchronized boolean cancel(String name) {
        ScheduledFuture<?> future = futureMap.get(name);
        if (future == null) {
            return false;
        }
        return future.cancel(true);
    }

    /**
     * 添加定时任务
     *
     * @param defaultName 任务名称；null则使用注解提供的名称规则
     * @param runnable    任务对象
     */
    private void schedule(String defaultName, Runnable runnable) {
        TaskTip tip = runnable.getClass().getAnnotation(TaskTip.class);
        schedule(defaultName, runnable, tip);
    }

    /**
     * 添加定时任务
     *
     * @param defaultName 任务名称；null则使用注解提供的名称规则
     * @param runnable    任务对象
     * @param tip         任务参数
     */
    private void schedule(String defaultName, Runnable runnable, TaskTip tip) {
        Class<?> cl = runnable.getClass();
        String name = defaultName != null ? defaultName : tip.name().length() == 0 ? cl.getSimpleName() + cl.hashCode() : tip.name();
        // 检测任务是否重复
        if (futureMap.containsKey(name)) {
            LOGGER.warn("Already exist task {}!!!", name);
            return;
        }
        // 检测任务是否可添加
        if (config.isAllowed(name)) {
            switch (tip.type()) {
                case CRON: {
                    if (tip.cron().length() == 0) {
                        LOGGER.warn("Can not load runnable {}, it is needed to set cron", name);
                        break;
                    }
                    ScheduledFuture<?> future = schedule.schedule(runnable, new CronTrigger(tip.cron()));
                    if (future != null) {
                        futureMap.put(name, future);
                    }
                    break;
                }
                case REPEAT_DELAY: {
                    if (tip.interval() == 0) {
                        LOGGER.warn("Can not load runnable {}, it is needed to set interval", name);
                        break;
                    }
                    ScheduledFuture<?> future = schedule.scheduleWithFixedDelay(runnable, new Date(System.currentTimeMillis() + tip.unit().toMillis(tip.delay())), tip.unit().toMillis(tip.interval()));
                    futureMap.put(name, future);
                    break;
                }
                case REPEAT_RATE: {
                    if (tip.interval() == 0) {
                        LOGGER.warn("Can not load runnable {}, it is needed to set interval", name);
                        break;
                    }
                    ScheduledFuture<?> future = schedule.scheduleAtFixedRate(runnable, new Date(System.currentTimeMillis() + tip.unit().toMillis(tip.delay())), tip.unit().toMillis(tip.interval()));
                    futureMap.put(name, future);
                    break;
                }
                case DELAY: {
                    delay(runnable, tip.unit().toMillis(tip.delay()));
                    break;
                }
                default:
                    LOGGER.warn("No such task type {} for {}", tip.type(), name);
            }
        } else {
            LOGGER.warn("Task - {} is disabled.", name);
        }
    }

}
