package idea.verlif.spring.taskservice.anno;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Verlif
 * @version 1.0
 * @date 2022/1/5 10:03
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface TaskTipHead {

    /**
     * 开启任务的环境
     *
     * @return 环境标签数组
     */
    String[] profiles() default {};

}
