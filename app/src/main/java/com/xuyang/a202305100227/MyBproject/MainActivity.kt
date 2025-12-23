package com.xuyang.a202305100227.MyBproject

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.CheckBox
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.xuyang.a202305100227.MyBproject.adapter.TodoAdapter
import com.xuyang.a202305100227.MyBproject.db.TodoDatabase
import com.xuyang.a202305100227.MyBproject.db.TodoRepository
import com.xuyang.a202305100227.MyBproject.db.entity.Todo
import com.xuyang.a202305100227.MyBproject.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TodoAdapter
    private lateinit var tvTotalCount: android.widget.TextView
    private lateinit var cbAll: CheckBox
    private lateinit var cbCompleted: CheckBox
    private lateinit var cbUncompleted: CheckBox
    private lateinit var textInputLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var etSearch: TextInputEditText
    private var allTodos: List<Todo> = emptyList()
    private var currentSearchQuery: String = ""

    private val viewModel: TodoViewModel by viewModels {
        val database = TodoDatabase.getDatabase(application)
        val repository = TodoRepository(database.todoDao())
        TodoViewModel.TodoViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化视图
        initViews()

        // 设置RecyclerView
        setupRecyclerView()

        // 设置筛选监听
        setupFilterListeners()

        // 设置搜索监听
        setupSearchListener()

        // 观察数据变化
        observeData()

        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            showAddTodoDialog()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rv_todo)
        tvTotalCount = findViewById(R.id.tv_total_count)
        cbAll = findViewById(R.id.cb_all)
        cbCompleted = findViewById(R.id.cb_completed)
        cbUncompleted = findViewById(R.id.cb_uncompleted)
        textInputLayout = findViewById(R.id.textInputLayout)
        etSearch = findViewById(R.id.et_search)

        // 设置清除按钮点击事件
        textInputLayout.setEndIconOnClickListener {
            // 清空搜索框内容
            etSearch.text?.clear()
            // 让搜索框失去焦点
            etSearch.clearFocus()
            // 重置搜索
            searchTodos("")
        }
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onItemClick = { todo ->
                // 点击item显示详情并可以编辑
                showEditTodoDialog(todo)
            },
            onCheckboxClick = { todo ->
                // 切换完成状态并保存到数据库
                val updatedTodo = todo.copy(isCompleted = !todo.isCompleted)
                viewModel.update(updatedTodo)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFilterListeners() {
        // 全部复选框点击事件
        cbAll.setOnClickListener {
            if (cbAll.isChecked) {
                cbCompleted.isChecked = false
                cbUncompleted.isChecked = false
                filterTodos()
            }
        }

        // 已完成复选框点击事件
        cbCompleted.setOnClickListener {
            if (cbCompleted.isChecked) {
                cbAll.isChecked = false
                cbUncompleted.isChecked = false
                filterTodos()
            }
        }

        // 未完成复选框点击事件
        cbUncompleted.setOnClickListener {
            if (cbUncompleted.isChecked) {
                cbAll.isChecked = false
                cbCompleted.isChecked = false
                filterTodos()
            }
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 不需要实现
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本变化时进行搜索
                val searchQuery = s?.toString() ?: ""
                searchTodos(searchQuery)
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                // 不需要实现
            }
        })
    }

    private fun observeData() {
        // 观察所有待办事项
        viewModel.allTodos.observe(this, Observer { todos ->
            todos?.let {
                allTodos = it
                filterTodos()
            }
        })

        // 观察搜索结果
        viewModel.searchResults.observe(this, Observer { searchResults ->
            searchResults?.let {
                // 对搜索结果应用当前筛选条件
                val isShowAll = cbAll.isChecked
                val isShowCompleted = cbCompleted.isChecked
                val isShowUncompleted = cbUncompleted.isChecked

                val filteredSearchResults = viewModel.filterTodos(it, isShowAll, isShowCompleted, isShowUncompleted)
                adapter.submitList(filteredSearchResults)
                updateTotalCount(filteredSearchResults.size)
            }
        })
    }


    private fun showAddTodoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_todo, null)
        val etName: TextInputEditText = dialogView.findViewById(R.id.et_name)
        val etNote: TextInputEditText = dialogView.findViewById(R.id.et_note)
        val tvReminderTime: TextView = dialogView.findViewById(R.id.tv_reminder_time)

        // 计算默认时间（当前时间+1小时）并显示
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val defaultTime = calendar.timeInMillis

        // 格式化并显示时间
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timeText = dateFormat.format(java.util.Date(defaultTime))
        tvReminderTime.text = timeText

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("添加待办事项")
            .setView(dialogView)
            .setPositiveButton("确定", null) // 先设置为null，稍后手动处理
            .setNegativeButton("取消", null)
            .create()

        // 手动处理确定按钮点击，以便验证失败时不关闭对话框
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = etName.text?.toString()?.trim()
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val note = etNote.text?.toString()?.trim() ?: ""

                val newTodo = Todo(
                    id = 0, // 数据库会自动生成ID
                    name = name!!,
                    isCompleted = false,
                    reminderTime = defaultTime,
                    note = note
                )

                // 保存到数据库
                viewModel.insert(newTodo)

                Toast.makeText(this, "待办事项已添加", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showEditTodoDialog(todo: Todo) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_todo, null)
        val etName: TextInputEditText = dialogView.findViewById(R.id.et_name)
        val etNote: TextInputEditText = dialogView.findViewById(R.id.et_note)
        val cbCompleted: CheckBox = dialogView.findViewById(R.id.cb_completed)
        val tvReminderTime: TextView = dialogView.findViewById(R.id.tv_reminder_time)
        val tvReminderTimeLabel: TextView = dialogView.findViewById(R.id.tv_reminder_time_label)

        // 预填充数据
        etName.setText(todo.name)
        etNote.setText(todo.note)
        cbCompleted.isChecked = todo.isCompleted

        // 格式化并显示时间
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timeText = dateFormat.format(java.util.Date(todo.reminderTime))
        tvReminderTime.text = timeText

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("编辑待办事项")
            .setView(dialogView)
            .setPositiveButton("保存", null) // 先设置为null，稍后手动处理
            .setNegativeButton("取消", null)
            .create()

        // 手动处理确定按钮点击，以便验证失败时不关闭对话框
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = etName.text?.toString()?.trim()
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val note = etNote.text?.toString()?.trim() ?: ""
                val isCompleted = cbCompleted.isChecked

                // 更新待办事项
                val updatedTodo = todo.copy(
                    name = name!!,
                    note = note,
                    isCompleted = isCompleted
                )

                // 保存到数据库
                viewModel.update(updatedTodo)

                Toast.makeText(this, "待办事项已更新", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun searchTodos(searchQuery: String) {
        currentSearchQuery = searchQuery
        if (searchQuery.isEmpty()) {
            // 如果搜索查询为空，显示所有待办事项（并应用当前筛选条件）
            filterTodos()
        } else {
            // 否则，进行搜索
            viewModel.searchTodos(searchQuery)
        }
    }

    private fun filterTodos() {
        val isShowAll = cbAll.isChecked
        val isShowCompleted = cbCompleted.isChecked
        val isShowUncompleted = cbUncompleted.isChecked

        if (currentSearchQuery.isEmpty()) {
            // 如果没有搜索查询，直接筛选所有待办事项
            val filteredTodos = viewModel.filterTodos(allTodos, isShowAll, isShowCompleted, isShowUncompleted)
            adapter.submitList(filteredTodos)
            updateTotalCount(filteredTodos.size)
        } else {
            // 如果有搜索查询，重新执行搜索
            viewModel.searchTodos(currentSearchQuery)
        }
    }

    private fun updateTotalCount(count: Int) {
        tvTotalCount.text = "全部代办：$count"
    }
}