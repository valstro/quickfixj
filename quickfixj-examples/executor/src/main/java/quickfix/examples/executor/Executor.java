package quickfix.examples.executor;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.DefaultSessionFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.FixVersions;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.mina.acceptor.AbstractSocketAcceptor;

public class Executor {
    private final static Logger log = LoggerFactory.getLogger(Executor.class);

    public static void main(String[] args) throws Exception {
        new Executor().run();
    }

    public void run() throws Exception {
        SessionSettings settings = new SessionSettings();
        // Default settings 
        settings.setString("StartTime", "00:00:00");
        settings.setString("EndTime", "00:00:00");
        settings.setString("FileStorePath", "target/data/executor");
        settings.setString("HeartBeatInt", "30");
        settings.setString("UseDataDictionary", "Y");
        settings.setString("ValidOrderTypes", "1,2,F");

        // Session settings for EXEC->BANZAI
        SessionID id1 = new SessionID(FixVersions.BEGINSTRING_FIX44, "EXEC", "BANZAI");
        settings.setString(id1, SessionFactory.SETTING_CONNECTION_TYPE, SessionFactory.ACCEPTOR_CONNECTION_TYPE);
        settings.setString(id1, "BeginString", FixVersions.BEGINSTRING_FIX44);
        settings.setString(id1,"ConnectionType", "acceptor"); 
        settings.setString(id1,"SocketAcceptPort", "9880"); 
        settings.setString(id1,"SocketConnectAddress", "0.0.0.0"); 
        settings.setString(id1,"SenderCompID", "EXEC"); 
        settings.setString(id1,"TargetCompID", "BANZAI"); 

        Application application = new Application();
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();
        DefaultSessionFactory sessionFactory = new DefaultSessionFactory(application, messageStoreFactory, logFactory);
        SocketAcceptor acceptor = new SocketAcceptor(sessionFactory, settings);
        acceptor.start();

        log.info("=============");
        log.info("acceptor.getManagedSessions: {}", acceptor.getManagedSessions());
        log.info("acceptor.getAcceptorAddresses: {}", acceptor.getAcceptorAddresses());
        log.info("acceptor.getEndpoints: {}", acceptor.getEndpoints());
        log.info("executor.settings: {}", acceptor.getSettings());
        log.info("=============");
        
        System.out.println("At this point you can connect with EXEC->BANZAI on 9880");
        System.out.println("But not EXEC2->BANZAI2 ...");
        System.out.println("PRESS ANY KEY TO ADD A SESSION FOR EXEC2->BANZAI2");
        System.in.read();

        SessionID id2 = new SessionID(FixVersions.BEGINSTRING_FIX44, "EXEC2", "BANZAI2");
        settings.setString(id2, SessionFactory.SETTING_CONNECTION_TYPE, SessionFactory.ACCEPTOR_CONNECTION_TYPE);
        settings.setString(id2, "BeginString", FixVersions.BEGINSTRING_FIX44);
        settings.setString(id2,"ConnectionType", "acceptor"); 
        settings.setString(id2,"SocketAcceptPort", "9880"); 
        settings.setString(id2,"SocketConnectAddress", "0.0.0.0"); 
        settings.setString(id2,"SenderCompID", "EXEC2"); 
        settings.setString(id2,"TargetCompID", "BANZAI2"); 

        /* === Here's where the fun starts ... === */
        // We don't want to stop & restart:
        //acceptor.stop();
        //acceptor.start();
        /// Or use dynamic things (which don't work for our use case anyway):
        //Session newSession = sessionFactory.create(newId, settings); 
        //acceptor.addDynamicSession(newSession);
        //
        // Reading the code for AbstractSocketAcceptor, we can see it sets everthing up once initially, but doesn't 
        // provide a way to append more session info later -- unclear why but we can get around it with some hacking
        // until we understand it better:
        // Reflect on :
        //   private void createSessions(SessionSettings settings, boolean continueInitOnError) throws ConfigError {
        java.lang.reflect.Method meth = AbstractSocketAcceptor.class.getDeclaredMethod("createSessions", SessionSettings.class, boolean.class); 
        meth.setAccessible(true);
        meth.invoke(acceptor, settings, false);
        /* === ?? === */

        log.info("=============");
        log.info("acceptor.getManagedSessions: {}", acceptor.getManagedSessions());
        log.info("acceptor.getAcceptorAddresses: {}", acceptor.getAcceptorAddresses());
        log.info("acceptor.getEndpoints: {}", acceptor.getEndpoints());
        log.info("executor.settings: {}", acceptor.getSettings());
        log.info("=============");
        System.out.println("Press any key to exit");
        System.in.read();
        acceptor.stop();
    }
}
