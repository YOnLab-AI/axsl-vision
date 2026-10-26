package com.yonlab.vision;
import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
/** Serialize board ownership on a worker; a stale screen cannot restore a newer setting. */
final class BoardLedController {
 static final BoardLedController GLOBAL=new BoardLedController();
 private final BoardLed leds;private long owner;private AtomicBoolean alive;
 BoardLedController(){this(new BoardLed());}BoardLedController(BoardLed led){leds=led;}
 private static BoardLed.Report empty(String status){return new BoardLed.Report(status,"",Collections.emptyMap(),Collections.emptyMap(),Collections.emptyMap(),0);}
 synchronized BoardLed.Report set(long token,AtomicBoolean allowed,boolean on){if(allowed==null||!allowed.get())return empty("CANCELED");if(owner!=token){BoardLed.Report prior=leds.restore();if(prior.dirtyCount>0)return prior;owner=token;alive=allowed;}if(!allowed.get())return leds.restore();BoardLed.Report result=leds.set(on);return allowed.get()?result:leds.restore();}
 synchronized BoardLed.Report restore(long token){return owner==token?leds.restore():empty("STALE_OWNER_IGNORED");}
 synchronized BoardLed.Report restoreInactive(){return alive!=null&&!alive.get()?leds.restore():empty("NO_CHANGES");}
}
