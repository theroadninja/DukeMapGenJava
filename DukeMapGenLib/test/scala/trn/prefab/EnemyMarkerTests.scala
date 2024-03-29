package trn.prefab

import org.junit.{Test, Assert}

class EnemyMarkerTests {

  @Test
  def testEnemyMarker(): Unit = {
    val em = EnemyMarker(EnemyMarker.LizardTrooper)
    Assert.assertFalse(em.turnIntoRespawn)
    Assert.assertFalse(em.locationFuzzing)
    Assert.assertEquals(Set(Enemy.LizTroop), em.enemyList)

    val em2 = EnemyMarker(EnemyMarker.LocationFuzzing | EnemyMarker.Octabrain | EnemyMarker.PigCop)
    Assert.assertFalse(em2.turnIntoRespawn)
    Assert.assertTrue(em2.locationFuzzing)
    Assert.assertEquals(Set(Enemy.OctaBrain, Enemy.PigCop), em2.enemyList)
  }
}
