package com.xuyang.a202305100227.MyBproject.utils

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.xuyang.a202305100227.MyBproject.R
import com.xuyang.a202305100227.MyBproject.db.entity.Todo
import com.xuyang.a202305100227.MyBproject.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.text.trim

/**
 * 待办事项助手类
 * 整合了日期时间选择和待办事项对话框功能
 */
class TodoHelper(private val context: Context) {
    // 日期时间选择相关变量
    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = 0
    private var selectedMinute = 0

    /**
     * 回调接口，用于通知时间选择完成
     */
    interface OnDateTimeSelectedListener {
        fun onDateTimeSelected(timestamp: Long)
    }

    /**
     * 显示日期选择对话框
     * @param currentTime 当前时间戳
     * @param listener 时间选择完成的回调
     */
    fun showDatePickerDialog(currentTime: Long, listener: OnDateTimeSelectedListener) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        // 初始化默认日期为当前时间
        selectedYear = calendar.get(Calendar.YEAR)
        selectedMonth = calendar.get(Calendar.MONTH)
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = calendar.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth

                // 日期选择后自动显示时间选择对话框
                showTimePickerDialog(listener)
            },
            selectedYear,
            selectedMonth,
            selectedDay
        )
        datePickerDialog.show()
    }

    /**
     * 显示时间选择对话框
     */
    private fun showTimePickerDialog(listener: OnDateTimeSelectedListener) {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute

                // 构建选择的日期时间
                val calendar = Calendar.getInstance()
                calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                // 通知选择完成
                listener.onDateTimeSelected(calendar.timeInMillis)
            },
            selectedHour,
            selectedMinute,
            true
        )
        timePickerDialog.show()
    }

    /**
     * 格式化时间戳为字符串
     * @param timestamp 时间戳
     * @return 格式化后的时间字符串，格式为：yyyy-MM-dd HH:mm
     */
    fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 获取系统当前时间，精确到分钟
     */
    fun getCurrentTime(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 显示添加待办事项对话框
     * @param viewModel ViewModel实例，用于保存待办事项
     */
    fun showAddTodoDialog(viewModel: TodoViewModel) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null)
        val etName: TextInputEditText = dialogView.findViewById(R.id.et_name)
        val etNote: TextInputEditText = dialogView.findViewById(R.id.et_note)
        val tvReminderTime: TextView = dialogView.findViewById(R.id.tv_reminder_time)
        val btnSelectTime: Button = dialogView.findViewById(R.id.btn_select_time)

        // 使用系统当前时间作为默认时间
        var currentReminderTime = getCurrentTime()
        var currentReminderTextView: TextView = tvReminderTime

        // 设置点击事件，用于选择日期时间
        btnSelectTime.setOnClickListener {
            showDatePickerDialog(currentReminderTime, object : OnDateTimeSelectedListener {
                override fun onDateTimeSelected(timestamp: Long) {
                    currentReminderTime = timestamp
                    currentReminderTextView.text = formatTime(timestamp)
                    currentReminderTextView.visibility = View.VISIBLE
                }
            })
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("添加待办事项")
            .setView(dialogView)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create()

        // 手动处理确定按钮点击，以便验证失败时不关闭对话框
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = etName.text?.toString()?.trim()
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(context, "请输入任务名称", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val note = etNote.text?.toString()?.trim() ?: ""

                val newTodo = Todo(
                    id = 0, // 数据库会自动生成ID
                    name = name!!,
                    isCompleted = false,
                    reminderTime = currentReminderTime,
                    note = note
                )

                // 保存到数据库
                viewModel.insert(newTodo)

                Toast.makeText(context, "待办事项已添加", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    /**
     * 显示编辑待办事项对话框
     * @param todo 要编辑的待办事项
     * @param viewModel ViewModel实例，用于更新待办事项
     */
    fun showEditTodoDialog(todo: Todo, viewModel: TodoViewModel) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_todo, null)
        val etName: TextInputEditText = dialogView.findViewById(R.id.et_name)
        val etNote: TextInputEditText = dialogView.findViewById(R.id.et_note)
        val cbCompleted: CheckBox = dialogView.findViewById(R.id.cb_completed)
        val tvReminderTime: TextView = dialogView.findViewById(R.id.tv_reminder_time)
        val btnChangeTime: Button = dialogView.findViewById(R.id.btn_change_time)

        // 预填充数据
        etName.setText(todo.name)
        etNote.setText(todo.note)
        cbCompleted.isChecked = todo.isCompleted

        // 格式化并显示时间
        var currentReminderTime = todo.reminderTime
        val timeText = formatTime(currentReminderTime)
        tvReminderTime.text = timeText
        tvReminderTime.visibility = View.VISIBLE

        // 设置点击事件，用于选择日期时间
        var currentReminderTextView: TextView = tvReminderTime
        btnChangeTime.setOnClickListener {
            showDatePickerDialog(currentReminderTime, object : OnDateTimeSelectedListener {
                override fun onDateTimeSelected(timestamp: Long) {
                    currentReminderTime = timestamp
                    currentReminderTextView.text = formatTime(timestamp)
                    currentReminderTextView.visibility = View.VISIBLE
                }
            })
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("编辑待办事项")
            .setView(dialogView)
            .setPositiveButton("保存", null)
            .setNegativeButton("取消", null)
            .create()

        // 手动处理确定按钮点击，以便验证失败时不关闭对话框
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val name = etName.text?.toString()?.trim()
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(context, "请输入任务名称", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val note = etNote.text?.toString()?.trim() ?: ""
                val isCompleted = cbCompleted.isChecked

                // 更新待办事项
                val updatedTodo = todo.copy(
                    name = name!!,
                    note = note,
                    isCompleted = isCompleted,
                    reminderTime = currentReminderTime
                )

                // 保存到数据库
                viewModel.update(updatedTodo)

                Toast.makeText(context, "待办事项已更新", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}