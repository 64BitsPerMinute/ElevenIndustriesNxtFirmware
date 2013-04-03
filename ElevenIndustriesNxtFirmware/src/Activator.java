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
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.USB;
import lejos.robotics.Touch;
import lejos.robotics.navigation.DifferentialPilot;

public class Activator extends Object {

	private static boolean USBtest = false;

	// Initialize all sensors here
	private static Touch sTouch = new TouchSensor(SensorPort.S1);
	private static SoundSensor sSound = new SoundSensor(SensorPort.S2);
	private static LightSensor sLight = new LightSensor(SensorPort.S3);
	private static UltrasonicSensor sUltra = new UltrasonicSensor(SensorPort.S4);

	// DifferentialPilot is our motion controller, we will exclusively use the
	// object to navigate the robot around
	// the first two arguments are wheel diameters and track width (in cm)
	// respectively
	// last two arguments are left and right motors respectively
	private static DifferentialPilot pilot = new DifferentialPilot(2.25f, 5.5f,
			Motor.B, Motor.C);

	static DataOutputStream os; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Waiting...");

		// Establish the connection here, for testing purpose, we will use USB
		// connection
		NXTConnection connection = null;
		if (USBtest) {
			connection = USB.waitForConnection();
		} else {
			connection = Bluetooth.waitForConnection();
		}
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

		// Register a listener to port S1 which is the Touch sensor at the back
		SensorPort.S1.addSensorPortListener(new SensorPortListener() { // Listener's
																		// style

					@Override
					public void stateChanged(SensorPort arg0, int arg1, int arg2) {
						try {
							if (sTouch.isPressed()) {
								String str = "ALERT: bump into something, stopping all actions";
								os.write(str.getBytes());
								os.flush();
								System.out.print("Stouch1 pressed");
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						//pilot.stop();
					}

				});

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

	private static void sendDebugString(String s){
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
				pilot.rotate(-90);
				sendDebugString("TurnLeft Command Executed");
				break;
			case 'B':	
				sendDebugString("TurnRight Command Recieved");
				System.out.print("TurnRight Command Recieved");
				pilot.rotate(90);
				sendDebugString("TurnRight Command Executed");
				break;
			case 'C':
				sendDebugString("MoveForward Command Recieved");
				System.out.print("MoveForward CommandRecieved");
				pilot.forward();
				sendDebugString("MoveForward Command Executed");
				break;
			case 'D':
				sendDebugString("MoveBackward Command Recieved");
				System.out.print("MoveBackward Command Recieved");
				pilot.backward();
				sendDebugString("MoveBackward Command Executed");
				break;
			case 'F':
				sendDebugString("Stop Command Recieved");
				System.out.print("Stop Command Recieved");
				pilot.stop();
				sendDebugString("Stop Command Executed");
				break;
				
			default :
				break;
		}
	}

	/*
	 * Perform different actions based on the command
	 */
	private static String performAction(String cmd) {
		System.out.println("PC: " + cmd);
		String output = cmd;
		if (cmd.equalsIgnoreCase("forward")) {
			pilot.forward();
			output = "Traveling at: " + pilot.getTravelSpeed();
		} else if (cmd.equalsIgnoreCase("stop")) {
			pilot.stop();
			output = "Distance traveled: "
					+ pilot.getMovement().getDistanceTraveled();
		} else if (cmd.equalsIgnoreCase("status")) { // String.format does not
														// work
			output = "\nTouch sensor: "
					+ ((sTouch.isPressed()) ? "Pressed" : "Not Pressed");
			output += "\nSound sensor: " + sSound.readValue();
			output += "\nLight sensor: " + sLight.getLightValue();
			output += "\nUltra sensor: " + sUltra.getDistance();
			output += "\n";
		} else if (cmd.equalsIgnoreCase("turnRight")) {
			pilot.rotate(90);
		} else if (cmd.equalsIgnoreCase("turnLeft")) {
			pilot.rotate(-90);
		} else if (cmd.equalsIgnoreCase("turn180")) {
			pilot.rotate(180);
		}

		return output;

	}



}
