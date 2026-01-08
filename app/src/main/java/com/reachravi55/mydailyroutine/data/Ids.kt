package com.reachravi55.mydailyroutine.data

import java.util.UUID

object Ids {
    fun id(): String = UUID.randomUUID().toString()
}
