package idea.verlif.spring.taskservice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    /**
     * 是否启用正则名称匹配
     */
    private boolean regex = false;

    private final Map<String, Pattern> patternMap;

    public TaskConfig() {
        patternMap = new HashMap<>();
    }

    @Bean("schedulerForTask")
    @ConditionalOnMissingBean(name = "schedulerForTask")
    public ThreadPoolTaskScheduler schedulerForTask() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setThreadFactory(r -> {
            Thread t = new Thread(r);
            t.setName("task-" + t.getId());
            return t;
        });
        scheduler.setPoolSize(maxSize);
        return scheduler;
    }

    public boolean isAllowed(String value) {
        if (regex) {
            if (allowed.size() > 0) {
                for (String s : allowed) {
                    Pattern pattern = getPattern(s);
                    if (pattern.matcher(value).matches()) {
                        return true;
                    }
                }
            }
            if (blocked.size() > 0) {
                for (String s : blocked) {
                    Pattern pattern = getPattern(s);
                    if (pattern.matcher(value).matches()) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return allowed.size() == 0 && !blocked.contains(value) || allowed.contains(value);
        }
    }

    private Pattern getPattern(String key) {
        return patternMap.computeIfAbsent(key, Pattern::compile);
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

    public boolean isRegex() {
        return regex;
    }

    public void setRegex(boolean regex) {
        this.regex = regex;
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
