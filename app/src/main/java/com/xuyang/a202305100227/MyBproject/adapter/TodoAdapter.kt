package com.xuyang.a202305100227.MyBproject.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xuyang.a202305100227.MyBproject.R
import com.xuyang.a202305100227.MyBproject.db.entity.Todo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private val onItemClick: (Todo) -> Unit,
    private val onCheckboxClick: (Todo) -> Unit,
    private val onDeleteClick: (Todo) -> Unit
) : ListAdapter<Todo, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = getItem(position)
        holder.bind(todo, onItemClick, onCheckboxClick, onDeleteClick)
    }

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cb_todo_completed)
        private val tvTodoName: TextView = itemView.findViewById(R.id.tv_todo_name)
        private val tvReminderTime: TextView = itemView.findViewById(R.id.tv_reminder_time)
        private val ibDeleteTodo: ImageButton = itemView.findViewById(R.id.ib_delete_todo)

        fun bind(
            todo: Todo,
            onItemClick: (Todo) -> Unit,
            onCheckboxClick: (Todo) -> Unit,
            onDeleteClick: (Todo) -> Unit
        ) {
            tvTodoName.text = todo.name
            cbCompleted.isChecked = todo.isCompleted

            // 格式化提醒时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val timeText = dateFormat.format(Date(todo.reminderTime))
            tvReminderTime.text = timeText

            // 如果已完成，添加删除线效果
            if (todo.isCompleted) {
                tvTodoName.paintFlags = tvTodoName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                tvReminderTime.paintFlags = tvReminderTime.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvTodoName.paintFlags = tvTodoName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvReminderTime.paintFlags = tvReminderTime.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // 点击整个item
            itemView.setOnClickListener {
                onItemClick(todo)
            }

            // 点击复选框
            cbCompleted.setOnClickListener {
                onCheckboxClick(todo)
            }

            // 点击删除按钮
            ibDeleteTodo.setOnClickListener {
                onDeleteClick(todo)
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<Todo>() {
        override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem == newItem
        }
    }
}