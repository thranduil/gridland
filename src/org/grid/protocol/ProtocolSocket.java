/*
 *  AgentField - a simple capture-the-flag simulation for distributed intelligence
 *  Copyright (C) 2011 Luka Cehovin <http://vicos.fri.uni-lj.si/lukacu>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>. 
 */
package org.grid.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

// TODO: Auto-generated Javadoc
public class ProtocolSocket {

	public static class AppendableDataOutputStream extends DataOutputStream {

		  /**
  		 * Instantiates a new appendable data output stream.
  		 *
  		 * @param out the out
  		 * @throws IOException Signals that an I/O exception has occurred.
  		 */
  		public AppendableDataOutputStream(OutputStream out) throws IOException {
		    super(out);
		  }
	}
	
	private DataInputStream in;
	
	private DataOutputStream out;
	
	private Thread inputThread;
	
	private Thread outputThread;
	
	private boolean running = true;
	
	private boolean debug = Boolean.getBoolean("fri.pipt.protocol.debug");
	
	private ConcurrentLinkedQueue<String> inQueue = new ConcurrentLinkedQueue<String>();
	
	private ConcurrentLinkedQueue<String> outQueue = new ConcurrentLinkedQueue<String>();
	
	private Socket socket;
	
	/**
	 * Instantiates a new protocol socket.
	 *
	 * @param sck the socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public ProtocolSocket(Socket sck) throws IOException {

		this.socket = sck;

		inputThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				try {
					in = new DataInputStream(socket.getInputStream());
				} catch (IOException e1) {
					return;
				}
				
				while (running) {

					String message = "";
					try {
						message = in.readUTF();
					} catch (Exception e) {
						e.printStackTrace();
					} 
					

					if (message == null || message.length() == 0)
						continue;
				
					
					if (debug)
					{
						System.err.println("*** MESSAGE INCOMING <<< " + message + " <<<");
					}
					
					handleMessage(message);
				}
			}
			
		});
		inputThread.start();
		
		outputThread = new Thread(new Runnable() {

			//Sending messages
			@Override
			public void run() {
				

				try {
					out = new DataOutputStream(socket.getOutputStream());
				} catch (IOException e1) {
					return;
				}

				
				while (running) {

					try {
						synchronized (outQueue) {
							while (outQueue.isEmpty()) {
								try {
									outQueue.wait();
								} catch (InterruptedException e) {}
							}
						}

						String message = outQueue.poll();
						
						if (debug)
							System.err.println("*** PROTOCOL OUTGOING >>> " + message + " >>>");
						
						out.writeUTF(message);

						out.flush();
						
					} catch (IOException e) {
						if (debug)
							e.printStackTrace();
						close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 

				}
			}
			
		});
		outputThread.start();
	}
	
	/**
	 * Receive message.
	 *
	 * @return the message
	 */
	public String receiveMessage() {
		
		synchronized (inQueue) {
		
			if (inQueue.isEmpty())
				return null;
		
			return inQueue.poll();
			
		}
		
	}
	
	/**
	 * Wait message.
	 *
	 * @return the message
	 */
	public String waitMessage() {
		
		synchronized (inQueue) {
			while (true) {
			
				if (inQueue.isEmpty())
					try {
						inQueue.wait();
					} catch (InterruptedException e) {}
				else break;
			}
			return inQueue.poll();
		}
		
	}
	
	/**
	 * Send message.
	 *
	 * @param msg the msg
	 */
	public void sendMessage(String msg) {
		
		if (msg == null || msg.length() == 0)
			return;
			
		synchronized (outQueue) {
		
			outQueue.add(msg);
		
			outQueue.notifyAll();
			
		}
		
	}
	
	/**
	 * Close.
	 */
	public void close() {
		
		outQueue.clear();
		
		running = false;
		
		onTerminate();
		
		try {
			in.close();
		} catch (IOException e) {
		} catch (NullPointerException e) {
		}
		
		try {
			out.close();
		} catch (IOException e) {
		} catch (NullPointerException e) {
		}
	}
	
	/**
	 * Handle message.
	 *
	 * @param message the message string
	 */
	protected void handleMessage(String message) {
		
		synchronized (inQueue) {
			inQueue.add(message);
			inQueue.notifyAll();
			
		}
		
	}
	
	/**
	 * On terminate.
	 */
	protected void onTerminate() {
		
	}
	
	public InetAddress getRemoteAddress() {
		return socket.getInetAddress();
	}
	
	public int getRemotePort() {
		return socket.getPort();
	}

}
