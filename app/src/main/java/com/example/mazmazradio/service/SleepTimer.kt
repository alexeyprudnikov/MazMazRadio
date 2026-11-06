package com.example.mazmazradio.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Таймер сна.
 *
 * Живёт в процессе приложения, а не во ViewModel: свайп задачи не убивает
 * воспроизведение (оно держится медиа-сервисом), а ViewModel — убивает, и
 * «выключить через 30 минут» терялось вместе с экраном.
 *
 * Обратный отсчёт считается от дедлайна, а не суммированием `delay`: так он не
 * уезжает при загрузке потока.
 */
object SleepTimer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _remainingSeconds = MutableStateFlow<Int?>(null)

    /** Остаток в секундах; `null` — таймер не взведён. */
    val remainingSeconds: StateFlow<Int?> = _remainingSeconds.asStateFlow()

    /** Что сделать по истечении (поставить воспроизведение на паузу). Задаёт сервис. */
    var onExpire: (() -> Unit)? = null

    fun arm(minutes: Int) {
        cancel()
        if (minutes <= 0) return

        val deadline = System.currentTimeMillis() + minutes * 60_000L
        job = scope.launch {
            while (true) {
                val left = ((deadline - System.currentTimeMillis()) / 1000L).toInt()
                if (left <= 0) break
                _remainingSeconds.value = left
                // Отмена приходит сюда же: после cancel() остаток кода не выполняется.
                delay(1_000L)
            }
            _remainingSeconds.value = null
            onExpire?.invoke()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _remainingSeconds.value = null
    }
}
