package org.grid.protocol;

// TODO: Auto-generated Javadoc
public class NewMessage {

	public String[] data;
	public static enum MessageType { REGISTER, ACKNOWLEDGE, INITIALIZE, TERMINATE, SCAN, STATE, MOVE, SEND, RECEIVE};
	
	/**
	 * Sets data in NewMessage.
	 *
	 * @param dataStream the string of data
	 */
	public NewMessage(String dataStream)
	{
		if(dataStream != null && dataStream.length() > 0)
		{
			this.data = dataStream.split(";");
		}
	}
	
	public MessageType getMessageType() throws Exception
	{
		if(data != null && data.length > 0)
		{
			MessageType type = MessageType.values()[Integer.parseInt(data[0])];
			return type;
		}
		throw new Exception("Data for message is not set.");
	}
	
	public String getTeam()
	{
		if(data != null && data.length == 3)
		{
			return data[1];
		}
		return null;
	}
	
	public String getPassphrase()
	{
		if(data != null && data.length == 3)
		{
			return data[2];
		}
		return null;
	}
	
	/**
	 * Encode message.
	 */
	public static void EncodeMessage()
	{
		
	}
}
