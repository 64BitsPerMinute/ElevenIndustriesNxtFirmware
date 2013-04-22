import java.util.Stack;

import lejos.nxt.Battery;
import lejos.nxt.Button;
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
	
	static int xPosition = 0;
	static int yPosition = 0;
	static long previousSoundTime = 0;
	
	Stack<Command> log = new Stack<Command>();
	
	// DifferentialPilot is our motion controller, we will exclusively use the
	// object to navigate the robot around
	// the first two arguments are wheel diameters and track width (in cm)
	// respectively
	// last two arguments are left and right motors respectively

	// Instantiate Pilot
	private DifferentialPilot pilot = new DifferentialPilot(2.25f, 5.5f,
			Motor.B, Motor.C);

	public RobotSystem() {
		pilot.setRotateSpeed(30);
		SensorPort.S1.addSensorPortListener(new SensorPortListener() { 
					@Override
					public void stateChanged(SensorPort arg0, int arg1, int arg2) {
						if (sTouch.isPressed()) {
							System.out.print("\nYOU POKED ME");
							pilot.backward();
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							pilot.stop();
						}
					}

				});
		
		SensorPort.S2.addSensorPortListener(new SensorPortListener() {
			
			@Override
			public void stateChanged(SensorPort arg0, int arg1, int arg2) {
				if(sSound.readValue()>85 && (System.currentTimeMillis()-previousSoundTime)>300){
					pilot.stop();
					System.out.print("\nLOUD SOUND BOOM!");
					previousSoundTime = System.currentTimeMillis();
				}
			}
		});
		
		
	}
	
	public void breakpoint(){
		this.stop();
		Button.waitForPress();
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
	
	public String getTouchData(){
		if(sTouch.isPressed()){
			return "1";
		} else {
			return "0";
		}
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

	//Returning status string. s =success, f = fail, n = null
	
	public String turnLeft() {
		pilot.rotateLeft();
		return "S";
	}

	public String turnRight() {
		pilot.rotateRight();
		return "S";
	}

	public String moveForward(String speeds) {
		int leftSpeed = Integer.parseInt(speeds.substring(0,4));
		Motor.B.setSpeed(leftSpeed);
		int rightSpeed = Integer.parseInt(speeds.substring(4,8));
		Motor.C.setSpeed(rightSpeed);
		pilot.forward();
		return "S";
	}

	public String moveBackward(String speeds) {
		int leftSpeed = Integer.parseInt(speeds.substring(0,4));
		Motor.B.setSpeed(leftSpeed);
		int rightSpeed = Integer.parseInt(speeds.substring(4,8));
		Motor.C.setSpeed(rightSpeed);
		pilot.backward();
		return "S";
	}

	public String stop() {
		pilot.stop();
		return "S";
	}
	
	public int[] getLocation() {
			int[] ret = {xPosition, yPosition};
		return ret;
	}

	public void setHome() {
		log.clear();
		this.log('F',"");
	}

	public String goHome() {
		//pilot.rotate(180);
		for(int i = 0;i<log.size()-1;i++){
			Command c = log.pop();
			invertCommand(c,c.getTime()-log.peek().getTime());
		}	
		//pilot.rotate(180);
		setHome();
		return "S";
	}

	private void invertCommand(Command c, long time) {
		switch(c.getOpcode()){
		case 'A':
			this.turnRight();
			break;
		case 'B':
			this.turnLeft();
			break;
		case 'C':
			this.moveBackward(c.getParameters().substring(4,8)+c.getParameters().substring(0,4));
			break;
		case 'D':
			this.moveForward(c.getParameters().substring(4,8)+c.getParameters().substring(0,4));
			break;
		case 'F':
			this.stop();
			return;
		}
		
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void log(char c, String parameters) {
		log.push(new Command(c,parameters,System.currentTimeMillis()));
	}

}
