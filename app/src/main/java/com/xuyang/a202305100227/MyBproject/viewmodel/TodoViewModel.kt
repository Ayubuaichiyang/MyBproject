package com.xuyang.a202305100227.MyBproject.viewmodel

import androidx.lifecycle.LiveData
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

    // 搜索结果
    private val _searchResults = MutableLiveData<List<Todo>>()
    val searchResults: LiveData<List<Todo>> = _searchResults

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


    // 筛选：全部/已完成/未完成
    fun filterTodos(todos: List<Todo>?, isShowAll: Boolean, isShowCompleted: Boolean, isShowUncompleted: Boolean): List<Todo> {
        if (todos == null) return emptyList()
        return when {
            isShowAll -> todos
            isShowCompleted -> todos.filter { it.isCompleted }
            isShowUncompleted -> todos.filter { !it.isCompleted }
            else -> todos
        }
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