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
package org.grid.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Vector;

import org.grid.protocol.Neighborhood;
import org.grid.protocol.NewMessage;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.NewMessage.RegisterMessage;
import org.grid.protocol.ProtocolSocket;


// TODO: Auto-generated Javadoc
public class Dispatcher implements Runnable {

	public static enum Status {UNKNOWN, REGISTERED, USED}
	
	public class Client extends ProtocolSocket {

		private Status status = Status.UNKNOWN;
		
		private Team team;
		
		private Agent agent = null;
		
		private int totalMessages = 0, scanMessages = 0, msgMessages = 0;
		
		/**
		 * Instantiates a new client.
		 *
		 * @param socket the socket
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public Client(Socket socket)
				throws IOException {
			super(socket);
			listeners = new Vector<ClientListener>();
		}
		
		/* (non-Javadoc)
		 * @see org.grid.protocol.ProtocolSocket#handleMessage(org.grid.protocol.Message)
		 */
		protected void handleMessage(String msg) {
			
			NewMessage message = new NewMessage(msg);
			
			synchronized (this) {
				totalMessages++;
			}
			
			if (status == null)
				status = Status.UNKNOWN;
			
			switch (status) {
			case UNKNOWN: {
				
				if (message.getMessageType() == NewMessage.MessageType.REGISTER) {
					
					NewMessage.RegisterMessage xMessage = new NewMessage.RegisterMessage(message);
					team = game.getTeam(xMessage.getTeam());
	
					if (team == null) {
						
						Main.log("Unknown team: " + xMessage.getTeam());
						close();
						return;
					}
	
					if (team.getPassphrase() != null) {
					
						String passphrase = xMessage.getPassphrase();
						
						if (passphrase == null) passphrase = "";
						
						if (!passphrase.equals(team.getPassphrase())) {
							Main.log("Rejected client %s for team %s: invalid passphrase", this, xMessage.getTeam());
							close();
							return;
						}
						
					}
					
					Main.log("New client joined team " + team + ": " + this);
					
					team.addClient(this);
					
					status = Status.REGISTERED;
					
					sendMessage(new NewMessage.AcknowledgeMessage().encodeMessage());
				
				}
				
				break;
			}
			case REGISTERED: {
				
				if (agent != null && (message.getMessageType() == NewMessage.MessageType.ACKNOWLEDGE)) {
					
					status = Status.USED;
					
				}
				
				break;
			}
			case USED: {
				
				if (agent == null)
					return;
				
				if (message.getMessageType() == NewMessage.MessageType.SCAN) {
					
					scanMessages++;
					
					Neighborhood n = game.scanNeighborhood(neighborhoodSize, getAgent());
					while(n == null)
					{
						System.err.println("Scanning neighborhood for agent " + getAgent().getId() + " was unsuccessful. Retrying..");
						n = game.scanNeighborhood(neighborhoodSize, getAgent());
					}
					
					sendMessage(new NewMessage.StateMessage(getAgent().getDirection(), n, agent.hasFlag()).encodeMessage());
					
					return;
				}
				
				if (message.getMessageType() == NewMessage.MessageType.SEND) {
					
					msgMessages++;
					
					NewMessage.SendMessage xMessage = new NewMessage.SendMessage(message);
					
					int to = xMessage.getTo();
					
					if (xMessage.getMessage() == null || xMessage.getMessage().length > maxMessageSize) {
						Main.log("Message from %d to %d rejected: too long", agent.getId(), to);
						return;
					}
					
					game.message(team, agent.getId(), to, xMessage.getMessage());						
					
					return;
				}				

				if (message.getMessageType() == NewMessage.MessageType.MOVE) {
						
					NewMessage.MoveMessage xMessage = new NewMessage.MoveMessage(message);
					
					game.move(team, agent.getId(), xMessage.getDirection());
					
					//wait with sending state until agent is moving
					while(getAgent() != null 
							&& getAgent().isAlive() 
							&& getAgent().getDirection() != Direction.NONE)
					{
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					//after moving agent, if it is still alive, reply with current state					
					if(getAgent() != null && getAgent().isAlive())
					{
						Neighborhood n = game.scanNeighborhood(neighborhoodSize, getAgent());
						sendMessage(new NewMessage.StateMessage(getAgent().getDirection(), n, agent.hasFlag()).encodeMessage());
					}
					
					return;
				}	
				
			}
			}

			
		}

		public Agent getAgent() {
			return agent;
		}

		public Team getTeam() {
			return team;
		}
		
		public void setAgent(Agent agent) {
		
			if (this.agent != null) {
				status = Status.REGISTERED;
				sendMessage(new NewMessage.TerminateMessage().encodeMessage());
			}
			
			this.agent = agent;
			
			agent(agent);
			
			if (agent == null)
				return;
			
			sendMessage(new NewMessage.InitializeMessage(agent.getId(), maxMessageSize, game.getSpeed()).encodeMessage());
			
		}

		/* (non-Javadoc)
		 * @see org.grid.protocol.ProtocolSocket#onTerminate()
		 */
		@Override
		protected void onTerminate() {
			
			if (team != null)
				team.removeClient(this);
			
			synchronized (clients) {
				clients.remove(this);
			}
			
			
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			
			return getRemoteAddress() + ":" + getRemotePort(); 
			
		}
		
		/**
		 * Query message counter.
		 *
		 * @return the int
		 */
		public int queryMessageCounter() {
			synchronized (this) {
				int tmp = totalMessages;
				totalMessages = 0;
				msgMessages = 0;
				scanMessages = 0;
				return tmp;
			}
		}

		private Vector<ClientListener> listeners = new Vector<ClientListener>();

		/**
		 * Adds the listener.
		 *
		 * @param listener the listener
		 */
		public void addListener(ClientListener listener) {
			if (listeners == null)
				listeners = new Vector<ClientListener>();
			
			synchronized (listeners) {
				listeners.add(listener);
			}
			
		}
		
		/**
		 * Removes the listener.
		 *
		 * @param listener the listener
		 */
		public void removeListener(ClientListener listener) {
			synchronized (listeners) {
				listeners.remove(listener);
			}
		}
		
		/**
		 * Agent.
		 *
		 * @param agent the agent
		 */
		private void agent(Agent agent) {
			
			synchronized (listeners) {
				for (ClientListener l : listeners) {
					try {
						l.agent(this, agent);
					} catch (Exception e) {e.printStackTrace();}
					
				}
			}
		}
		
		/**
		 * Transfer.
		 *
		 * @param messages the messages
		 */
		private void transfer(int messages) {
			
			synchronized (listeners) {
				for (ClientListener l : listeners) {
					try {
						l.transfer(this, messages);
					} catch (Exception e) {e.printStackTrace();}
					
				}
			}
		}
		
		/**
		 * Traffic.
		 */
		public void traffic() {
			
			synchronized (this) {
				transfer(totalMessages);
				
				totalMessages = 0;				
			}

		}
		
		/**
		 * Send message.
		 *
		 * @param from the sender
		 * @param message the message
		 */
		public void send(int from, byte[] message) {
			
			if (status != Status.USED) return;
			
			sendMessage(new NewMessage.ReceiveMessage(from, message).encodeMessage());
			
		}
		
	}
	
	private HashSet<Client> clients = new HashSet<Client>();
	
	private ServerSocket socket;
	
	private Game game;
	
	private int maxMessageSize = 1024;
	
	private int neighborhoodSize = 5;	
	
	/**
	 * Instantiates a new dispatcher.
	 *
	 * @param port the port
	 * @param game the game
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public Dispatcher(int port, Game game) throws IOException {
		
		socket = new ServerSocket(port);
		
		this.game = game;
		
		this.maxMessageSize = game.getProperty("message.size", 256);

		this.neighborhoodSize = game.getNeighborhoodSize();
		
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * 
	 * 
	 * 
	 */
	@Override
	public void run() {
		
		Thread traffic = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				while (true) {
					synchronized (clients) {
					
						for (Client cl : clients) {
							cl.traffic();
						}
						
					}
				
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {

					}
				
				}
			}
		});
		traffic.setDaemon(true);
		traffic.start();
		
		
		while (true) {
			try {
				Socket sck = socket.accept();
				sck.setTcpNoDelay(true);
				synchronized (clients) {
					clients.add(new Client(sck));
				}
				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		
		}
	}
	
}
