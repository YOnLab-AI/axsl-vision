package com.yonlab.vision; import java.util.Locale;
/** Opt-in policy only: no device commands or inferred accelerator support. */
public final class OneBoardPolicy {private OneBoardPolicy(){} public static boolean detected(String hardware,String board,String model){String v=((hardware==null?"":hardware)+" "+(board==null?"":board)+" "+(model==null?"":model)).toLowerCase(Locale.ROOT);return v.contains("sdm845")||v.contains("aikit");} public static int threads(boolean board,boolean enabled){return board&&enabled?4:2;} }
