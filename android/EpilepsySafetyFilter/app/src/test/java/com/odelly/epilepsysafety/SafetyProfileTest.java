package com.odelly.epilepsysafety;
import static org.junit.Assert.*;
import org.junit.Test;
public class SafetyProfileTest {
  @Test public void flickerRange(){assertTrue(SafetyProfile.isPotentiallyProvocativeFlicker(5));assertTrue(SafetyProfile.isPotentiallyProvocativeFlicker(30));assertFalse(SafetyProfile.isPotentiallyProvocativeFlicker(4.9));assertFalse(SafetyProfile.isPotentiallyProvocativeFlicker(30.1));}
  @Test public void alphaClamped(){assertEquals(0,SafetyProfile.overlayAlphaFromDim(-1));assertEquals(160,SafetyProfile.overlayAlphaFromDim(80));assertEquals(200,SafetyProfile.overlayAlphaFromDim(100));}
}
