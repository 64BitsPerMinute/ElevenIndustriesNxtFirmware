import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.SoundSensor;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.USB;
import lejos.robotics.Touch;
import lejos.robotics.navigation.DifferentialPilot;

public class RobotComm extends Object {

	private static RobotSystem robot = new RobotSystem();
	private static DataOutputStream os; 
	private static DataInputStream is;
	private static int counter = 0;
	private static BTConnection connection = null;
	private static String headerString = "#";
	private static String messageSourceID = "R";
	private static String endString = "" + (char)0;
	private static int messageNumber = 0;
	private static long lastMessageTime = 0;
	static String input = "";
	String output = "";
	
	public static void main(String[] args) {
		do{
		
			System.out.println("\nWaiting for Connection...");
	
			// Establish the connection here, for testing purpose, we will use USB
			// connection
			//NXTConnection connection = null;
			
			//if (USBtest) {
			//	connection = USB.waitForConnection();
			//} else {
				connection = Bluetooth.waitForConnection();
			//}
				
			// An additional check before opening streams
			if (connection == null) {
				System.out.println("\nFailed");
			} else {
				System.out.println("\nConnection Established!");
			}
			
			// Open two data input and output streams for read and write
			// respectively
			//final DataOutputStream oHandle = connection.openDataOutputStream();
			os = new DataOutputStream(connection.openOutputStream());
			//final DataInputStream iHandle = connection.openDataInputStream(); //connection.openDataInputStream();
			is = new DataInputStream(connection.openInputStream());
			
			
			lastMessageTime = System.currentTimeMillis();
				
			
					do {
						try {
							
							
							byte[] buffer = new byte[256]; // allocate a buffer of max size
															// 256 bytes
							int count = is.read(buffer); // pass the buffer to the //iHandle
																// input handle to read
							if (count > 0) { // check if number of bytes read is more than
												// zero
								lastMessageTime = System.currentTimeMillis();
								input = (new String(buffer)).trim(); // convert back to
																		// string and trim
																		// down the blank
																		// space
			
								//if(verifyMessage(input)){
									parseMessage(input);
								//}
									
								//output = performAction(input); // perform arbitrary actions
								
								//String str = output + " OK";
								
								//os.write(("Recieved message from station :"+input).getBytes()); // ACK
								//os.flush(); // flush the output bytes
								
							}
							Thread.sleep(10);
							
						} catch (Exception e) {
							System.out.println(" write error " + e);
							System.exit(1);
						}
					} while ((System.currentTimeMillis())-lastMessageTime<10000);//!input.equalsIgnoreCase("exit")
	
						System.out.println("\nSignal Lost...");
						robot.stop();
						try {
							//oHandle.close();
							//iHandle.close();
							os.close();
							is.close();
							connection.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		} while (true);
	}
	
	public static boolean verifyMessage(String message) {
		// Check if correct number of bytes
		/*
		if (message.getBytes().length != 59) {
			System.out.print("Message is the incorrect size");
			return false;
		}
		*/
		// Verify checksum
		byte[] receivedBytes = message.getBytes();
		byte[] checksumBytes = new byte[2];
		System.arraycopy(receivedBytes, 1, checksumBytes,
				0, 2);
		byte[] checksumContent = new byte[55];
		System.arraycopy(receivedBytes, 3, checksumContent,
				0, 55);
		short calculatedChecksum = 0;
		for (int i = 0; i < checksumContent.length; i++) {
			calculatedChecksum += checksumContent[i];
		}
		// See if checksum matches the calculated checksum
		// (algorithm courtesy of
		// http://fkooman.wordpress.com/2008/11/25/convert-short-to-byte-array-and-byte-array-to-short/)
		if ((short) (((checksumBytes[0] << 8)) | ((checksumBytes[1] & 0xff))) != calculatedChecksum) {
			System.out.print("Message failed Checksum");
			return false;
		}
		return true;
		
	}

	public static void sendString(String s){
		try {
			os.write(s.getBytes());
			os.flush();
			messageNumber++;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // ACK
		 // flush the output bytes
	}
	
	private static void parseMessage(String input) {
		byte[] messageBytes = input.getBytes();
		
		//String opcode = input.substring(8,8);
		char opcode = input.charAt(8);
		String messageID = input.substring(3,8);
		String status = "N";
		String messageSource = input.substring(3,4);
		switch(opcode){
			case 'A':
				//pilot.setRotateSpeed((float)parameters.getBytes(parameters.substring(4)));				
				System.out.print("\nTurnLeft Command");
				sendCommandAcknowledgement(messageID);
				if(messageSource.equals("D")){
					robot.breakpoint();
				}
				status = robot.turnLeft();
				sendExecutionResponse(messageID, status);
				break;
			case 'B':	
				sendCommandAcknowledgement(messageID);
				System.out.print("\nTurnRight Command");
				if(messageSource.equals("D")){
					robot.breakpoint();
				}
				status = robot.turnRight();
				sendExecutionResponse(messageID, status);
				break;
			case 'C':
				sendCommandAcknowledgement(messageID);
				System.out.print("\nMoveForward Command");
				if(messageSource.equals("D")){
					robot.breakpoint();
				}
				status = robot.moveForward();
				sendExecutionResponse(messageID, status);
				break;
			case 'D':
				sendCommandAcknowledgement(messageID);
				System.out.print("\nMoveBackward Command");
				if(messageSource.equals("D")){
					robot.breakpoint();
				}
				status = robot.moveBackward();
				sendExecutionResponse(messageID, status);
				break;
			case 'F':
				sendCommandAcknowledgement(messageID);
				System.out.print("\nStop Command");
				if(messageSource.equals("D")){
					robot.breakpoint();
				}
				status = robot.stop();
				sendExecutionResponse(messageID, status);
				break;
			case 'S':
				System.out.print(" P");
				sendCommandAcknowledgement(messageID);
				sendSystemStatusData();			
//				sendString("Sound:"+Integer.toString(robot.getSoundData()));
//				sendString("Ultra:"+Integer.toString(robot.getUltraSonicData()));
//				sendString("Light:"+Integer.toString(robot.getLightData()));
//				sendString("Battery:"+Integer.toString(robot.getBatteryStrength()));
//				sendString("Signal:"+Integer.toString(connection.getSignalStrength()));	
//				sendString("MotorBSpeed:"+Integer.toString(robot.getMotorBSpeed()));
//				sendString("MotorCSpeed:"+Integer.toString(robot.getMotorCSpeed()));
//				sendString("MotorBAngle:"+Integer.toString(robot.getMotorBAngle()));
//				sendString("MotorCAngle:"+Integer.toString(robot.getMotorCAngle()));
				break;
			default :
				break;
		}
	}

	private static void sendCommandAcknowledgement(String messageID) {
		String opcode = "N";
		String message = messageSourceID + format4ByteNumber(messageNumber)  + opcode + messageID;
		String checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}
	
	private static void sendExecutionResponse(String messageID, String status) {
		String opcode = "L";
		String message = messageSourceID + format4ByteNumber(messageNumber)  + opcode + messageID + status;
		String checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}
	
	public static void sendSystemStatusData(){
		String opcode = "K";
		String parameters = "";
		
		parameters = format4ByteNumber(robot.getUltraSonicData())+format4ByteNumber(robot.getLightData())+format4ByteNumber(robot.getSoundData())+robot.getTouchData()+"0"+format4ByteNumber(robot.getBatteryStrength())+format4ByteNumber(connection.getSignalStrength())+format4ByteNumber(robot.getLocation()[0])+format4ByteNumber(robot.getLocation()[1]);
		String message = messageSourceID + format4ByteNumber(messageNumber)  + opcode + parameters;
		String checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}


	private static String format4ByteNumber(int number) {
//		return "" + (char)((number/16777216)%256) + (char)((number/65536)%256) +
//				(char)((number/256)%256) + (char)((number)%256);
		int[] numbers = {0,0,0,0};
		numbers[3] = number%10;
		numbers[2] = (number%100)/10;
		numbers[1] = (number%1000)/100;
		numbers[0] = (number%10000)/1000;
		
		
		
		String ret = "";
		ret += Integer.toString(numbers[0]) + Integer.toString(numbers[1]) + Integer.toString(numbers[2]) + Integer.toString(numbers[3]);
		return ret;
	}
	public static String calculateChecksum(String messageContent) {
		int checksum = 0;
		for(int i = 0; i < messageContent.length(); i++) {
			checksum += (int)messageContent.charAt(i);
		}
		return formatChecksum(checksum);
	}
	private static String formatChecksum(int checksum) {
		//return "" + (char)((checksum/256)%256) + (char)(checksum %256);
		return "" + ((checksum%100)/10) + (checksum%10);
		
	}



}
