package ru.cleverpumpkin.calendar.adapter

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ru.cleverpumpkin.calendar.CalendarDateView
import ru.cleverpumpkin.calendar.DateInfoProvider
import ru.cleverpumpkin.calendar.R
import ru.cleverpumpkin.calendar.SimpleLocalDate
import ru.cleverpumpkin.calendar.adapter.item.CalendarItem
import ru.cleverpumpkin.calendar.adapter.item.DateItem
import ru.cleverpumpkin.calendar.adapter.item.EmptyItem
import ru.cleverpumpkin.calendar.adapter.item.MonthItem
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

class CalendarAdapter(
    private val dateInfoProvider: DateInfoProvider,
    private val onDateClickHandler: (SimpleLocalDate, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val MONTH_FORMAT = "LLLL yyyy"
        const val DAY_FORMAT = "d"

        const val DAY_VIEW_TYPE = 0
        const val MONTH_VIEW_TYPE = 1
        const val EMPTY_VIEW_TYPE = 2
    }

    private val calendarItems = mutableListOf<CalendarItem>()

    private val monthFormatter = SimpleDateFormat(MONTH_FORMAT, Locale.getDefault())
    private val dayFormatter = SimpleDateFormat(DAY_FORMAT, Locale.getDefault())

    override fun getItemViewType(position: Int) = when (calendarItems[position]) {
        is DateItem -> DAY_VIEW_TYPE
        is MonthItem -> MONTH_VIEW_TYPE
        is EmptyItem -> EMPTY_VIEW_TYPE
        else -> throw IllegalStateException("Unknown item at position $position")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        DAY_VIEW_TYPE -> {
            val dayView = CalendarDateView(parent.context)
            val dayItemViewHolder = DateItemViewHolder(dayView)

            dayItemViewHolder.dayView.setOnClickListener {
                val adapterPosition = dayItemViewHolder.adapterPosition
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                val dateItem = calendarItems[adapterPosition] as DateItem
                onDateClickHandler.invoke(dateItem.localDate, adapterPosition)
            }

            dayItemViewHolder
        }

        MONTH_VIEW_TYPE -> {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_month, parent, false)
            MonthItemViewHolder(v as TextView)
        }

        EMPTY_VIEW_TYPE -> {
            val v = View(parent.context)
            EmptyItemViewHolder(v)
        }

        else -> throw IllegalStateException("Unknown view type")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            DAY_VIEW_TYPE -> {
                val dateItem = calendarItems[position] as DateItem
                val dateItemViewHolder = holder as DateItemViewHolder
                dateItemViewHolder.dayView.text = dayFormatter.format(dateItem.localDate.toDate())

                if (dateInfoProvider.isDateEnabled(dateItem.localDate)) {
                    if (dateInfoProvider.isDateSelected(dateItem.localDate)) {
                        dateItemViewHolder.itemView.setBackgroundColor(Color.BLUE)
                    } else {
                        dateItemViewHolder.itemView.setBackgroundColor(Color.WHITE)
                    }
                } else {
                    dateItemViewHolder.itemView.setBackgroundColor(Color.GRAY)
                }
            }

            MONTH_VIEW_TYPE -> {
                val monthItem = calendarItems[position] as MonthItem
                val monthItemViewHolder = holder as MonthItemViewHolder

                monthItemViewHolder.textView.text =
                        monthFormatter.format(monthItem.localDate.toDate())
            }
        }
    }

    override fun getItemCount() = calendarItems.size

    fun findMonthItemPosition(localDate: SimpleLocalDate): Int {
        val year = localDate.year
        val month = localDate.month

        return calendarItems.indexOfFirst { item ->
            if (item is MonthItem) {
                if (item.localDate.year == year && item.localDate.month == month) {
                    return@indexOfFirst true
                }
            }

            return@indexOfFirst false
        }
    }

    fun findDateItemPosition(localDate: SimpleLocalDate): Int {
        return calendarItems.indexOfFirst { item ->
            if (item is DateItem) {
                if (item.localDate == localDate) {
                    return@indexOfFirst true
                }
            }

            return@indexOfFirst false
        }
    }

    fun getDateItemsRange(dateFrom: SimpleLocalDate,
        dateTo: SimpleLocalDate
    ): List<SimpleLocalDate> {

        return calendarItems
            .filterIsInstance<DateItem>()
            .filter { dateItem ->
                if (dateItem.localDate in dateFrom..dateTo) {
                    return@filter true
                } else {
                    false
                }
            }
            .map { it.localDate }
    }

    fun setItems(calendarItems: List<CalendarItem>) {
        this.calendarItems.clear()
        this.calendarItems.addAll(calendarItems)
        notifyDataSetChanged()
    }

    fun addNextCalendarItems(nextCalendarItems: List<CalendarItem>) {
        calendarItems.addAll(nextCalendarItems)
        notifyItemRangeInserted(calendarItems.size - nextCalendarItems.size, nextCalendarItems.size)
    }

    fun addPrevCalendarItems(prevCalendarItems: List<CalendarItem>) {
        calendarItems.addAll(0, prevCalendarItems)
        notifyItemRangeInserted(0, prevCalendarItems.size)
    }


    class DateItemViewHolder(val dayView: CalendarDateView) : RecyclerView.ViewHolder(dayView)

    class MonthItemViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    class EmptyItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}