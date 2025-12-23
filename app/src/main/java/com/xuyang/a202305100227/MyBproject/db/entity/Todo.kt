package com.xuyang.a202305100227.MyBproject.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Todo实体类，包含核心属性
 */
@Entity(tableName = "todo_table")
data class Todo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val isCompleted: Boolean = false,
    val name: String,
    val reminderTime: Long,
    val note: String = ""
)