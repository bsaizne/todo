package net.icycloud.fdtodolist.local;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper dbHelper;

    // 顶部标题与操作
    private TextView tvHeaderTitle;
    private TextView tvHeaderSub;
    private Button btnAction;

    // 页面容器
    private View viewQuadrants;
    private View viewTimeline;
    private View viewTasks;
    private View viewNotes;

    // 底部导航按钮
    private Button navQuadrants;
    private Button navTimeline;
    private Button navTasks;
    private Button navNotes;

    // 四象限控件
    private TextView tvQ1Count, tvQ2Count, tvQ3Count, tvQ4Count;
    private Button btnAddQ1, btnAddQ2, btnAddQ3, btnAddQ4;
    private LinearLayout llQ1Tasks, llQ2Tasks, llQ3Tasks, llQ4Tasks;

    // 时间轴控件
    private ListView lvTimeline;
    private TextView tvTimelineEmpty;
    private TimelineAdapter timelineAdapter;
    private List<Task> timelineTaskList = new ArrayList<Task>();

    // 清单列表控件
    private ListView lvTasks;
    private TextView tvTasksEmpty;
    private Button btnFilterAll, btnFilterActive, btnFilterDone;
    private TaskAdapter taskAdapter;
    private List<Task> allTaskList = new ArrayList<Task>();
    private Integer completedFilter = null; // null: 全部, 0: 未完成, 1: 已完成

    // 备忘录便签控件
    private ListView lvNotes;
    private TextView tvNotesEmpty;
    private Button btnNotesQuickAdd;
    private NoteAdapter noteAdapter;
    private List<Note> noteList = new ArrayList<Note>();

    private int currentTab = 0; // 0: 四象限, 1: 时间轴, 2: 清单, 3: 备忘录

    public static final String[] QUAD_NAMES = {
        "重要且紧急 (红)",
        "重要不紧急 (黄)",
        "紧急不重要 (蓝)",
        "不重要不紧急 (绿)"
    };

    public static final String[] QUAD_SHORT_NAMES = {
        "重要且紧急",
        "重要不紧急",
        "紧急不重要",
        "不重要不紧急"
    };

    public static final int[] QUAD_COLORS = {
        0xFFFF3300, // 红
        0xFFE6A200, // 黄
        0xFF0099FF, // 蓝
        0xFF669900  // 绿
    };

    public static final int[] QUAD_DOT_RES = {
        R.drawable.bg_dot_1,
        R.drawable.bg_dot_2,
        R.drawable.bg_dot_3,
        R.drawable.bg_dot_4
    };

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
    private SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        initViews();
        initListeners();
        switchTab(0); // 默认打开四象限
    }

    private void initViews() {
        tvHeaderTitle = findViewById(R.id.tv_header_title);
        tvHeaderSub = findViewById(R.id.tv_header_sub);
        btnAction = findViewById(R.id.btn_action);

        viewQuadrants = findViewById(R.id.view_quadrants);
        viewTimeline = findViewById(R.id.view_timeline);
        viewTasks = findViewById(R.id.view_tasks);
        viewNotes = findViewById(R.id.view_notes);

        navQuadrants = findViewById(R.id.nav_quadrants);
        navTimeline = findViewById(R.id.nav_timeline);
        navTasks = findViewById(R.id.nav_tasks);
        navNotes = findViewById(R.id.nav_notes);

        // 四象限
        tvQ1Count = findViewById(R.id.tv_q1_count);
        tvQ2Count = findViewById(R.id.tv_q2_count);
        tvQ3Count = findViewById(R.id.tv_q3_count);
        tvQ4Count = findViewById(R.id.tv_q4_count);

        btnAddQ1 = findViewById(R.id.btn_add_q1);
        btnAddQ2 = findViewById(R.id.btn_add_q2);
        btnAddQ3 = findViewById(R.id.btn_add_q3);
        btnAddQ4 = findViewById(R.id.btn_add_q4);

        llQ1Tasks = findViewById(R.id.ll_q1_tasks);
        llQ2Tasks = findViewById(R.id.ll_q2_tasks);
        llQ3Tasks = findViewById(R.id.ll_q3_tasks);
        llQ4Tasks = findViewById(R.id.ll_q4_tasks);

        // 时间轴
        lvTimeline = findViewById(R.id.lv_timeline);
        tvTimelineEmpty = findViewById(R.id.tv_timeline_empty);
        timelineAdapter = new TimelineAdapter();
        lvTimeline.setAdapter(timelineAdapter);

        // 清单列表
        lvTasks = findViewById(R.id.lv_tasks);
        tvTasksEmpty = findViewById(R.id.tv_tasks_empty);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterActive = findViewById(R.id.btn_filter_active);
        btnFilterDone = findViewById(R.id.btn_filter_done);
        taskAdapter = new TaskAdapter();
        lvTasks.setAdapter(taskAdapter);

        // 备忘录
        lvNotes = findViewById(R.id.lv_notes);
        tvNotesEmpty = findViewById(R.id.tv_notes_empty);
        btnNotesQuickAdd = findViewById(R.id.btn_notes_quick_add);
        noteAdapter = new NoteAdapter();
        lvNotes.setAdapter(noteAdapter);
    }

    private void initListeners() {
        // 底部导航
        navQuadrants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(0);
            }
        });

        navTimeline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(1);
            }
        });

        navTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(2);
            }
        });

        navNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(3);
            }
        });

        // 顶部新增按钮
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentTab == 3) {
                    showAddNoteDialog();
                } else {
                    showAddTaskDialog(1); // 默认象限1
                }
            }
        });

        // 四象限内专属添加按钮
        btnAddQ1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog(1);
            }
        });
        btnAddQ2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog(2);
            }
        });
        btnAddQ3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog(3);
            }
        });
        btnAddQ4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTaskDialog(4);
            }
        });

        // 清单筛选按钮
        btnFilterAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTaskFilter(null);
            }
        });
        btnFilterActive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTaskFilter(0);
            }
        });
        btnFilterDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTaskFilter(1);
            }
        });

        // 备忘录快捷新建
        btnNotesQuickAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNoteDialog();
            }
        });
    }

    private void switchTab(int tab) {
        currentTab = tab;

        // 导航颜色
        int activeColor = 0xFF2A62F8;
        int normalColor = 0xFF777777;
        navQuadrants.setTextColor(tab == 0 ? activeColor : normalColor);
        navTimeline.setTextColor(tab == 1 ? activeColor : normalColor);
        navTasks.setTextColor(tab == 2 ? activeColor : normalColor);
        navNotes.setTextColor(tab == 3 ? activeColor : normalColor);

        // 隐藏/显示各页面
        viewQuadrants.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
        viewTimeline.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        viewTasks.setVisibility(tab == 2 ? View.VISIBLE : View.GONE);
        viewNotes.setVisibility(tab == 3 ? View.VISIBLE : View.GONE);

        // 顶部文案变更
        if (tab == 0) {
            tvHeaderSub.setText("四象限管理");
            btnAction.setText("+ 新建待办");
            loadQuadrants();
        } else if (tab == 1) {
            tvHeaderSub.setText("时间轴视图");
            btnAction.setText("+ 新建待办");
            loadTimeline();
        } else if (tab == 2) {
            tvHeaderSub.setText("清单列表");
            btnAction.setText("+ 新建待办");
            loadAllTasks();
        } else if (tab == 3) {
            tvHeaderSub.setText("便签备忘录");
            btnAction.setText("+ 新建便签");
            loadNotes();
        }
    }

    // --- 1. 四象限逻辑 ---
    private void loadQuadrants() {
        populateQuadrant(1, llQ1Tasks, tvQ1Count);
        populateQuadrant(2, llQ2Tasks, tvQ2Count);
        populateQuadrant(3, llQ3Tasks, tvQ3Count);
        populateQuadrant(4, llQ4Tasks, tvQ4Count);
    }

    private void populateQuadrant(final int quadrant, LinearLayout container, TextView countView) {
        container.removeAllViews();
        List<Task> tasks = dbHelper.getTasksByQuadrant(quadrant);
        countView.setText("(" + tasks.size() + ")");

        if (tasks.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("暂无待办");
            emptyTv.setTextSize(11);
            emptyTv.setTextColor(0xFF999999);
            emptyTv.setPadding(8, 16, 8, 8);
            container.addView(emptyTv);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (final Task t : tasks) {
            View itemView = inflater.inflate(R.layout.item_quadrant_task, container, false);
            CheckBox cb = itemView.findViewById(R.id.cb_qtask_done);
            final TextView tvTitle = itemView.findViewById(R.id.tv_qtask_title);
            Button btnDel = itemView.findViewById(R.id.btn_qtask_delete);

            tvTitle.setText(t.title);
            cb.setOnCheckedChangeListener(null);
            cb.setChecked(t.isCompleted == 1);
            applyStrikethrough(tvTitle, t.isCompleted == 1);

            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        applyStrikethrough(tvTitle, true);
                        t.isCompleted = 1;
                        dbHelper.updateTaskCompletion(t.id, true);
                        buttonView.setEnabled(false);
                        buttonView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadQuadrants();
                            }
                        }, 250);
                    } else {
                        t.isCompleted = 0;
                        dbHelper.updateTaskCompletion(t.id, false);
                        loadQuadrants();
                    }
                }
            });

            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteTask(t);
                }
            });

            container.addView(itemView);
        }
    }

    // --- 2. 时间轴逻辑 ---
    private void loadTimeline() {
        timelineTaskList.clear();
        timelineTaskList.addAll(dbHelper.getTimelineTasks());
        timelineAdapter.notifyDataSetChanged();
        tvTimelineEmpty.setVisibility(timelineTaskList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // --- 3. 清单列表逻辑 ---
    private void setTaskFilter(Integer filter) {
        completedFilter = filter;
        btnFilterAll.setTextColor(filter == null ? 0xFF2A62F8 : 0xFF777777);
        btnFilterActive.setTextColor(filter != null && filter == 0 ? 0xFF2A62F8 : 0xFF777777);
        btnFilterDone.setTextColor(filter != null && filter == 1 ? 0xFF2A62F8 : 0xFF777777);
        loadAllTasks();
    }

    private void loadAllTasks() {
        allTaskList.clear();
        allTaskList.addAll(dbHelper.getAllTasks(completedFilter));
        taskAdapter.notifyDataSetChanged();
        tvTasksEmpty.setVisibility(allTaskList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // --- 4. 备忘录便签逻辑 ---
    private void loadNotes() {
        noteList.clear();
        noteList.addAll(dbHelper.getAllNotes());
        noteAdapter.notifyDataSetChanged();
        tvNotesEmpty.setVisibility(noteList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // --- 对话框：新建/编辑待办 ---
    private void showAddTaskDialog(int defaultQuadrant) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        final EditText etTitle = view.findViewById(R.id.et_title);
        final EditText etDesc = view.findViewById(R.id.et_desc);
        final Spinner spQuadrant = view.findViewById(R.id.sp_quadrant);
        final Button btnPickTime = view.findViewById(R.id.btn_pick_time);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, QUAD_NAMES);
        spQuadrant.setAdapter(adapter);
        int selIndex = Math.max(0, Math.min(defaultQuadrant - 1, 3));
        spQuadrant.setSelection(selIndex);

        final Calendar cal = Calendar.getInstance();
        final long[] selectedTime = new long[]{cal.getTimeInMillis()};

        btnPickTime.setText("🕒 时间: " + fullFormat.format(cal.getTime()));
        btnPickTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 先选日期，再选时间
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, month);
                        cal.set(Calendar.DAY_OF_MONTH, day);

                        new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                                cal.set(Calendar.HOUR_OF_DAY, hour);
                                cal.set(Calendar.MINUTE, minute);
                                cal.set(Calendar.SECOND, 0);
                                selectedTime[0] = cal.getTimeInMillis();
                                btnPickTime.setText("🕒 时间: " + fullFormat.format(cal.getTime()));
                            }
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(MainActivity.this, "待办事项不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int quad = spQuadrant.getSelectedItemPosition() + 1; // 1-4
                    dbHelper.insertTask(title, desc, quad, selectedTime[0]);
                    refreshCurrentTab();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    // --- 对话框：新建备忘便签 ---
    private void showAddNoteDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null);
        final EditText etNote = view.findViewById(R.id.et_note_content);

        new AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String content = etNote.getText().toString().trim();
                    if (content.isEmpty()) {
                        Toast.makeText(MainActivity.this, "便签内容不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dbHelper.insertNote(content);
                    if (currentTab == 3) {
                        loadNotes();
                    } else {
                        switchTab(3); // 自动切到备忘录查看
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void confirmDeleteTask(final Task t) {
        new AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定删除待办「" + t.title + "」？")
            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dbHelper.deleteTask(t.id);
                    refreshCurrentTab();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void refreshCurrentTab() {
        if (currentTab == 0) loadQuadrants();
        else if (currentTab == 1) loadTimeline();
        else if (currentTab == 2) loadAllTasks();
        else if (currentTab == 3) loadNotes();
    }

    private void applyStrikethrough(TextView tv, boolean strikethrough) {
        if (strikethrough) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setTextColor(0xFFAAAAAA);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tv.setTextColor(0xFF222222);
        }
    }

    // --- Timeline 适配器 ---
    private class TimelineAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return timelineTaskList.size();
        }

        @Override
        public Object getItem(int position) {
            return timelineTaskList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return timelineTaskList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_timeline, parent, false);
            }

            final Task task = timelineTaskList.get(position);

            TextView tvTime = convertView.findViewById(R.id.tv_timeline_time);
            TextView tvDate = convertView.findViewById(R.id.tv_timeline_date);
            View vDot = convertView.findViewById(R.id.v_timeline_dot);
            TextView tvBadge = convertView.findViewById(R.id.tv_timeline_quad_badge);
            CheckBox cbDone = convertView.findViewById(R.id.cb_timeline_done);
            TextView tvTitle = convertView.findViewById(R.id.tv_timeline_title);
            TextView tvDesc = convertView.findViewById(R.id.tv_timeline_desc);
            Button btnDel = convertView.findViewById(R.id.btn_timeline_delete);

            // 时间解析
            long displayTime = task.dueTime > 0 ? task.dueTime : task.createdAt;
            Date d = new Date(displayTime);
            tvTime.setText(timeFormat.format(d));
            tvDate.setText(dateFormat.format(d));

            // 象限颜色与标签
            int qIdx = Math.max(0, Math.min(task.quadrant - 1, 3));
            vDot.setBackgroundResource(QUAD_DOT_RES[qIdx]);
            tvBadge.setText(QUAD_SHORT_NAMES[qIdx]);
            tvBadge.setTextColor(QUAD_COLORS[qIdx]);

            // 标题与完成状态
            tvTitle.setText(task.title);
            cbDone.setOnCheckedChangeListener(null);
            cbDone.setChecked(task.isCompleted == 1);
            applyStrikethrough(tvTitle, task.isCompleted == 1);

            cbDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    task.isCompleted = isChecked ? 1 : 0;
                    dbHelper.updateTaskCompletion(task.id, isChecked);
                    loadTimeline();
                }
            });

            if (task.description != null && !task.description.isEmpty()) {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(task.description);
            } else {
                tvDesc.setVisibility(View.GONE);
            }

            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteTask(task);
                }
            });

            return convertView;
        }
    }

    // --- 清单列表适配器 ---
    private class TaskAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return allTaskList.size();
        }

        @Override
        public Object getItem(int position) {
            return allTaskList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return allTaskList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_task, parent, false);
            }

            final Task task = allTaskList.get(position);
            View vBar = convertView.findViewById(R.id.v_priority_bar);
            CheckBox cbDone = convertView.findViewById(R.id.cb_done);
            TextView tvTitle = convertView.findViewById(R.id.tv_title);
            TextView tvDesc = convertView.findViewById(R.id.tv_desc);
            TextView tvTag = convertView.findViewById(R.id.tv_priority_tag);
            Button btnDelete = convertView.findViewById(R.id.btn_delete);

            int qIdx = Math.max(0, Math.min(task.quadrant - 1, 3));
            int color = QUAD_COLORS[qIdx];
            vBar.setBackgroundColor(color);
            tvTag.setTextColor(color);
            tvTag.setText(QUAD_SHORT_NAMES[qIdx]);

            tvTitle.setText(task.title);
            if (task.description != null && !task.description.isEmpty()) {
                tvDesc.setVisibility(View.VISIBLE);
                tvDesc.setText(task.description);
            } else {
                tvDesc.setVisibility(View.GONE);
            }

            cbDone.setOnCheckedChangeListener(null);
            cbDone.setChecked(task.isCompleted == 1);
            applyStrikethrough(tvTitle, task.isCompleted == 1);

            cbDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    task.isCompleted = isChecked ? 1 : 0;
                    dbHelper.updateTaskCompletion(task.id, isChecked);
                    loadAllTasks();
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteTask(task);
                }
            });

            return convertView;
        }
    }

    // --- 便签备忘录适配器 ---
    private class NoteAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return noteList.size();
        }

        @Override
        public Object getItem(int position) {
            return noteList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return noteList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_note, parent, false);
            }

            final Note note = noteList.get(position);
            TextView tvContent = convertView.findViewById(R.id.tv_note_content);
            TextView tvTime = convertView.findViewById(R.id.tv_note_time);
            Button btnCopy = convertView.findViewById(R.id.btn_note_copy);
            Button btnDelete = convertView.findViewById(R.id.btn_note_delete);

            tvContent.setText(note.content);
            tvTime.setText(fullFormat.format(new Date(note.createdAt)));

            btnCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        ClipData clip = ClipData.newPlainText("note", note.content);
                        cm.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "便签已复制到剪贴板", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("确认删除")
                        .setMessage("确定要删除这条便签备忘吗？")
                        .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbHelper.deleteNote(note.id);
                                loadNotes();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                }
            });

            return convertView;
        }
    }
}
