import java.io.IOException;

import lejos.nxt.Battery;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.SoundSensor;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.robotics.Touch;
import lejos.robotics.navigation.DifferentialPilot;

public class RobotSystem {

	// Initialize all sensors here
	private static Touch sTouch = new TouchSensor(SensorPort.S1);
	private static SoundSensor sSound = new SoundSensor(SensorPort.S2);
	private static LightSensor sLight = new LightSensor(SensorPort.S3);
	private static UltrasonicSensor sUltra = new UltrasonicSensor(SensorPort.S4);

	static double xPosition = 0;
	static double yPosition = 0;

	// DifferentialPilot is our motion controller, we will exclusively use the
	// object to navigate the robot around
	// the first two arguments are wheel diameters and track width (in cm)
	// respectively
	// last two arguments are left and right motors respectively

	// Instantiate Pilot
	private DifferentialPilot pilot = new DifferentialPilot(2.25f, 5.5f,
			Motor.B, Motor.C);

	public void RobotSystem() {
		// Register a listener to port S1 which is the Touch sensor at the back
		SensorPort.S1.addSensorPortListener(new SensorPortListener() { // Listener's
																		// style
					@Override
					public void stateChanged(SensorPort arg0, int arg1, int arg2) {
						if (sTouch.isPressed()) {
							RobotCommunicator.sendDebugString("true");
							System.out.print("sTouch:true");
						}
					}

				});
		
	}
	
	public int getSoundData(){
		return sSound.readValue();
	}
	
	public int getLightData(){
		return sLight.getLightValue();
	}
	
	public int getUltraSonicData(){
		return sUltra.getDistance();
	}
	
	public int getBatteryStrength(){
		return Battery.getVoltageMilliVolt();
	}
	
	public int getMotorBSpeed(){
		return  Motor.B.getSpeed();
	}
	
	public int getMotorCSpeed(){
		return  Motor.C.getSpeed();
	}
	
	public int getMotorBAngle(){
		return Motor.B.getLimitAngle();
	}
	
	public int getMotorCAngle(){
		return Motor.C.getLimitAngle();
	}

	public void turnLeft() {
		pilot.rotate(-90);
		RobotCommunicator.sendDebugString("TurnLeft Command Executed");
	}

	public void turnRight() {
		pilot.rotate(90);
		RobotCommunicator.sendDebugString("TurnRight Command Executed");
	}

	public void moveForward() {
		pilot.forward();
		RobotCommunicator.sendDebugString("MoveForward Command Executed");
	}

	public void moveBackward() {
		pilot.backward();
		RobotCommunicator.sendDebugString("MoveBackward Command Executed");
	}

	public void stop() {
		pilot.stop();
		RobotCommunicator.sendDebugString("Stop Command Executed");
	}
	
	private static double[] getLocation() {
			double[] ret = {xPosition, yPosition};
		return ret;
	}

}
