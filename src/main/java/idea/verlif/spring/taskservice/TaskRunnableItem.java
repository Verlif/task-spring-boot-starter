package idea.verlif.spring.taskservice;

import idea.verlif.spring.taskservice.anno.TaskTip;

/**
 * @author Verlif
 * @version 1.0
 * @date 2022/4/13 11:21
 */
public class TaskRunnableItem {

    private final Runnable target;
    private final TaskTip tip;

    public TaskRunnableItem(Runnable target, TaskTip tip) {
        this.target = target;
        this.tip = tip;
    }

    public Runnable getTarget() {
        return target;
    }

    public TaskTip getTip() {
        return tip;
    }
}
