package com.odelly.epilepsysafety;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FlickerMitigationEngineTest {
  @Test public void ignoresSlowChanges() {
    FlickerMitigationEngine e = new FlickerMitigationEngine(20, 3, 30);
    assertFalse(e.update(0, 0));
    assertFalse(e.update(255, 1000));
  }

  @Test public void detectsRepeatedRapidHighContrastTransitions() {
    FlickerMitigationEngine e = new FlickerMitigationEngine(20, 3, 30);
    e.update(0, 0);
    e.update(255, 33);
    e.update(0, 66);
    assertTrue(e.update(255, 99));
  }
}
