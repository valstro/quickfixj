package quickfix.examples.executor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import quickfix.FixVersions;
import quickfix.Session;
import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.DefaultSessionFactory;
import quickfix.mina.acceptor.AbstractSocketAcceptor;
import quickfix.SessionFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

import javax.management.JMException;
import javax.management.ObjectName;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.beans.PropertyChangeSupport;

public class Executor {
    private final static Logger log = LoggerFactory.getLogger(Executor.class);

    public static void main(String[] args) throws Exception {
        try {
            InputStream inputStream = getSettingsInputStream(args);
            SessionSettings settings = new SessionSettings(inputStream);
            inputStream.close();
            new Executor().run(settings);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void run(SessionSettings settings) throws Exception { 
        Application application = new Application(settings);
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true);
        MessageFactory messageFactory = new DefaultMessageFactory();
        DefaultSessionFactory sessionFactory = new DefaultSessionFactory(application, messageStoreFactory, logFactory);
        SocketAcceptor acceptor = new SocketAcceptor(sessionFactory, settings);
        acceptor.start();

        log.info("=============");
        log.info("Sessions are: {}", acceptor.getManagedSessions());
        SessionID sessionId = new SessionID(FixVersions.BEGINSTRING_FIX44, "EXEC", "BANZAI");
        log.info("SessionID {}", sessionId);
        Session session = Session.lookupSession(sessionId);
        log.info("Session is {}", session);
        log.info("Session.getRemoteAddress {}", session.getRemoteAddress());
        log.info("acceptor.getAcceptorAddresses: {}", acceptor.getAcceptorAddresses());
        log.info("acceptor.getEndpoints: {}", acceptor.getEndpoints());
        log.info("executor.settings: {}", acceptor.getSettings());
        log.info("=============");

        System.out.println("PRESS ANY KEY TO ADD A SESSION FOR EXEC2->BANZAI2");

        System.in.read();
        SessionID newId = new SessionID(FixVersions.BEGINSTRING_FIX44, "EXEC2", "BANZAI2");
        settings.setString(newId, SessionFactory.SETTING_CONNECTION_TYPE, SessionFactory.ACCEPTOR_CONNECTION_TYPE);
        settings.setString(newId, "BeginString", FixVersions.BEGINSTRING_FIX44);
        settings.setString(newId,"ConnectionType", "acceptor"); 
        settings.setString(newId,"SocketAcceptPort", "9880"); 
        settings.setString(newId,"SocketConnectAddress", "0.0.0.0"); 
        settings.setString(newId,"SenderCompID", "EXEC2"); 
        settings.setString(newId,"TargetCompID", "BANZAI2"); 
        settings.setString(newId,"AcceptorTempalate", "Y"); 

        /* === ?? === */
        // We don't want to stop & restart:
        //acceptor.stop();
        //acceptor.start();
        /// Or use dynamic things (which don't work for our use case anyway):
        //Session newSession = sessionFactory.create(newId, settings); 
        //acceptor.addDynamicSession(newSession);
        //   private void createSessions(SessionSettings settings, boolean continueInitOnError) throws ConfigError {
        java.lang.reflect.Method meth = AbstractSocketAcceptor.class.getDeclaredMethod("createSessions", SessionSettings.class, boolean.class); 
        meth.setAccessible(true);
        System.out.println("===> Start invoke <====");
        meth.invoke(acceptor, settings, false);
        System.out.println("===> End invoke <====");
        /* === ?? === */

        while(true) { 
            log.info("acceptor.getAcceptorAddresses: {}", acceptor.getAcceptorAddresses());
            log.info("Sessions are: {}", acceptor.getManagedSessions());
            log.info("acceptor.getEndpoints: {}", acceptor.getEndpoints());
            log.info("executor.settings: {}", acceptor.getSettings());
            java.lang.Thread.sleep(5000);
        }
        //acceptor.stop();
    }

    private static InputStream getSettingsInputStream(String[] args) throws FileNotFoundException {
        InputStream inputStream = null;
        if (args.length == 0) {
            inputStream = Executor.class.getResourceAsStream("executor.cfg");
        } else if (args.length == 1) {
            inputStream = new FileInputStream(args[0]);
        }
        if (inputStream == null) {
            System.out.println("usage: " + Executor.class.getName() + " [configFile].");
            System.exit(1);
        }
        return inputStream;
    }
}
