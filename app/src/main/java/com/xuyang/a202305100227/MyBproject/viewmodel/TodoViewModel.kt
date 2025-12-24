package com.xuyang.a202305100227.MyBproject.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xuyang.a202305100227.MyBproject.db.TodoRepository
import com.xuyang.a202305100227.MyBproject.db.entity.Todo
import kotlinx.coroutines.launch

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {
    // 所有Todo
    val allTodos: LiveData<List<Todo>> = repository.allTodos

    // 搜索查询
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    // 筛选状态
    private val _isShowAll = MutableLiveData(true)
    val isShowAll: LiveData<Boolean> = _isShowAll

    private val _isShowCompleted = MutableLiveData(false)
    val isShowCompleted: LiveData<Boolean> = _isShowCompleted

    private val _isShowUncompleted = MutableLiveData(false)
    val isShowUncompleted: LiveData<Boolean> = _isShowUncompleted

    // 筛选和搜索后的结果
    private val _filteredTodos = MediatorLiveData<List<Todo>>()
    val filteredTodos: LiveData<List<Todo>> = _filteredTodos

    init {
        // 当allTodos、searchQuery或筛选状态变化时，更新filteredTodos
        _filteredTodos.addSource(allTodos) { updateFilteredTodos() }
        _filteredTodos.addSource(_searchQuery) { updateFilteredTodos() }
        _filteredTodos.addSource(_isShowAll) { updateFilteredTodos() }
        _filteredTodos.addSource(_isShowCompleted) { updateFilteredTodos() }
        _filteredTodos.addSource(_isShowUncompleted) { updateFilteredTodos() }
    }

    // 插入
    fun insert(todo: Todo) = viewModelScope.launch {
        repository.insert(todo)
    }

    // 更新
    fun update(todo: Todo) = viewModelScope.launch {
        repository.update(todo)
    }

    // 删除
    fun delete(todo: Todo) = viewModelScope.launch {
        repository.delete(todo)
    }

    // 设置搜索查询
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 设置筛选状态
    fun setFilter(isShowAll: Boolean, isShowCompleted: Boolean, isShowUncompleted: Boolean) {
        _isShowAll.value = isShowAll
        _isShowCompleted.value = isShowCompleted
        _isShowUncompleted.value = isShowUncompleted
    }

    // 更新筛选后的结果
    private fun updateFilteredTodos() {
        val todos = allTodos.value ?: emptyList()
        val query = searchQuery.value ?: ""
        val showAll = isShowAll.value ?: true
        val showCompleted = isShowCompleted.value ?: false
        val showUncompleted = isShowUncompleted.value ?: false

        // 首先根据搜索查询过滤
        val searchedTodos = if (query.isEmpty()) {
            todos
        } else {
            todos.filter { todo ->
                todo.name.contains(query, ignoreCase = true) ||
                        todo.note.contains(query, ignoreCase = true)
            }
        }

        // 然后根据筛选条件进一步过滤
        val filtered = when {
            showAll -> searchedTodos
            showCompleted -> searchedTodos.filter { it.isCompleted }
            showUncompleted -> searchedTodos.filter { !it.isCompleted }
            else -> searchedTodos
        }

        _filteredTodos.value = filtered
    }

    class TodoViewModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TodoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}