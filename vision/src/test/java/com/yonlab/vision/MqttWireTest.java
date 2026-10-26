package com.yonlab.vision;
import org.junit.Test;import static org.junit.Assert.*;import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.util.concurrent.*;
public final class MqttWireTest {
 @Test public void endpointValidation(){assertEquals(8883,MqttWire.Endpoint.parse("ssl://example.com").port);assertEquals(1883,MqttWire.Endpoint.parse("tcp://localhost").port);for(String s:new String[]{"https://x","tcp://x:0","tcp://u:p@x","tcp://x/a","tcp://x?q=a","tcp://x#x","x"})try{MqttWire.Endpoint.parse(s);fail(s);}catch(IllegalArgumentException expected){}}
 @Test public void rejectWildcardTopics(){for(String s:new String[]{"","a/#","+","a\0b"})try{MqttWire.validateTopic(s);fail(s);}catch(IllegalArgumentException expected){}MqttWire.validateTopic("yon/방문자");}
 @Test public void boundedPacketRoundTrip()throws Exception{byte[] data=new byte[16384];data[12]=42;ByteArrayOutputStream out=new ByteArrayOutputStream();MqttWire.send(out,0x30,data);MqttWire.Packet p=MqttWire.read(new ByteArrayInputStream(out.toByteArray()));assertArrayEquals(data,p.body);assertEquals(0x30,p.header);}
 @Test public void rejectOversizeAndTruncated(){for(byte[] data:new byte[][]{{0x30,(byte)128,(byte)128,4},{0x30,5,1},{0x30,(byte)128,(byte)128,(byte)128,(byte)128}})try{MqttWire.read(new ByteArrayInputStream(data));fail();}catch(IOException expected){}}
 @Test public void brokerConfirmsActualQosOnePublish()throws Exception{try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){ExecutorService worker=Executors.newSingleThreadExecutor();Future<byte[]> got=worker.submit(()->{try(Socket s=server.accept()){s.setSoTimeout(5000);MqttWire.Packet connect=MqttWire.read(s.getInputStream());assertEquals(0x10,connect.header);MqttWire.send(s.getOutputStream(),0x20,new byte[]{0,0});MqttWire.Packet message=MqttWire.read(s.getInputStream());assertEquals(0x32,message.header);MqttWire.send(s.getOutputStream(),0x40,new byte[]{0,1});assertEquals(0xe0,MqttWire.read(s.getInputStream()).header);return message.body;}});try(MqttWire client=new MqttWire()){MqttWire.Receipt receipt=client.publish(MqttWire.Endpoint.parse("tcp://127.0.0.1:"+server.getLocalPort()),"yon-test","",null,"yon/test","검증".getBytes(StandardCharsets.UTF_8));assertEquals(6,receipt.bytes);}byte[] bytes=got.get(10,TimeUnit.SECONDS);assertTrue(bytes.length>6);worker.shutdownNow();}}
 @Test public void closedBeforeConnectCannotSend()throws Exception{MqttWire client=new MqttWire();client.close();try{client.publish(MqttWire.Endpoint.parse("tcp://127.0.0.1:1"),"yon","",null,"a",new byte[0]);fail();}catch(InterruptedIOException expected){}}
 @Test public void acknowledgedPublishSurvivesDisconnectWriteFailure()throws Exception{
  InputStream input=new ByteArrayInputStream(new byte[]{0x20,2,0,0,0x40,2,0,1});
  OutputStream output=new OutputStream(){
   @Override public void write(int value)throws IOException{if(value==0xe0)throw new IOException("disconnect failed");}
  };
  MqttWire.Receipt receipt=MqttWire.publishConnected(input,output,"yon-test","",null,"yon/test",new byte[]{1,2,3});
  assertEquals(1,receipt.packetId);assertEquals(3,receipt.bytes);
 }
 @Test public void totalDeadlineStopsBrokerThatNeverAcknowledges()throws Exception{assertBrokerDeadline(false);}
 @Test public void totalDeadlineStopsSlowTrickleBelowReadTimeout()throws Exception{assertBrokerDeadline(true);}
 private static void assertBrokerDeadline(boolean trickle)throws Exception{
  ExecutorService worker=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"mqtt-test-broker");t.setDaemon(true);return t;});
  try(ServerSocket server=new ServerSocket(0,1,InetAddress.getLoopbackAddress());MqttWire client=new MqttWire(350)){
   Future<?> broker=worker.submit(()->{try(Socket s=server.accept()){
    s.setSoTimeout(3000);MqttWire.read(s.getInputStream());
    if(trickle){OutputStream out=s.getOutputStream();out.write(new byte[]{0x20,(byte)0x80,(byte)0x80,1});out.flush();while(!Thread.currentThread().isInterrupted()){out.write(0);out.flush();Thread.sleep(30);}}
    else{s.getInputStream().read();}
   }catch(IOException expected){}catch(InterruptedException stopped){Thread.currentThread().interrupt();}});
   long before=System.nanoTime();Future<MqttWire.Receipt> result=worker.submit(()->client.publish(MqttWire.Endpoint.parse("tcp://127.0.0.1:"+server.getLocalPort()),"yon-test","",null,"yon/test",new byte[]{1}));
   try{result.get(2,TimeUnit.SECONDS);fail("Broker without a complete ACK must time out");}
   catch(ExecutionException expected){assertTrue(expected.getCause().toString(),expected.getCause() instanceof SocketTimeoutException);}
   assertTrue("Caller exceeded total deadline allowance",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-before)<1800);
   broker.cancel(true);
  }finally{worker.shutdownNow();}
 }
 @Test public void totalDeadlineBoundsBlockedDnsCallerEvenIfResolverIgnoresInterrupt()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"mqtt-test-dns");t.setDaemon(true);return t;});
  MqttWire.AddressResolver resolver=host->{entered.countDown();while(release.getCount()>0)try{release.await();}catch(InterruptedException ignored){}return InetAddress.getLoopbackAddress();};
  try(MqttWire client=new MqttWire(250,resolver)){
   Future<MqttWire.Receipt> result=worker.submit(()->client.publish(MqttWire.Endpoint.parse("tcp://delayed.invalid:1883"),"yon-test","",null,"yon/test",new byte[]{1}));
   assertTrue(entered.await(1,TimeUnit.SECONDS));
   try{result.get(2,TimeUnit.SECONDS);fail("DNS caller must not wait for native resolution indefinitely");}
   catch(ExecutionException expected){assertTrue(expected.getCause().toString(),expected.getCause() instanceof SocketTimeoutException);}
   assertEquals("Resolution can still be running after caller timeout",1,release.getCount());
  }finally{release.countDown();worker.shutdownNow();}
 }
 @Test public void closeCancelsDnsCallerWithoutWaitingForNativeResolution()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
  ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"mqtt-test-cancel");t.setDaemon(true);return t;});
  MqttWire.AddressResolver resolver=host->{entered.countDown();try{while(release.getCount()>0)try{release.await();}catch(InterruptedException ignored){}return InetAddress.getLoopbackAddress();}finally{finished.countDown();}};
  try(MqttWire client=new MqttWire(15000,resolver)){
   Future<MqttWire.Receipt> result=worker.submit(()->client.publish(MqttWire.Endpoint.parse("tcp://delayed.invalid:1883"),"yon-test","",null,"yon/test",new byte[]{1}));
   assertTrue(entered.await(1,TimeUnit.SECONDS));long before=System.nanoTime();client.close();
   try{result.get(1,TimeUnit.SECONDS);fail("Canceled DNS caller must stop");}catch(ExecutionException expected){assertTrue(expected.getCause() instanceof InterruptedIOException);assertFalse(expected.getCause() instanceof SocketTimeoutException);}
   assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-before)<800);assertEquals(1,release.getCount());
  }finally{release.countDown();assertTrue(finished.await(1,TimeUnit.SECONDS));worker.shutdownNow();}
 }
 @Test public void dnsQueueRejectsExcessRequestsAndCancellationFreesQueuedSlot()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);
  ExecutorService worker=Executors.newFixedThreadPool(4,r->{Thread t=new Thread(r,"mqtt-test-dns-bound");t.setDaemon(true);return t;});
  MqttWire.AddressResolver resolver=host->{entered.countDown();try{while(release.getCount()>0)try{release.await();}catch(InterruptedException ignored){}return InetAddress.getLoopbackAddress();}finally{finished.countDown();}};
  MqttWire first=new MqttWire(3000,resolver),second=new MqttWire(3000,resolver),third=new MqttWire(3000,resolver);
  try{
   worker.submit(()->first.publish(MqttWire.Endpoint.parse("tcp://blocked.invalid:1883"),"yon-test","",null,"yon/test",new byte[]{1}));assertTrue(entered.await(1,TimeUnit.SECONDS));
   ExecutorCompletionService<MqttWire.Receipt> waiting=new ExecutorCompletionService<>(worker);
   waiting.submit(()->second.publish(MqttWire.Endpoint.parse("tcp://queued.invalid:1883"),"yon-test","",null,"yon/test",new byte[]{1}));
   waiting.submit(()->third.publish(MqttWire.Endpoint.parse("tcp://excess.invalid:1883"),"yon-test","",null,"yon/test",new byte[]{1}));
   Future<MqttWire.Receipt> rejected=waiting.poll(1,TimeUnit.SECONDS);assertNotNull("One running resolver plus one queued request must reject excess work",rejected);
   try{rejected.get();fail("Excess DNS request must fail promptly");}catch(ExecutionException expected){assertTrue(expected.getCause() instanceof IOException);assertEquals("DNS resolver busy",expected.getCause().getMessage());}
   second.close();third.close();
   try(MqttWire afterCancel=new MqttWire(250,resolver)){
    try{afterCancel.publish(MqttWire.Endpoint.parse("tcp://slot-reused.invalid:1883"),"yon-test","",null,"yon/test",new byte[]{1});fail();}
    catch(SocketTimeoutException expected){} // Accepted into the released queue, then caller deadline elapsed.
   }
  }finally{first.close();second.close();third.close();release.countDown();assertTrue(finished.await(1,TimeUnit.SECONDS));worker.shutdownNow();}
 }
}
