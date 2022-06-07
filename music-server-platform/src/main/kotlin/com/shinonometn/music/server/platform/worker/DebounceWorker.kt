package com.shinonometn.music.server.platform.worker

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

open class DebounceWorker {
    private val logger = LoggerFactory.getLogger(DebounceWorker::class.java)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val workRegistry = ConcurrentHashMap<String, Work>()

    private class Work(
        val key: String,
        var current: Job,
        var next: Job?,
        val onAllFinish: (String) -> Unit,
        val lock: ReentrantLock = ReentrantLock()
    ) {
        private val logger = LoggerFactory.getLogger(DebounceWorker::class.java)

        fun tryEnqueue(newJob: Job) : Boolean {
            val current = this.current
            val next = this.next
            if (!lock.tryLock()) {
                logger.info("Another worker is modifying work context@{}. Ignore.", key)
                return false
            }

            try {
                if ((current.isCompleted || current.isCancelled) && next != null) {
                    this.current = next
                    this.next = newJob
                    next.start()
                    logger.debug("Work enqueue@{} success.", key)
                    return true
                }

                if (!current.isActive) { current.start() }

                this.next = newJob
                if(next == null) logger.info("Work has been replaced @{}.", key)
                logger.debug("Work enqueue@{} success.", key)
                return true
            } finally {
                lock.unlock()
            }
        }

        fun finish() {
            val next = this.next
            if (!lock.tryLock()) return logger.info("Another worker is modifying work context@{}. Ignore.", key)

            try {
                if (next != null) {
                    this.current = next
                    next.start()
                    logger.debug("Next work@{} started.", key)
                    return
                } else {
                    logger.debug("All work@{} finished.", key)
                    onAllFinish(key)
                    return
                }
            } finally {
                lock.unlock()
            }
        }
    }

    private fun onAllWorkFinished(key: String) {
        workRegistry.remove(key)
        logger.debug("Work@{} been remove from registry.", key)
    }

    protected fun tryPushNewJob(key: String, block: suspend CoroutineScope.() -> Unit): Job? {
        val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } catch (e: Exception) {
                logger.error("Work@{} meet exception.", key, e)
            } finally {
                workRegistry[key]?.finish()
            }
        }
        val work = workRegistry.computeIfAbsent(key) { Work(it, job, null, this::onAllWorkFinished) }
        if (!work.lock.tryLock()) return null
        if (work.current == job) {
            job.start()
            return job
        }
        if(!work.tryEnqueue(job)) return null
        return job
    }
}