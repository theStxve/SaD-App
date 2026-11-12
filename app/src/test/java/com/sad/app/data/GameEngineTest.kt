package com.sad.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GameEngineTest {

    @Test
    fun xpTableMatchesRarities() {
        assertEquals(25, GameEngine.xpForRarity("common"))
        assertEquals(50, GameEngine.xpForRarity("uncommon"))
        assertEquals(100, GameEngine.xpForRarity("rare"))
        assertEquals(200, GameEngine.xpForRarity("epic"))
        assertEquals(25, GameEngine.xpForRarity("irgendwas"))
    }

    @Test
    fun addonXpRewardOverridesRarity() {
        val place = PlaceEntity("a", "Turm", "Dungeon", "tower", "epic", 0.0, 0.0)
        assertEquals(200, GameEngine.xpFor(place))
        place.xpReward = 333
        assertEquals(333, GameEngine.xpFor(place))
    }
}
