package com.xuyang.a202305100227.MyBproject

import android.os.Bundle
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
import android.widget.CheckBox
import com.google.android.material.textfield.TextInputEditText
import com.xuyang.a202305100227.MyBproject.adapter.TodoAdapter
import com.xuyang.a202305100227.MyBproject.db.TodoDatabase
import com.xuyang.a202305100227.MyBproject.db.TodoRepository
import com.xuyang.a202305100227.MyBproject.utils.TodoHelper
import com.xuyang.a202305100227.MyBproject.viewmodel.TodoViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TodoAdapter
    private lateinit var tvTotalCount: android.widget.TextView
    private lateinit var cbAll: CheckBox
    private lateinit var cbCompleted: CheckBox
    private lateinit var cbUncompleted: CheckBox
    private lateinit var textInputLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var etSearch: TextInputEditText
    private var currentSearchQuery: String = ""

    private lateinit var todoHelper: TodoHelper

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

        // 初始化待办事项助手
        todoHelper = TodoHelper(this)

        // 观察数据变化
        observeData()

        // 初始化筛选条件
        updateFilter()

        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener { _ ->
            todoHelper.showAddTodoDialog(viewModel)
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
            currentSearchQuery = ""
            viewModel.setSearchQuery("")
        }
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onItemClick = { todo ->
                // 点击item显示详情并可以编辑
                todoHelper.showEditTodoDialog(todo, viewModel)
            },
            onCheckboxClick = { todo ->
                // 切换完成状态并保存到数据库
                val updatedTodo = todo.copy(isCompleted = !todo.isCompleted)
                viewModel.update(updatedTodo)
            },
            onDeleteClick = { todo ->
                // 从数据库中删除待办事项
                viewModel.delete(todo)
                Toast.makeText(this, "待办事项已删除", Toast.LENGTH_SHORT).show()
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
                updateFilter()
            } else {
                // 如果取消选择全部，确保至少选择一个其他选项
                if (!cbCompleted.isChecked && !cbUncompleted.isChecked) {
                    cbAll.isChecked = true // 恢复选择全部
                }
            }
        }

        // 已完成复选框点击事件
        cbCompleted.setOnClickListener {
            if (cbCompleted.isChecked) {
                cbAll.isChecked = false
                cbUncompleted.isChecked = false
                updateFilter()
            } else {
                // 如果取消选择已完成，确保至少选择一个其他选项
                if (!cbAll.isChecked && !cbUncompleted.isChecked) {
                    cbAll.isChecked = true // 恢复选择全部
                }
            }
        }

        // 未完成复选框点击事件
        cbUncompleted.setOnClickListener {
            if (cbUncompleted.isChecked) {
                cbAll.isChecked = false
                cbCompleted.isChecked = false
                updateFilter()
            } else {
                // 如果取消选择未完成，确保至少选择一个其他选项
                if (!cbAll.isChecked && !cbCompleted.isChecked) {
                    cbAll.isChecked = true // 恢复选择全部
                }
            }
        }
    }

    /**
     * 更新筛选条件到ViewModel
     */
    private fun updateFilter() {
        val isShowAll = cbAll.isChecked
        val isShowCompleted = cbCompleted.isChecked
        val isShowUncompleted = cbUncompleted.isChecked
        viewModel.setFilter(isShowAll, isShowCompleted, isShowUncompleted)
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 不需要实现
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本变化时进行搜索
                val searchQuery = s?.toString() ?: ""
                currentSearchQuery = searchQuery
                viewModel.setSearchQuery(searchQuery)
            }

            override fun afterTextChanged(s: android.text.Editable?) {
                // 不需要实现
            }
        })
    }

    private fun observeData() {
        // 观察筛选后的待办事项
        viewModel.filteredTodos.observe(this, Observer { todos ->
            todos?.let {
                adapter.submitList(it)
                updateTotalCount(it.size)
            }
        })
    }

    private fun updateTotalCount(count: Int) {
        tvTotalCount.text = "全部代办：$count"
    }
}