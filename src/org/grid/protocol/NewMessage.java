package org.grid.protocol;

import java.util.ArrayList;

// TODO: Auto-generated Javadoc
public class NewMessage {

	protected ArrayList<String> data;
	public static enum MessageType { REGISTER, ACKNOWLEDGE, INITIALIZE, TERMINATE, SCAN, STATE, MOVE, SEND, RECEIVE};
	public static enum Direction {NONE, UP, DOWN, LEFT, RIGHT};
	
	public NewMessage(MessageType type)
	{
		data = new ArrayList<String>();
		data.add(Integer.toString(type.ordinal()));
	}
	
	public NewMessage(ArrayList<String> message)
	{
		data = message;
	}
	
	public MessageType getMessageType() throws Exception
	{
		if(data != null && data.size() > 0)
		{
			MessageType type = MessageType.values()[Integer.parseInt(data.get(0))];
			return type;
		}
		throw new Exception("Data for message is not set.");
	}
	
	public static class RegisterMessage extends NewMessage
	{
		public RegisterMessage(String team, String passphrase) {
			super(MessageType.REGISTER);
			data.add(team);
			data.add(passphrase);
		}
		
		public String getTeam()
		{
			if(data != null && data.size() == 3)
			{
				return data.get(1);
			}
			return null;
		}
		
		public String getPassphrase()
		{
			if(data != null && data.size() == 3)
			{
				return data.get(2);
			}
			return null;
		}
	}
	
	public static class AcknowledgeMessage extends NewMessage
	{
		public AcknowledgeMessage() {
			super(MessageType.ACKNOWLEDGE);
		}
	}
	
	public static class InitializeMessage extends NewMessage
	{
		public InitializeMessage(int id, int maxMessageSize, int gameSpeed) {
			super(MessageType.INITIALIZE);
			data.add(Integer.toString(id));
			data.add(Integer.toString(maxMessageSize));
			data.add(Integer.toString(gameSpeed));
		}
		
		public int getId()
		{
			if(data != null && data.size() == 4)
			{
				return Integer.parseInt(data.get(1));
			}
			return -1;
		}
		
		public int getMaxMessageSize()
		{
			if(data != null && data.size() == 4)
			{
				return Integer.parseInt(data.get(2));
			}
			return -1;
		}
		
		public int getGameSpeed()
		{
			if(data != null && data.size() == 4)
			{
				return Integer.parseInt(data.get(3));
			}
			return -1;
		}
	}
	
	public static class TerminateMessage extends NewMessage
	{
		public TerminateMessage() {
			super(MessageType.TERMINATE);
		}
	}

	public static class ScanMessage extends NewMessage
	{
		public ScanMessage(int stamp) {
			super(MessageType.SCAN);
			data.add(Integer.toString(stamp));
		}
		
		public int getStamp()
		{
			if(data != null && data.size() == 2)
			{
				return Integer.parseInt(data.get(1));
			}
			return -1;
		}
	}
	
	public static class StateMessage extends NewMessage
	{
		public StateMessage(Direction direction, Neighborhood neighborhood, boolean hasFlag) {
			super(MessageType.STATE);
			data.add(Integer.toString(direction.ordinal()));
			data.add(Integer.toString((neighborhood.getSize())));
			data.add(neighborhood.getRawGrid());
			data.add(hasFlag ? "1" : "0");
		}
		
		public Direction getDirection() throws Exception
		{
			if(data != null && data.size() == 5)
			{
				Direction dir = Direction.values()[Integer.parseInt(data.get(1))];
				return dir;
			}
			throw new Exception("Invalid StateMessage!");
		}
		
		public int getNeighborhoodSize()
		{
			if(data != null && data.size() == 5)
			{
				return Integer.parseInt(data.get(2));
			}
			else
			{
				return -1;
			}
		}
		
		public int[] getNeighborhood()
		{	
			if(data != null && data.size() == 5)
			{
				int size = Integer.parseInt(data.get(2));
				int[] result = new int[(size * 2 + 1) * (size * 2 + 1)];
				String[] nArray = data.get(3).split(", ");
				if(result.length == nArray.length)
				{
					for(int i = 0 ; i < result.length; i++)
					{
						result[0] = Integer.parseInt(nArray[i]);
					}
				}
				else
				{
					System.out.println("getNeighborhood() - Neighborhood is not the right size");
				}
				return result;
			}
			else
			{
				return null;
			}
		}
		
		public boolean getHasFlag()
		{
			if(data != null && data.size() == 5)
			{
				if(data.get(4) == "0")
					return false;
				else
					return true;
			}
			else
			{
				System.out.println("getHasFlag() - Data is null or not correnct size");
				return false;
			}
		}
	}
	
	public static class MoveMessage extends NewMessage
	{
		public MoveMessage(Direction direction) {
			super(MessageType.MOVE);
			data.add(Integer.toString(direction.ordinal()));
		}
		
		public Direction getDirection() throws Exception
		{
			if(data != null && data.size() == 2)
			{
				Direction dir = Direction.values()[Integer.parseInt(data.get(1))];
				return dir;
			}
			throw new Exception("Invalid MoveMessage");
		}
	}
	
	public static class SendMessage extends NewMessage
	{
		public SendMessage(int to, byte[] message) {
			super(MessageType.SEND);
			data.add(Integer.toString(to));
			data.add(new String(message));
		}
		
		public int getTo()
		{
			if(data != null && data.size() == 3)
			{
				return Integer.parseInt(data.get(1));
			}
			return -1;
		}
		
		public byte[] getMessage()
		{
			if(data != null && data.size() == 3)
			{
				return data.get(2).getBytes();
			}
			return new byte[]{};
		}
	}
	
	public static class ReceiveMessage extends NewMessage
	{
		public ReceiveMessage(int from, byte[] message) {
			super(MessageType.RECEIVE);
			data.add(Integer.toString(from));
			data.add(new String(message));
		}
		
		public int getFrom()
		{
			if(data != null && data.size() == 3)
			{
				return Integer.parseInt(data.get(1));
			}
			return -1;
		}
		
		public byte[] getMessage()
		{
			if(data != null && data.size() == 3)
			{
				return data.get(2).getBytes();
			}
			return new byte[]{};
		}
	}
}
