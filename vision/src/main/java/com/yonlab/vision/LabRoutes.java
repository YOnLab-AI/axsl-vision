package com.yonlab.vision;
import java.util.*;
final class LabRoutes {
 private LabRoutes(){}
 static boolean photoTool(String value){return Arrays.asList("qr","pet","ball","ocr","pose").contains(value);}
 static String[] otherRoles(String[] roles,String self){LinkedHashSet<String> out=new LinkedHashSet<>();if(roles!=null)for(String role:roles)if(role!=null&&!role.equals(self))out.add(role);return out.toArray(new String[0]);}
}
