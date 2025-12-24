package com.xuyang.a202305100227.MyBproject.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.xuyang.a202305100227.MyBproject.db.entity.Todo

@Dao
interface TodoDao {
    // 插入
    @Insert
    suspend fun insert(todo: Todo)

    // 更新
    @Update
    suspend fun update(todo: Todo)

    // 删除
    @Delete
    suspend fun delete(todo: Todo)

    // 查询所有
    @Query("SELECT * FROM todo_table ORDER BY reminderTime")
    fun getAllTodos(): LiveData<List<Todo>>

    // 搜索（按名称或备注）
    @Query("SELECT * FROM todo_table WHERE name LIKE :searchQuery OR note LIKE :searchQuery ORDER BY reminderTime")
    fun searchTodos(searchQuery: String): LiveData<List<Todo>>
}