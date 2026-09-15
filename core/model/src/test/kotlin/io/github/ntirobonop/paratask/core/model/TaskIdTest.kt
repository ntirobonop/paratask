package io.github.ntirobonop.paratask.core.model

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskIdTest {
    @Test
    fun randomIdsAreUnique() {
        assertNotEquals(TaskId.random(), TaskId.random())
    }

    @Test
    fun blankIdIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            TaskId(" ")
        }
    }
}
