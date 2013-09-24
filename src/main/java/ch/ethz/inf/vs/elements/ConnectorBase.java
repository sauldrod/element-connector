package ch.ethz.inf.vs.elements;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * ConnectorBase is a partial implementation of a {@link Connector}. It connects
 * a server to a network interface and a port. ConnectorBase contains two
 * separate threads for sending and receiving. The receiver thread constantly
 * calls {@link #receiveNext()} which is supposed to listen on a socket until a
 * datagram arrives and forward it to the {@link RawDataChannel2}. The sender
 * thread constantly calls {@link #sendNext() which is supposed to wait on the
 * outgoing queue for a {@link RawData} message to send. Both
 * {@link #sendNext()} and {@link #receiveNext()} are expected to be blocking.
 */
public abstract class ConnectorBase implements Connector {
	
	/** The local address. */
	private final EndpointAddress localAddr;
	
	private Thread receiverThread;
	private Thread senderThread;

	/** The queue of outgoing block (for sending). */
	private final BlockingQueue<RawData> outgoing; // Messages to send
	
	/** The receiver of incoming messages */
	private RawDataChannel receiver; // Receiver of messages
	
	/** Indicates whether the connector has started and not stopped yet */
	private boolean running;
	
	/**
	 * Instantiates a new connector base.
	 *
	 * @param address the address to listen to
	 */
	public ConnectorBase(EndpointAddress address) {
		if (address == null)
			throw new NullPointerException();
		this.localAddr = address;

		// TODO: optionally define maximal capacity
		this.outgoing = new LinkedBlockingQueue<RawData>();
	}
	
	/**
	 * Gets the name of the connector, e.g. the transport protocol used such as UDP or DTlS.
	 *
	 * @return the name
	 */
	public abstract String getName();
	
	/**
	 * Blocking method. Waits until a message comes from the network. New
	 * messages should be wrapped into a {@link RawData} object and
	 * {@link #forwardIncoming(RawData)} should be called to forward it to the
	 * {@link RawDataChannel2}. // TODO: changes
	 * 
	 * @throws Exception
	 *             any exceptions that should be properly logged
	 */
	protected abstract RawData receiveNext() throws Exception;
	
	/**
	 * Blocking method. Waits until a new message should be sent over the
	 * network. //TODO: changed
	 * 
	 * @throws Exception any exception that should be properly logged
	 */
	protected abstract void sendNext(RawData raw) throws Exception;
	
	protected int getReceiverThreadCount() {
		return 1;
	}
	
	protected int getSenderThreadCount() {
		return 1;
	}
	
	private void receiveNextMessageFromNetwork() throws Exception {
		RawData raw = receiveNext();
		if (raw != null)
			receiver.receiveData(raw);
	}
	
	private void sendNextMessageOverNetwork() throws Exception {
		RawData raw = outgoing.take(); // Blocking
		if (raw == null)
			throw new NullPointerException();
		sendNext(raw);
	}
	
	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#start()
	 */
	@Override
	public synchronized void start() throws IOException {
		if (running) return;
		running = true;

		//int senderCount = getSenderThreadCount();
		//int receiverCount = getReceiverThreadCount();
		//LOGGER.fine(getName()+"-connector starts "+senderCount+" sender threads and "+receiverCount+" receiver threads");
		
		senderThread = new Worker(getName()+"-Sender"+localAddr) {
				public void prepare() { prepareSending(); }
				public void work() throws Exception { sendNextMessageOverNetwork(); }
			};

		receiverThread = new Worker(getName()+"-Receiver"+localAddr) {
				public void prepare() { prepareReceiving(); }
				public void work() throws Exception { receiveNextMessageFromNetwork(); }
			};
		
		receiverThread.start();
		senderThread.start();
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#stop()
	 */
	@Override
	public synchronized void stop() {
		if (!running) return;
		running = false;
		senderThread.interrupt();
		receiverThread.interrupt();
		outgoing.clear();
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#destroy()
	 */
	@Override // TODO: Note that this does not call stop but the subclass has to do
	public synchronized void destroy() { }

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#send(ch.inf.vs.californium.network.RawData)
	 */
	@Override
	public void send(RawData msg) {
		if (msg == null)
			throw new NullPointerException();
		outgoing.add(msg);
		// TODO is it better not to switch the thread?
		// Careful: Buffer might not be used thread-safe
//		try {
//			sendNext(msg);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	/* (non-Javadoc)
	 * @see ch.inf.vs.californium.network.connector.Connector#setRawDataReceiver(ch.inf.vs.californium.network.RawDataChannel)
	 */
	@Override
	public void setRawDataReceiver(RawDataChannel receiver) {
		this.receiver = receiver;
	}
	
	public void prepareSending() {}
	public void prepareReceiving() {}
	
	/**
	 * Abstract worker thread that wraps calls to
	 * {@link ConnectorBase#getNextOutgoing()} and
	 * {@link ConnectorBase#receiveNext()}. Therefore, exceptions do not crash
	 * the threads and will be properly logged.
	 */
	private abstract class Worker extends Thread {

		/**
		 * Instantiates a new worker.
		 *
		 * @param name the name, e.g., of the transport protocol
		 */
		private Worker(String name) {
			super(name);
			setDaemon(false);
//			setDaemon(true); // TODO: smart?
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				//LOGGER.info("Start "+getName()+", (running = "+running+")");
				prepare();
				while (running) {
					try {
						work();
					} catch (Throwable t) {
						//if (running)
							//LOGGER.log(Level.WARNING, "Exception \""+t+"\" in thread " + getName()+": running="+running, t);
						//else
							//LOGGER.info("Exception \""+t+"\" in thread " + getName()+" has successfully stopped socket thread");
					}
				}
			} finally {
				//LOGGER.info(getName()+" has terminated (running = "+running+")");
			}
		}

		/**
		 * Override this method and call {@link ConnectorBase#receiveNext()} or
		 * {@link ConnectorBase#sendNext()}.
		 * 
		 * @throws Exception the exception to be properly logged
		 */
		protected abstract void work() throws Exception;
		protected abstract void prepare();
	}

	/**
	 * Gets the local address this connector is listening to.
	 *
	 * @return the local address
	 */
	public EndpointAddress getLocalAddr() {
		return localAddr;
	}
	
	/**
	 * Gets the receiver.
	 *
	 * @return the receiver
	 */
	public RawDataChannel getReceiver() {
		return receiver;
	}
	
	/**
	 * Sets the receiver for incoming messages.
	 *
	 * @param receiver the new receiver
	 */
	public void setReceiver(RawDataChannel receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * Checks the connector has started but not stopped yet.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return running;
	}
	
}
