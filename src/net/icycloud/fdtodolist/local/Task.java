package net.icycloud.fdtodolist.local;

public class Task {
    public long id;
    public String title;
    public String description;
    public int quadrant; // 1: 重要且紧急, 2: 重要不紧急, 3: 紧急不重要, 4: 不重要不紧急
    public int isCompleted; // 0: 未完成, 1: 已完成
    public long createdAt;
    public long dueTime; // 执行时间/时间轴时间戳

    public Task() {}

    public Task(long id, String title, String description, int quadrant, int isCompleted, long createdAt, long dueTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.quadrant = quadrant;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
        this.dueTime = dueTime;
    }
}
