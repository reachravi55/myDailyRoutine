package com.reachravi55.mydailyroutine.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    fun todayKey(): String = LocalDate.now().format(fmt)
    fun parseKey(key: String): LocalDate = LocalDate.parse(key, fmt)
    fun formatKey(date: LocalDate): String = date.format(fmt)
}
