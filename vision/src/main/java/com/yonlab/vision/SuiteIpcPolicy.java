package com.yonlab.vision;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/** Pure protocol policy shared by sender/receiver and executable host boundary tests. */
public final class SuiteIpcPolicy {
    public static final int VERSION=1,MAX_PAYLOAD_BYTES=2048;
    private static final String[] ROLES={"lab","access","detect","flow","signage","face","drive","vis","compare"};
    private SuiteIpcPolicy(){}
    public static boolean knownRole(String role){return role!=null&&Arrays.asList(ROLES).contains(role);}
    public static boolean knownPackage(String name){if(name==null||!name.startsWith("com.yonlab."))return false;return knownRole(name.substring(11));}
    public static String packageFor(String role){if(!knownRole(role))throw new IllegalArgumentException("unknown role");return "com.yonlab."+role;}
    public static int bytes(String payload){return payload==null?-1:payload.getBytes(StandardCharsets.UTF_8).length;}
    public static boolean nonce(String value){try{return value!=null&&value.length()==36&&UUID.fromString(value).toString().equals(value);}catch(IllegalArgumentException e){return false;}}
    public static String request(int version,String sender,String target,String actualTarget,String nonce,String payload){
        if(version!=VERSION)return "VERSION";if(!knownPackage(sender)||!knownPackage(target)||!target.equals(actualTarget))return "TARGET";
        if(!nonce(nonce))return "NONCE";int size=bytes(payload);if(size<0||size>MAX_PAYLOAD_BYTES)return "PAYLOAD";return null;
    }
    public static boolean ack(int version,String responder,String destination,String nonce,String echo,String expectedTarget,String expectedSource,String expectedNonce,String expectedPayload){
        return version==VERSION&&knownPackage(responder)&&knownPackage(destination)&&responder.equals(expectedTarget)&&destination.equals(expectedSource)
            &&nonce(nonce)&&nonce.equals(expectedNonce)&&bytes(echo)>=0&&bytes(echo)<=MAX_PAYLOAD_BYTES&&echo.equals(expectedPayload);
    }
}
