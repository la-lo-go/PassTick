package org.ligi.passandroid.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.ligi.passandroid.json_adapter.ColorAdapter
import org.ligi.passandroid.json_adapter.ZonedTimeAdapter

fun createPassMoshi(): Moshi = Moshi.Builder()
    .add(ZonedTimeAdapter())
    .add(ColorAdapter())
    .addLast(KotlinJsonAdapterFactory())
    .build()

