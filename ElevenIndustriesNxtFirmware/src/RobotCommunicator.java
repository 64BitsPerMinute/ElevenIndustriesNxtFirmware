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

public class RobotCommunicator extends Object {

	private static boolean USBtest = false;

	private static RobotSystem robot;
	
	static DataOutputStream os; 
	
	private static int counter = 0;
	
	public static void main(String[] args) {
		System.out.println("Waiting...");

		// Establish the connection here, for testing purpose, we will use USB
		// connection
		//NXTConnection connection = null;
		BTConnection connection = null;
		//if (USBtest) {
		//	connection = USB.waitForConnection();
		//} else {
			connection = Bluetooth.waitForConnection();
		//}
			
		// An additional check before opening streams
		if (connection == null) {
			System.out.println("Failed");
		} else {
			System.out.println("Connected");
		}

		// Open two data input and output streams for read and write
		// respectively
		final DataOutputStream oHandle = connection.openDataOutputStream();
		os = new DataOutputStream(connection.openOutputStream());
		final DataInputStream iHandle = connection.openDataInputStream(); //connection.openDataInputStream();
		final DataInputStream is = new DataInputStream(connection.openInputStream());
		String input = "", output = "";

		
		// SensorPort.S4.addSensorPortListener(new SensorPortListener() { //
		// sensor 4 somehow doesnt get this event
		//
		// @Override
		// public void stateChanged(SensorPort arg0, int arg1, int arg2) {
		// if (sUltra.getDistance() < 30){
		// pilot.stop();
		// String str = "ALERT: obstacle ahead, stopping all actions";
		// try {
		// oHandle.write(str.getBytes());
		// oHandle.flush();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// pilot.stop();
		// }
		//
		// }
		//
		// });

		// setup threading for listener (Threading style)
		// (new Thread() {
		// public void run(){
		// while (true){
		// if (sTouch.isPressed()){
		// try {
		// oHandle.writeUTF("ALERT: bump into something, stopping all actions");
		// pilot.stop();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// }
		// }).start();

		do {
			try {
				byte[] buffer = new byte[256]; // allocate a buffer of max size
												// 256 bytes
				int count = is.read(buffer); // pass the buffer to the //iHandle
													// input handle to read
				if (count > 0) { // check if number of bytes read is more than
									// zero
					input = (new String(buffer)).trim(); // convert back to
															// string and trim
															// down the blank
															// space

					//if(verifyMessage(input)){
						parseMessage(input);
					//}
						
					//output = performAction(input); // perform arbitrary actions
					
					//String str = output + " OK";
					os.write(("Recieved message from station :"+input).getBytes()); // ACK
					os.flush(); // flush the output bytes
					if(counter > 200){
						counter = 0;
						
						sendDebugString("Sound:"+Integer.toString(robot.getSoundData()));
						sendDebugString("Ultra:"+Integer.toString(robot.getUltraSonicData()));
						sendDebugString("Light:"+Integer.toString(robot.getLightData()));
						sendDebugString("Battery:"+Integer.toString(robot.getBatteryStrength()));
						sendDebugString("Signal:"+Integer.toString(connection.getSignalStrength()));
						sendDebugString("MotorBSpeed:"+Integer.toString(robot.getMotorBSpeed()));
						sendDebugString("MotorCSpeed:"+Integer.toString(robot.getMotorCSpeed()));
						sendDebugString("MotorBAngle:"+Integer.toString(robot.getMotorBAngle()));
						sendDebugString("MotorCAngle:"+Integer.toString(robot.getMotorCAngle()));
						
						
					}
				}
				Thread.sleep(10);

			} catch (Exception e) {
				System.out.println(" write error " + e);
				System.exit(1);
			}
		} while (!input.equalsIgnoreCase("exit"));

		System.out.println("Ending session...");
		try {
			oHandle.close();
			iHandle.close();
			connection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

	public static void sendDebugString(String s){
		try {
			os.write(s.getBytes());
			os.flush();
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
		//String parameters = input.substring(9);
		switch(opcode){
			case 'A':
				//pilot.setRotateSpeed((float)parameters.getBytes(parameters.substring(4)));				
				System.out.print("TurnLeft Command Recieved");
				sendDebugString("TurnLeft Command Recieved");
				robot.turnLeft();
				break;
			case 'B':	
				sendDebugString("TurnRight Command Recieved");
				System.out.print("TurnRight Command Recieved");
				robot.turnRight();
				break;
			case 'C':
				sendDebugString("MoveForward Command Recieved");
				System.out.print("MoveForward CommandRecieved");
				robot.moveForward();
				break;
			case 'D':
				sendDebugString("MoveBackward Command Recieved");
				System.out.print("MoveBackward Command Recieved");
				robot.moveBackward();
				break;
			case 'F':
				sendDebugString("Stop Command Recieved");
				System.out.print("Stop Command Recieved");
				robot.stop();
				break;
			default :
				break;
		}
	}





}
