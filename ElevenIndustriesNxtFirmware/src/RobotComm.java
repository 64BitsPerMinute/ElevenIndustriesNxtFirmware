import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;

public class RobotComm extends Object {

	//Each of these are needed among multiple methods below (local variables don't make sense)
	private static final int minMessageLength = 9;
	private static final long timeOut = 10000;
	private static RobotSystem robot = new RobotSystem();
	private static DataOutputStream outputStream;
	private static DataInputStream inputStream;
	private static BTConnection connection = null;
	private static String headerString = "#";
	private static String messageSourceID = "R";
	private static String endString = "" + (char) 0;
	private static int messageNumber = 0;
	private static long lastMessageTime = 0;
	private static String input = "";

	public static void main(String[] args) {
		do {

			System.out.println("\nWaiting for Connection...");
			connection = Bluetooth.waitForConnection();
			if (connection == null) {
				System.out.println("\nFailed");
			} else {
				System.out.println("\nConnection Established!");
			}

			outputStream = new DataOutputStream(connection.openOutputStream());
			if(outputStream==null){
				continue;
			}
			inputStream = new DataInputStream(connection.openInputStream());
			if(inputStream==null){
				continue;
			}
			lastMessageTime = System.currentTimeMillis();
			robot.setHome();
			do {
				try {

					byte[] buffer = new byte[256];
					int count = inputStream.read(buffer);
					if (count > 0) {
						lastMessageTime = System.currentTimeMillis();
						input = (new String(buffer)).trim();
						//if (verifyMessage(input)) {
							parseMessage(input);
						//}
					}
					Thread.sleep(10);

				} catch (Exception e) {
					System.out.println(" write error " + e);
					System.exit(1);
				}
			} while ((System.currentTimeMillis()) - lastMessageTime < timeOut);
			System.out.println("\nSignal Lost...");
			robot.stop();
			try {
				outputStream.close();
				inputStream.close();
				connection.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} while (true);
	}

	public static boolean verifyMessage(String message) {
		if (message.length() < minMessageLength) {
			System.out.print("MessageTooShort");
			return false;
		}
		String content = message.substring(3, message.length() - 1);
		if (calculateChecksum(content).equals(message.substring(1, 3))) {
			return true;
		}
		System.out.print("Checksum verification failed");
		return false;
	}

	public static void sendString(String s) {
		try {
			outputStream.write(s.getBytes());
			outputStream.flush();
			messageNumber++;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void parseMessage(String input) {
		char opcode = input.charAt(8);
		String messageID = input.substring(3, 8);
		String status = "N";
		String breakpointTrigger = "1111";
		String breakpointSubstring = "";
		String speedParameters = "";
		switch (opcode) {
		case 'A':
			System.out.print("\nTurnLeft Command");
			sendCommandAcknowledgement(messageID);
			breakpointSubstring = input.substring(9,13);
			if (breakpointSubstring.equals(breakpointTrigger)) {
				robot.breakpoint();
			}
			robot.log('A',"");
			status = robot.turnLeft();
			sendExecutionResponse(messageID, status);
			break;
		case 'B':
			sendCommandAcknowledgement(messageID);
			System.out.print("\nTurnRight Command");
			breakpointSubstring = input.substring(9,13);
			if (breakpointSubstring.equals(breakpointTrigger)) {
				robot.breakpoint();
			}
			robot.log('B',"");
			status = robot.turnRight();
			sendExecutionResponse(messageID, status);
			break;
		case 'C':
			sendCommandAcknowledgement(messageID);
			System.out.print("\nMoveForward Command");
			breakpointSubstring = input.substring(9,13);
			if (breakpointSubstring.equals(breakpointTrigger)) {
				robot.breakpoint();
			}
			speedParameters = input.substring(13,21);
			robot.log('C',speedParameters);
			status = robot.moveForward(speedParameters);// get two speed parameters																
			sendExecutionResponse(messageID, status);
			break;
		case 'D':
			sendCommandAcknowledgement(messageID);
			System.out.print("\nMoveBackward Command");
			breakpointSubstring = input.substring(9,13);
			if (breakpointSubstring.equals(breakpointTrigger)) {
				robot.breakpoint();
			}
			speedParameters = input.substring(13,21);
			robot.log('D',speedParameters);			
			status = robot.moveBackward(speedParameters);
			sendExecutionResponse(messageID, status);
			break;
		case 'F':
			sendCommandAcknowledgement(messageID);
			System.out.print("\nStop Command");
			breakpointSubstring = input.substring(9,13);
			if (breakpointSubstring.equals(breakpointTrigger)) {
				robot.breakpoint();
			}
			robot.log('F',"");
			status = robot.stop();
			sendExecutionResponse(messageID, status);
			break;
		case 'S':
			System.out.print(" P");
			sendCommandAcknowledgement(messageID);
			sendSystemStatusData();
			break;
		case 'T':
			sendCommandAcknowledgement(messageID);
			System.out.print("\nGoing Home");
			status = robot.goHome();
			sendExecutionResponse(messageID, status);
			break;
		case 'Y':
			System.out.print(" Y");
			sendCommandAcknowledgement(messageID);
			robot.setHome();
			break;
		case 'Z':
			System.out.print(" Z");
			sendCommandAcknowledgement(messageID);
			sendWheelStatusData();
			break;
		default:
			break;
		}
	}

	private static void sendCommandAcknowledgement(String messageID) {
		String opcode = "N";
		String message = messageSourceID + format4ByteNumber(messageNumber)
				+ opcode + messageID;
		String checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}

	private static void sendExecutionResponse(String messageID, String status) {
		if(!status.equals("S")){
			RobotComm.sendErrorCode('k');
			return;
		}
		String opcode = "L";
		String message = messageSourceID + format4ByteNumber(messageNumber)
				+ opcode + messageID + status;
		String checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}

	private static void sendErrorCode(char errorCode) {
		String opcode = "M";
		String message = messageSourceID + format4ByteNumber(messageNumber)
				+ opcode + Character.toString(errorCode);
		String checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}

	public static void sendSystemStatusData() {
		String opcode = "K";
		String parameters = "";
		String message;
		String checksum;
		parameters = format4ByteNumber(robot.getUltraSonicData())
				+ format4ByteNumber(robot.getLightData())
				+ format4ByteNumber(robot.getSoundData())
				+ robot.getTouchData() + "0"
				+ format4ByteNumber(robot.getBatteryStrength())
				+ format4ByteNumber(connection.getSignalStrength())
				+ format4ByteNumber(robot.getLocation()[0])
				+ format4ByteNumber(robot.getLocation()[1]);
		message = messageSourceID + format4ByteNumber(messageNumber)
				+ opcode + parameters;
		checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}

	public static void sendWheelStatusData() {
		String opcode = "Z";
		String parameters = "";

		parameters = format4ByteNumber(robot.getMotorBAngle())
				+ format4ByteNumber(robot.getMotorBSpeed())
				+ format4ByteNumber(robot.getMotorCAngle())
				+ format4ByteNumber(robot.getMotorCSpeed());
		String message = messageSourceID + format4ByteNumber(messageNumber)
				+ opcode + parameters;
		String checksum = calculateChecksum(message);
		message = headerString + checksum + message + endString;
		sendString(message);
	}

	private static String format4ByteNumber(int number) {
		int[] digits = { 0, 0, 0, 0 };
		digits[3] = number % 10;
		digits[2] = (number % 100) / 10;
		digits[1] = (number % 1000) / 100;
		digits[0] = (number % 10000) / 1000;
		String returnString = "";
		returnString += Integer.toString(digits[0]) + Integer.toString(digits[1])
				+ Integer.toString(digits[2]) + Integer.toString(digits[3]);
		return returnString;
	}

	public static String calculateChecksum(String messageContent) {
		int checksum = 0;
		for (int i = 0; i < messageContent.length(); i++) {
			checksum += (int) messageContent.charAt(i);
		}
		return formatChecksum(checksum);
	}

	private static String formatChecksum(int checksum) {
		return "" + ((checksum % 100) / 10) + (checksum % 10);
	}

}
