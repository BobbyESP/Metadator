package com.bobbyesp.metadator.core.domain.editor

import com.bobbyesp.metadator.core.model.MultiValueMode
import com.bobbyesp.metadator.tags.api.ArtworkChange
import com.bobbyesp.metadator.tags.api.EmbeddedPicture
import com.bobbyesp.metadator.tags.api.TagChanges
import com.bobbyesp.metadator.tags.api.TagField
import com.bobbyesp.metadator.tags.api.TagMap
import com.bobbyesp.metadator.tags.api.TagSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorLogicTest {

    @Test
    fun `positions read both styles`() {
        assertEquals(
            Position("3", "12"),
            TrackPositions.read(TagMap.of("TRACKNUMBER" to listOf("3/12")), TagField.TrackNumber),
        )
        assertEquals(
            Position("3", "12"),
            TrackPositions.read(
                TagMap.of("TRACKNUMBER" to listOf("3"), "TRACKTOTAL" to listOf("12")),
                TagField.TrackNumber,
            ),
        )
        assertEquals(Position("A1", ""), TrackPositions.read(TagMap.of("TRACKNUMBER" to listOf("A1")), TagField.TrackNumber))
    }

    @Test
    fun `combined style writes n over total`() {
        val tags = TagMap.of("TRACKNUMBER" to listOf("1"))
        assertEquals(
            mapOf("TRACKNUMBER" to listOf("3/12")),
            TrackPositions.write(tags, TagField.TrackNumber, Position("3", "12"), PositionStyle.Combined),
        )
    }

    @Test
    fun `separate style reuses the file's total key`() {
        val tags = TagMap.of("TRACKNUMBER" to listOf("1"), "TOTALTRACKS" to listOf("10"))
        assertEquals(
            mapOf("TRACKNUMBER" to listOf("3"), "TOTALTRACKS" to listOf("12")),
            TrackPositions.write(tags, TagField.TrackNumber, Position("3", "12"), PositionStyle.Separate),
        )
    }

    @Test
    fun `style follows the format`() {
        assertEquals(PositionStyle.Separate, TrackPositions.detectStyle(TagMap.Empty, "flac"))
        assertEquals(PositionStyle.Combined, TrackPositions.detectStyle(TagMap.Empty, "mp3"))
        assertEquals(
            PositionStyle.Separate,
            TrackPositions.detectStyle(TagMap.of("TRACKTOTAL" to listOf("9")), "mp3"),
        )
    }

    private val snapshot =
        TagSnapshot(
            TagMap.of("TITLE" to listOf("a"), "GENRE" to listOf("Pop")),
            listOf(EmbeddedPicture(byteArrayOf(1), "image/jpeg")),
        )

    @Test
    fun `a draft knows what changed and can revert it`() {
        val draft = TagDraft(snapshot).set("TITLE", listOf("b")).set("GENRE", listOf("Pop"))

        assertTrue(draft.isDirty)
        assertEquals(setOf("TITLE"), draft.changedKeys)
        assertTrue(draft.isChanged("TITLE"))
        assertFalse(draft.isChanged("GENRE"))
        assertFalse(draft.revert("TITLE").isDirty)
    }

    @Test
    fun `removing a cover that does not exist is no change`() {
        val noCover = TagDraft(snapshot.copy(pictures = emptyList())).removeCover()
        assertEquals(ArtworkChange.Unchanged, noCover.artwork)
        val removed = TagDraft(snapshot).removeCover()
        assertNull(removed.cover)
        assertTrue(removed.isDirty)
    }

    @Test
    fun `joined mode joins only changed multi-value fields`() {
        val changes =
            TagChanges(
                fields =
                    mapOf(
                        "ARTIST" to listOf("A", "B"),
                        "TITLE" to listOf("x"),
                        "COMMENT" to listOf("1", "2"),
                    )
            )
        val joined = changes.withMultiValueMode(MultiValueMode.Joined, "; ")
        assertEquals(listOf("A; B"), joined.fields["ARTIST"])
        assertEquals(listOf("1", "2"), joined.fields["COMMENT"])
        assertEquals(changes, changes.withMultiValueMode(MultiValueMode.Separate, "; "))
    }

    @Test
    fun `typed values split on the separator`() {
        assertEquals(listOf("A", "B"), splitValues("A ; B;", "; "))
        assertEquals(listOf("Tyler, The Creator"), splitValues("Tyler, The Creator", "; "))
    }
}
