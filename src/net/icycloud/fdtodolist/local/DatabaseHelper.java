package net.icycloud.fdtodolist.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "clean_todo.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_TASK = "local_task";
    public static final String COL_TASK_ID = "_id";
    public static final String COL_TASK_TITLE = "title";
    public static final String COL_TASK_DESC = "description";
    public static final String COL_TASK_QUADRANT = "quadrant";
    public static final String COL_TASK_IS_COMPLETED = "is_completed";
    public static final String COL_TASK_CREATED_AT = "created_at";
    public static final String COL_TASK_DUE_TIME = "due_time";

    public static final String TABLE_NOTE = "local_note";
    public static final String COL_NOTE_ID = "_id";
    public static final String COL_NOTE_CONTENT = "content";
    public static final String COL_NOTE_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTaskTable = "CREATE TABLE " + TABLE_TASK + " (" +
                COL_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TASK_TITLE + " TEXT NOT NULL, " +
                COL_TASK_DESC + " TEXT, " +
                COL_TASK_QUADRANT + " INTEGER DEFAULT 1, " +
                COL_TASK_IS_COMPLETED + " INTEGER DEFAULT 0, " +
                COL_TASK_CREATED_AT + " INTEGER DEFAULT 0, " +
                COL_TASK_DUE_TIME + " INTEGER DEFAULT 0" +
                ")";

        String createNoteTable = "CREATE TABLE " + TABLE_NOTE + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_CONTENT + " TEXT NOT NULL, " +
                COL_NOTE_CREATED_AT + " INTEGER DEFAULT 0" +
                ")";

        db.execSQL(createTaskTable);
        db.execSQL(createNoteTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_TASK + " ADD COLUMN " + COL_TASK_DUE_TIME + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist
            }
        }
    }

    // --- Task CRUD ---
    public long insertTask(String title, String desc, int quadrant, long dueTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TASK_TITLE, title);
        cv.put(COL_TASK_DESC, desc != null ? desc : "");
        cv.put(COL_TASK_QUADRANT, quadrant);
        cv.put(COL_TASK_IS_COMPLETED, 0);
        long now = System.currentTimeMillis();
        cv.put(COL_TASK_CREATED_AT, now);
        cv.put(COL_TASK_DUE_TIME, dueTime > 0 ? dueTime : now);
        return db.insert(TABLE_TASK, null, cv);
    }

    public void updateTaskCompletion(long id, boolean isCompleted) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TASK_IS_COMPLETED, isCompleted ? 1 : 0);
        db.update(TABLE_TASK, cv, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void deleteTask(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TASK, COL_TASK_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Task> getTasksByQuadrant(int quadrant) {
        List<Task> list = new ArrayList<Task>();
        SQLiteDatabase db = this.getReadableDatabase();
        // 仅展示未完成待办（已完成任务自动在四象限消失，但完好保存在时间轴和清单中）
        Cursor c = db.query(TABLE_TASK, null, COL_TASK_QUADRANT + "=? AND " + COL_TASK_IS_COMPLETED + "=0",
                new String[]{String.valueOf(quadrant)}, null, null,
                COL_TASK_DUE_TIME + " ASC, " + COL_TASK_ID + " DESC");

        if (c != null && c.moveToFirst()) {
            do {
                list.add(cursorToTask(c));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public List<Task> getTimelineTasks() {
        List<Task> list = new ArrayList<Task>();
        SQLiteDatabase db = this.getReadableDatabase();
        // 按执行时间顺序列出（时间轴）
        Cursor c = db.query(TABLE_TASK, null, null, null, null, null,
                COL_TASK_IS_COMPLETED + " ASC, " + COL_TASK_DUE_TIME + " ASC, " + COL_TASK_ID + " ASC");

        if (c != null && c.moveToFirst()) {
            do {
                list.add(cursorToTask(c));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    public List<Task> getAllTasks(Integer completedFilter) {
        List<Task> list = new ArrayList<Task>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;
        if (completedFilter != null) {
            selection = COL_TASK_IS_COMPLETED + "=?";
            selectionArgs = new String[]{String.valueOf(completedFilter)};
        }

        Cursor c = db.query(TABLE_TASK, null, selection, selectionArgs, null, null,
                COL_TASK_IS_COMPLETED + " ASC, " + COL_TASK_QUADRANT + " ASC, " + COL_TASK_ID + " DESC");

        if (c != null && c.moveToFirst()) {
            do {
                list.add(cursorToTask(c));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    private Task cursorToTask(Cursor c) {
        Task t = new Task();
        t.id = c.getLong(c.getColumnIndexOrThrow(COL_TASK_ID));
        t.title = c.getString(c.getColumnIndexOrThrow(COL_TASK_TITLE));
        t.description = c.getString(c.getColumnIndexOrThrow(COL_TASK_DESC));
        t.quadrant = c.getInt(c.getColumnIndexOrThrow(COL_TASK_QUADRANT));
        t.isCompleted = c.getInt(c.getColumnIndexOrThrow(COL_TASK_IS_COMPLETED));
        t.createdAt = c.getLong(c.getColumnIndexOrThrow(COL_TASK_CREATED_AT));
        int dueIdx = c.getColumnIndex(COL_TASK_DUE_TIME);
        if (dueIdx >= 0) {
            t.dueTime = c.getLong(dueIdx);
        }
        if (t.dueTime == 0) {
            t.dueTime = t.createdAt;
        }
        return t;
    }

    // --- Note CRUD (独立备忘录) ---
    public long insertNote(String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NOTE_CONTENT, content);
        cv.put(COL_NOTE_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_NOTE, null, cv);
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTE, COL_NOTE_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Note> getAllNotes() {
        List<Note> list = new ArrayList<Note>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TABLE_NOTE, null, null, null, null, null, COL_NOTE_ID + " DESC");
        if (c != null && c.moveToFirst()) {
            do {
                Note n = new Note();
                n.id = c.getLong(c.getColumnIndexOrThrow(COL_NOTE_ID));
                n.content = c.getString(c.getColumnIndexOrThrow(COL_NOTE_CONTENT));
                n.createdAt = c.getLong(c.getColumnIndexOrThrow(COL_NOTE_CREATED_AT));
                list.add(n);
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }
}
