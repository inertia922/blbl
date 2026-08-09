package blbl.cat3399.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerAutoResumePartSwitchTest {
    private val parts =
        listOf(
            PlayerPlaylistItem(bvid = "bv1", cid = 100L, title = "第1话"),
            PlayerPlaylistItem(bvid = "bv1", cid = 200L, title = "第2话"),
            PlayerPlaylistItem(bvid = "bv1", cid = 300L, title = "第3话"),
        )

    @Test
    fun singlePartNeverSwitches() {
        assertNull(
            resolveAutoResumePartSwitchIndex(
                parts = listOf(PlayerPlaylistItem(bvid = "bv1", cid = 100L)),
                partsListIndex = 0,
                autoResumeCancelledByUser = false,
                lastCid = 100L,
                cid = 100L,
            ),
        )
    }

    @Test
    fun lastCidPointingToAnotherPartSwitches() {
        assertEquals(
            2,
            resolveAutoResumePartSwitchIndex(
                parts = parts,
                partsListIndex = 0,
                autoResumeCancelledByUser = false,
                lastCid = 300L,
                cid = 100L,
            ),
        )
    }

    @Test
    fun matchingCurrentCidDoesNotSwitch() {
        assertNull(
            resolveAutoResumePartSwitchIndex(
                parts = parts,
                partsListIndex = 0,
                autoResumeCancelledByUser = false,
                lastCid = 100L,
                cid = 100L,
            ),
        )
    }

    @Test
    fun alreadyOnTargetPartDoesNotSwitch() {
        assertNull(
            resolveAutoResumePartSwitchIndex(
                parts = parts,
                partsListIndex = 2,
                autoResumeCancelledByUser = false,
                lastCid = 300L,
                cid = 100L,
            ),
        )
    }

    @Test
    fun userCancellationDisablesSwitch() {
        assertNull(
            resolveAutoResumePartSwitchIndex(
                parts = parts,
                partsListIndex = 0,
                autoResumeCancelledByUser = true,
                lastCid = 300L,
                cid = 100L,
            ),
        )
    }

    @Test
    fun unknownOrInvalidLastCidDoesNotSwitch() {
        assertNull(
            resolveAutoResumePartSwitchIndex(
                parts = parts,
                partsListIndex = 0,
                autoResumeCancelledByUser = false,
                lastCid = 999L,
                cid = 100L,
            ),
        )
        assertNull(
            resolveAutoResumePartSwitchIndex(
                parts = parts,
                partsListIndex = 0,
                autoResumeCancelledByUser = false,
                lastCid = null,
                cid = 100L,
            ),
        )
        assertNull(
            resolveAutoResumePartSwitchIndex(
                parts = parts,
                partsListIndex = 0,
                autoResumeCancelledByUser = false,
                lastCid = 0L,
                cid = 100L,
            ),
        )
    }
}
