package net.icycloud.fdtodolist.local;

public class Note {
    public long id;
    public String content;
    public long createdAt;

    public Note() {}

    public Note(long id, String content, long createdAt) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }
}
