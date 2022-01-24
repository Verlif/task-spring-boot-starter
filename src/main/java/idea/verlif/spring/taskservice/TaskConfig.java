package idea.verlif.spring.taskservice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务配置
 *
 * @author Verlif
 * @version 1.0
 * @date 2021/12/21 14:38
 */
@Configuration
@ConfigurationProperties(prefix = "station.task")
public class TaskConfig {

    /**
     * 允许的定时任务列表。存在值时，屏蔽列表无效。
     */
    private List<String> allowed = new ArrayList<>();

    /**
     * 屏蔽的定时任务列表。当允许列表存在值时，屏蔽列表无效。
     */
    private List<String> blocked = new ArrayList<>();

    /**
     * 最大线程池数
     */
    private Integer maxSize = 20;

    @Bean
    @ConditionalOnMissingBean(ThreadPoolTaskScheduler.class)
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadFactory(Thread::new);
        scheduler.setPoolSize(maxSize);
        return scheduler;
    }

    public boolean isAllowed(String value) {
        return allowed.size() == 0 && !blocked.contains(value) || allowed.contains(value);
    }

    public List<String> getAllowed() {
        return allowed;
    }

    public List<String> getBlocked() {
        return blocked;
    }

    public void setAllowed(List<String> allowed) {
        this.allowed = allowed;
    }

    public void setBlocked(List<String> blocked) {
        this.blocked = blocked;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    public void addAllowed(String allowed) {
        if (this.allowed == null) {
            this.allowed = new ArrayList<>();
        }
        this.allowed.add(allowed);
    }

    public void addBlocked(String blocked) {
        if (this.blocked == null) {
            this.blocked = new ArrayList<>();
        }
        this.blocked.add(blocked);
    }
}
