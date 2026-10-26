package com.yonlab.vision;

/** Engine-independent values; bounding boxes use normalized upright image coordinates. */
public final class Observations {
    private Observations(){}
    public static class Face {
        public float left,top,right,bottom;
        public float yaw=Float.NaN,pitch=Float.NaN,roll=Float.NaN;
        public float leftEye=-1,rightEye=-1,smile=-1,mouthOpen=-1;
        public float leftEyeAspect=Float.NaN,rightEyeAspect=Float.NaN;public int landmarkCount;
        public int trackingId=-1;
        public float[] embedding;
        public String label="";
        public float similarity=Float.NaN;
        public int expressionIndex=-1;
        public float expressionConfidence=Float.NaN;
        public float[] expressionProbabilities;
        public float ageYears=Float.NaN,femaleProbability=Float.NaN,maleProbability=Float.NaN;
        public int genderIndex=-1;
        public Face(){}
        public Face(float l,float t,float r,float b){left=l;top=t;right=r;bottom=b;}
    }
    public static class Detection {
        public float left,top,right,bottom,score;
        public int classId,trackingId=-1;
        public String label;
        public Detection(){}
        public Detection(int id,String label,float score,float l,float t,float r,float b){classId=id;this.label=label;this.score=score;left=l;top=t;right=r;bottom=b;}
    }
}
