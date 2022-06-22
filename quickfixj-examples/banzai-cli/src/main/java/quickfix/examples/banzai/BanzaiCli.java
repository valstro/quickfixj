/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix.examples.banzai;

import quickfix.examples.banzai.Order;
import quickfix.examples.banzai.OrderSide;
import quickfix.examples.banzai.OrderTIF;
import quickfix.examples.banzai.OrderTableModel;
import quickfix.examples.banzai.OrderType;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

//import javax.swing.JFrame;
//import javax.swing.UIManager;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.Initiator;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

//import quickfix.examples.banzai.ui.BanzaiFrame;

/**
 * Entry point for the Banzai application.
 */
public class BanzaiCli {
  private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

  private static final Logger log = LoggerFactory.getLogger(BanzaiCli.class);
  private static BanzaiCli banzai;
  private static BanzaiApplication application;
  private boolean initiatorStarted = false;
  private Initiator initiator = null;
  //private JFrame frame = null;

  public BanzaiCli(String[] args) throws Exception {
    InputStream inputStream = null;
    if (args.length == 0) {
      inputStream = BanzaiCli.class.getResourceAsStream("banzai.cfg");
    } else if (args.length == 1) {
      inputStream = new FileInputStream(args[0]);
    }
    if (inputStream == null) {
      System.out.println("usage: " + BanzaiCli.class.getName() + " [configFile].");
      return;
    }
    SessionSettings settings = new SessionSettings(inputStream);
    inputStream.close();

    boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true"));

    OrderTableModel orderTableModel = new OrderTableModel();
    ExecutionTableModel executionTableModel = new ExecutionTableModel();
    application = new BanzaiApplication(orderTableModel, executionTableModel);
    MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
    LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);
    MessageFactory messageFactory = new DefaultMessageFactory();

    initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory,
        messageFactory);

    JmxExporter exporter = new JmxExporter();
    exporter.register(initiator);

    //frame = new BanzaiFrame(orderTableModel, executionTableModel, application);
    //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  public synchronized void logon() {
    log.info("Logon - begin");
    if (!initiatorStarted) {
      try {
        initiator.start();
        initiatorStarted = true;
      } catch (Exception e) {
        log.error("Logon failed", e);
      }
    } else {
      for (SessionID sessionId : initiator.getSessions()) {
        log.info("Logging on session Id: " + sessionId);
        Session.lookupSession(sessionId).logon();
      }
    }
  }

  private void sendOrder(final BanzaiApplication application) {

    SessionID sessionId = new SessionID("FIX.4.4:BANZAI->EXEC");
    Order order = new Order();
    order.setSide(OrderSide.BUY);
    order.setType(OrderType.MARKET);
    order.setTIF(OrderTIF.DAY);

    order.setSymbol("ABC");
    order.setQuantity(123);
    order.setOpen(234);

    OrderType type = order.getType();
    if (type == OrderType.LIMIT || type == OrderType.STOP_LIMIT)
      order.setLimit(1.2);
    if (type == OrderType.STOP || type == OrderType.STOP_LIMIT)
      order.setStop(2.3);
    order.setSessionID(sessionId);

    //orderTableModel.addOrder(order);
    application.send(order);
  }

  public void logout() {
    for (SessionID sessionId : initiator.getSessions()) {
      Session.lookupSession(sessionId).logout("user requested");
    }
  }

  public void stop() {
    shutdownLatch.countDown();
  }

  /*
     public JFrame getFrame() {
     return frame;
     }
     */

  public static BanzaiCli get() {
    return banzai;
  }

  public static void main(String[] args) throws Exception {
    banzai = new BanzaiCli(args);
    //if (!System.getProperties().containsKey("openfix")) {
    banzai.logon();
    banzai.sendOrder(application);
    //}
    shutdownLatch.await();
  }

}
