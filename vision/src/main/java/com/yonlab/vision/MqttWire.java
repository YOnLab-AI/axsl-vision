package com.yonlab.vision;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import javax.net.ssl.*;

/** Bounded MQTT 3.1.1 one-shot QoS1 sender. No background reconnection or saved credentials. */
public final class MqttWire implements AutoCloseable {
    public static final int MAX_PACKET=65536;
    public static final class Endpoint {
        public final String host;public final int port;public final boolean tls;
        private Endpoint(String host,int port,boolean tls){this.host=host;this.port=port;this.tls=tls;}
        public static Endpoint parse(String value){
            try{URI uri=new URI(value.trim());String s=uri.getScheme();boolean tls="ssl".equals(s);
                if((!tls&&!"tcp".equals(s))||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath()==null||uri.getPath().isEmpty()))throw new IllegalArgumentException();
                int port=uri.getPort()<0?(tls?8883:1883):uri.getPort();if(port<1||port>65535)throw new IllegalArgumentException();
                return new Endpoint(uri.getHost(),port,tls);
            }catch(Exception e){throw new IllegalArgumentException("브로커 주소를 확인하세요");}
        }
    }
    public static final class Receipt {public final int packetId,bytes;Receipt(int id,int size){packetId=id;bytes=size;}}
    interface AddressResolver {InetAddress resolve(String host)throws UnknownHostException;}
    private static final ThreadFactory DAEMON=r->{Thread t=new Thread(r,"YOnMqttDeadline");t.setDaemon(true);return t;};
    // Native DNS may ignore interruption. Bound concurrency and queued work independently of callers.
    private static final ThreadPoolExecutor DNS=new ThreadPoolExecutor(1,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"YOnMqttDns");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final ScheduledThreadPoolExecutor WATCHDOG=new ScheduledThreadPoolExecutor(1,DAEMON);
    static{DNS.allowCoreThreadTimeOut(true);WATCHDOG.setRemoveOnCancelPolicy(true);}
    private final long timeoutMillis;
    private final AddressResolver resolver;
    public MqttWire(){this(15000L);}
    MqttWire(long timeoutMillis){this(timeoutMillis,InetAddress::getByName);}
    MqttWire(long timeoutMillis,AddressResolver resolver){if(timeoutMillis<1||timeoutMillis>15000L||resolver==null)throw new IllegalArgumentException("Invalid deadline");this.timeoutMillis=timeoutMillis;this.resolver=resolver;}
    private volatile Socket socket;
    private volatile boolean closed,timedOut;
    private boolean started;
    private long deadlineNanos;
    private Future<InetAddress> lookup;
    private ScheduledFuture<?> deadlineTask;
    public Receipt publish(Endpoint endpoint,String clientId,String user,char[] password,String topic,byte[] payload)throws IOException{
        validateTopic(topic);if(payload==null||payload.length>60000)throw new IllegalArgumentException("메시지는 60KB 이하로 입력하세요");
        if(password!=null&&password.length>0&&(user==null||user.isEmpty()))throw new IllegalArgumentException("사용자 이름을 입력하세요");
        Socket raw=new Socket();
        synchronized(this){
            if(closed||started){raw.close();throw new InterruptedIOException("Canceled");}
            started=true;socket=raw;deadlineNanos=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            deadlineTask=WATCHDOG.schedule(this::expire,timeoutMillis,TimeUnit.MILLISECONDS);
        }
        try{
            InetAddress address=resolve(endpoint.host);checkActive();
            raw.connect(new InetSocketAddress(address,endpoint.port),socketTimeout());raw.setSoTimeout(socketTimeout());
            checkActive();
            if(endpoint.tls){
                SSLSocket ssl=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(raw,endpoint.host,endpoint.port,true);
                synchronized(this){if(closed){ssl.close();throw stopped();}socket=ssl;}
                ssl.setSoTimeout(socketTimeout());ssl.startHandshake();checkActive();
                if(!HttpsURLConnection.getDefaultHostnameVerifier().verify(endpoint.host,ssl.getSession()))throw new SSLHandshakeException("Hostname verification failed");
            }
            checkActive();Socket connected=socket;
            if(connected==null)throw stopped();
            return publishConnected(connected.getInputStream(),connected.getOutputStream(),clientId,user,password,topic,payload);
        }catch(IOException failure){
            if(timedOut){SocketTimeoutException timeout=new SocketTimeoutException("MQTT deadline exceeded");timeout.initCause(failure);throw timeout;}
            throw failure;
        }finally{close();}
    }
    private InetAddress resolve(String host)throws IOException{
        Future<InetAddress> request;
        synchronized(this){
            if(closed)throw stopped();
            try{request=DNS.submit(()->resolver.resolve(host));}catch(RejectedExecutionException busy){throw new IOException("DNS resolver busy",busy);}
            lookup=request;
        }
        try{return request.get(remainingNanos(),TimeUnit.NANOSECONDS);}
        catch(TimeoutException expired){expire();throw new SocketTimeoutException("MQTT deadline exceeded");}
        catch(CancellationException canceled){throw stopped();}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();close();InterruptedIOException failure=new InterruptedIOException("Interrupted");failure.initCause(interrupted);throw failure;}
        catch(ExecutionException failed){Throwable cause=failed.getCause();if(cause instanceof IOException)throw (IOException)cause;throw new IOException("DNS resolution failed",cause);}
        finally{synchronized(this){if(lookup==request)lookup=null;}}
    }
    private long remainingNanos()throws IOException{checkActive();return Math.max(1,deadlineNanos-System.nanoTime());}
    private int socketTimeout()throws IOException{return (int)Math.max(1,Math.min(5000,TimeUnit.NANOSECONDS.toMillis(remainingNanos())));}
    private void checkActive()throws IOException{
        if(!closed&&System.nanoTime()-deadlineNanos>=0)expire();
        if(closed)throw stopped();
    }
    private IOException stopped(){return timedOut?new SocketTimeoutException("MQTT deadline exceeded"):new InterruptedIOException("Canceled");}
    private void expire(){synchronized(this){if(closed)return;timedOut=true;}close();}
    static Receipt publishConnected(InputStream input,OutputStream output,String clientId,String user,char[] password,String topic,byte[] payload)throws IOException{
            ByteArrayOutputStream body=new ByteArrayOutputStream();writeUtf(body,"MQTT");body.write(4);int flags=2;
            if(user!=null&&!user.isEmpty())flags|=128;if(password!=null&&password.length>0)flags|=64;
            body.write(flags);write16(body,15);writeUtf(body,clientId);if((flags&128)!=0)writeUtf(body,user);if((flags&64)!=0)writeUtf(body,new String(password));
            send(output,0x10,body.toByteArray());Packet conn=read(input);if(conn.header!=0x20||conn.body.length!=2||conn.body[0]!=0||conn.body[1]!=0)throw new IOException("MQTT connection refused");
            body.reset();writeUtf(body,topic);write16(body,1);body.write(payload);send(output,0x32,body.toByteArray());
            Packet ack=read(input);if(ack.header!=0x40||ack.body.length!=2||ack.body[0]!=0||ack.body[1]!=1)throw new IOException("Invalid MQTT PUBACK");
            // PUBACK confirms delivery; closing the connection cannot revoke that receipt.
            try{send(output,0xe0,new byte[0]);}catch(IOException ignored){}
            return new Receipt(1,payload.length);
    }
    public static void validateTopic(String topic){if(topic==null||topic.isEmpty()||topic.indexOf('#')>=0||topic.indexOf('+')>=0||topic.indexOf('\0')>=0||topic.getBytes(StandardCharsets.UTF_8).length>512)throw new IllegalArgumentException("보낼 토픽을 확인하세요");}
    static void write16(OutputStream out,int n)throws IOException{out.write(n>>>8);out.write(n&255);}
    static void writeUtf(OutputStream out,String value)throws IOException{if(value==null||value.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid MQTT string");byte[] bytes=value.getBytes(StandardCharsets.UTF_8);if(bytes.length>65535)throw new IllegalArgumentException("String too long");write16(out,bytes.length);out.write(bytes);}
    static void send(OutputStream out,int header,byte[] body)throws IOException{if(body.length>MAX_PACKET)throw new IOException("Packet too large");out.write(header);int remaining=body.length;do{int digit=remaining%128;remaining/=128;if(remaining>0)digit|=128;out.write(digit);}while(remaining>0);out.write(body);out.flush();}
    static final class Packet {final int header;final byte[] body;Packet(int h,byte[] b){header=h;body=b;}}
    static Packet read(InputStream input)throws IOException{int header=input.read();if(header<0)throw new EOFException();int size=0,multiplier=1;boolean complete=false;
        for(int n=0;n<4;n++){int digit=input.read();if(digit<0)throw new EOFException();size+=(digit&127)*multiplier;if(size>MAX_PACKET)throw new IOException("Packet too large");if((digit&128)==0){complete=true;break;}multiplier*=128;}
        if(!complete)throw new IOException("Invalid remaining length");byte[] body=new byte[size];int offset=0;while(offset<size){int n=input.read(body,offset,size-offset);if(n<0)throw new EOFException();offset+=n;}return new Packet(header,body);
    }
    @Override public void close(){
        Socket current;Future<InetAddress> request;ScheduledFuture<?> timer;
        synchronized(this){closed=true;current=socket;socket=null;request=lookup;lookup=null;timer=deadlineTask;deadlineTask=null;}
        if(request!=null){request.cancel(true);DNS.purge();}
        if(timer!=null)timer.cancel(false);
        if(current!=null)try{current.close();}catch(IOException ignored){}
    }
}
