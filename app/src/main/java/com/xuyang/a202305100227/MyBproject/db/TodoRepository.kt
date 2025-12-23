package com.xuyang.a202305100227.MyBproject.db

import androidx.lifecycle.LiveData
import com.xuyang.a202305100227.MyBproject.db.entity.Todo

class TodoRepository(private val todoDao: TodoDao) {
    // 所有Todo
    val allTodos: LiveData<List<Todo>> = todoDao.getAllTodos()

    // 插入
    suspend fun insert(todo: Todo) {
        todoDao.insert(todo)
    }

    // 更新
    suspend fun update(todo: Todo) {
        todoDao.update(todo)
    }

    // 删除
    suspend fun delete(todo: Todo) {
        todoDao.delete(todo)
    }

    // 搜索
    fun searchTodos(searchQuery: String): LiveData<List<Todo>> {
        return todoDao.searchTodos("%$searchQuery%") // 模糊查询
    }
}