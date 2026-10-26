package com.yonlab.vision;
import org.junit.Test;import static org.junit.Assert.*;
public class AgeHeadOnlyTest {
 @Test public void currentRuntimeAgeOutputDoesNotPopulateGender(){Observations.Face face=new Observations.Face();face.genderIndex=1;face.femaleProbability=.1f;face.maleProbability=.9f;AgeGenderPolicy.applyAge(new float[]{.37f},face);assertEquals(37,face.ageYears,.0001);assertEquals(-1,face.genderIndex);assertTrue(Float.isNaN(face.femaleProbability));assertTrue(Float.isNaN(face.maleProbability));}
 @Test(expected=IllegalArgumentException.class)public void invalidAgeIsNotInvented(){AgeGenderPolicy.applyAge(new float[]{Float.NaN},new Observations.Face());}
}
