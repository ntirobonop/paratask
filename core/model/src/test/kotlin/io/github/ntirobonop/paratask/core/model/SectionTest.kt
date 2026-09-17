package io.github.ntirobonop.paratask.core.model

import java.time.Instant
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SectionTest {
    @Test
    fun idsMustBeNonBlankAndGeneratedIdsAreDistinct() {
        assertThrows(IllegalArgumentException::class.java) { SectionId(" ") }
        assertNotEquals(SectionId.random(), SectionId.random())
    }

    @Test
    fun sectionNameMustBeNonBlank() {
        assertThrows(IllegalArgumentException::class.java) {
            Section(SectionId("section"), ProjectId("project"), "  ", Instant.EPOCH, Instant.EPOCH)
        }
    }
}
