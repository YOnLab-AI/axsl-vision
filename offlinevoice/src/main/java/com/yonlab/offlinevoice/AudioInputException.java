package com.yonlab.offlinevoice;
import java.io.IOException;

/** Stable diagnostic codes; no source URI or recognized text is put in messages. */
public final class AudioInputException extends IOException {
 public final String code;
 public String temporaryInputState="UNKNOWN";
 public Boolean extractorReleased,decoderReleased;
 public Boolean selectedSourceClosed,providerOpenFinished;
 public AudioInputException(String code){super(code);this.code=code;}
 public AudioInputException(String code,Throwable cause){super(code,cause);this.code=code;}
 AudioInputException cleanup(String state,Boolean extractor,Boolean decoder){temporaryInputState=state;extractorReleased=extractor;decoderReleased=decoder;return this;}
 AudioInputException provider(Boolean closed,boolean finished){selectedSourceClosed=closed;providerOpenFinished=finished;return this;}
}
