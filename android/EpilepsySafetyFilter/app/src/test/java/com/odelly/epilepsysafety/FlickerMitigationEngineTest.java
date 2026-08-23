package com.odelly.epilepsysafety;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlickerMitigationEngineTest {
  @Test public void detectsRepeatedRapidHighContrastTransitions() {
    FlickerMitigationEngine e = new FlickerMitigationEngine(20, 5, 30, 3);
    assertFalse(e.update(0, 0));
    assertFalse(e.update(255, 33));
    assertFalse(e.update(0, 66));
    assertTrue(e.update(255, 99));
  }

  @Test public void ignoresSlowTransitionsOutsideTemporalWindow() {
    FlickerMitigationEngine e = new FlickerMitigationEngine(20, 5, 30, 3);
    e.update(0, 0);
    e.update(255, 500);
    e.update(0, 1000);
    assertFalse(e.update(255, 1500));
  }

  @Test public void ignoresSmallLuminanceChanges() {
    FlickerMitigationEngine e = new FlickerMitigationEngine(30, 5, 30, 3);
    e.update(100, 0);
    e.update(150, 33);
    e.update(100, 66);
    assertFalse(e.update(150, 99));
  }

  @Test public void maximumSensitivityCanTriggerQuickly() {
    FlickerMitigationEngine e = new FlickerMitigationEngine(5, 5, 30, 2);
    e.update(0, 0);
    e.update(255, 34);
    assertTrue(e.update(0, 68));
  }
}
