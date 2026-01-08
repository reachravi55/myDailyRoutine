package com.reachravi55.mydailyroutine.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

fun LocalDate.key(): String = format(fmt)
fun parseDateKey(key: String): LocalDate = LocalDate.parse(key, fmt)
