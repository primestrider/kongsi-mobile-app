package com.primestrider.kotlinstarter.feature.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/** A state list that survives rotation and process recreation; items must be saveable (String, Boolean, Int…). */
@Composable
fun <T : Any> rememberSaveableStateList(vararg initial: T): SnapshotStateList<T> =
    rememberSaveable(
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { initial.toList().toMutableStateList() }
