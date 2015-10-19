
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.Timer;

/**
 * Elevator for use with simulation.
 */
public class Elevator {
	
	private final int CAPACITY = 12;
	private double acceleration = 1;  // Floors per sec ^2
	private double speed, currentPosition = 0, halfwayPoint, minSpeed = .1;
	private int destination, currentFloor = 0; // currentFloor is integer version of currentPosition
	private String status = "stopped";
	private DestinationList destinations = new DestinationList();
	private LinkedList<Person> occupants = new LinkedList<Person>();
	private Timer waitTimer = new Timer(0, new WaitListener());
	private double currentTimeScale = 1;
	
	/**
	 * Add someone to the elevator and input their destination.
	 */
	public void addOccupant(Person p) {
		occupants.add(p);
		goTo(p.getDestination());
	}
	
	/**
	 * Get maximum capacity of elevator.
	 */
	public int getCapacity() {
		return CAPACITY;
	}
	
	/**
	 * Get a list of all current destinations.
	 */
	public DestinationList getDestinations() {
		return destinations;
	}
	
	/**
	 * Get acceleration in floors per second ^ 2 (realtime)
	 */
	public double getAcceleration() {
		return acceleration;
	}
	
	/**
	 * Return current direction of movement. If waiting, return direction of next destination.
	 */
	public String getDirection() {
		if(status.equals("waiting")) {
			if(!destinations.isEmpty()) {
				if(destinations.peekFirst() > currentPosition) return "up";
				else return "down";
			}
			else return null;
		}
		if(status.equals("stopped")) return null;
		return status;
	}
	
	/**
	 * Pull the next destination from the list. It will be either the first or last item.
	 */
	private int getNextDestination() {
		if(!destinations.isEmpty()) {
			if(destinations.peekFirst() > currentPosition) {
				status = "up";
				return destinations.removeFirst();
			}
			if(destinations.peekLast() < currentPosition) {
				status = "down";
				return destinations.removeLast();
			}
			if(destinations.peekFirst() < currentPosition && destinations.peekLast() > currentPosition) {
				if(Math.random() < .5) {
					status = "down";
					return destinations.removeFirst();
				}
				else { 
					status = "up";
					return destinations.removeLast();
				}
			}
			if(destinations.peekFirst() == destination) {;
				status = "waiting";
				waitTimer.restart();
				return destinations.removeFirst();
			}
			if(destinations.peekLast() == destination) {;
				status = "waiting";
				waitTimer.restart();
				return destinations.removeLast();
			}
		}
		throw new RuntimeException("Error: Next destination not found!");
	}
	
	/**
	 * Get the people in the elevator.
	 */
	public LinkedList<Person> getOccupants() {
		return occupants;
	}
	
	/**
	 * Get position of elevator (in floors).
	 */
	public double getPosition() {
		return currentPosition;
	}
	
	/**
	 * Get speed in floors per second (realtime)
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Get status of elevator.
	 * Possible statuses are "up" (going up), "down" (going down), "waiting" (doors open, not moving), and "stopped" (doors closed, not moving).
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * Order the elevator to go to a floor.
	 */
	public void goTo(int floor) {
		if(destinations.isEmpty()) destinations.add(floor);
		else destinations.addInOrder(floor);
	}
	
	/**
	 * If stopped, get next destination. If moving towards a destination, continue. If at destination, wait.
	 */
	private void move() {
		if(status.equals("stopped")) {
			destination = getNextDestination();
			halfwayPoint = (destination + currentPosition) / 2;
		}
		else {
			if(status.equals("up")) {
				if(currentPosition < destination) {
					
					// Accelerate to halfway point, then decelerate
					if(currentPosition < halfwayPoint) {
//						System.out.println(currentPosition);
						speed += acceleration;
//						System.out.println("Current speed is " + speed);
					}
					else {
						if(speed - acceleration > minSpeed) speed -= acceleration;
//						System.out.println("Stopping, floor is " + currentPosition + " and destination is " + destination);
//						System.out.println("Current speed is " + speed);
					}
					
					// Move
					currentPosition += speed;
				}
				else {  // We are at destination
					currentPosition = destination; // Snap to floor
					speed = 0;
					status = "waiting";
					waitTimer.restart();
				}
				currentFloor = (int)Math.floor(currentPosition);
			}
			if(status.equals("down")) {
				if(currentPosition > destination) {
					if(currentPosition > halfwayPoint) speed += acceleration;
					else {
						if(speed - acceleration > minSpeed) speed -= acceleration;
//						System.out.println("Stopping, floor is " + currentPosition + " and destination is " + destination);
//						System.out.println("Current speed is " + speed);
					}
					currentPosition -= speed;
				}
				else {
					currentPosition = destination;
					speed = 0;
					status = "waiting";
					waitTimer.restart();
				}
				currentFloor = (int)Math.ceil(currentPosition);
			}
		}
		
	}
	
	/**
	 * Calibrate movement and wait speeds so that 1 realtime second = 1 simulation second
	 * @param t the simulation's update frequency
	 */
	public void calibrateTimeScale(double t) {
		acceleration *= t * t;
		minSpeed *= t;
		if(t != 0) setWaitTime((int)(waitTimer.getInitialDelay() / (t / currentTimeScale)));
		else setWaitTime(0);
	}
	
	/**
	 * Modify movement and wait speeds to maintain proper elevator behavior relative to time scale
	 * @param t the time scale to adjust to
	 */
	public void setTimeScale(double t) {
		acceleration *= (t / currentTimeScale) * (t / currentTimeScale);
		minSpeed *= (t / currentTimeScale);
		if(t != 0) setWaitTime((int)(waitTimer.getInitialDelay() / (t / currentTimeScale)));
		else setWaitTime(0);
		currentTimeScale = t;
	}
	
	/**
	 * Set how long the elevator waits when stopping at a floor.
	 * @param t the new wait time in ms
	 */
	public void setWaitTime(int t) {
		if(t != 0) waitTimer.setInitialDelay(t);
		else waitTimer.setInitialDelay(0);
	}
	
	/**
	 * Remove people whose destination is the current floor.
	 */
	public void unload() {
		Iterator<Person> iter = occupants.iterator();
		while(iter.hasNext()) {
			if(iter.next().getDestination() == currentPosition) iter.remove();
		}
	}

	/**
	 * Advance the elevator's state. Call repeatedly to run the simulation.
	 */
	public void update() {
		if(destinations.isEmpty() && status.equals("stopped")) return;
		
		// Move people out
		if(status.equals("waiting")) {
			if(!occupants.isEmpty()) unload();
			return;
		}
		
		// Check for intermediate destinations
		if(!status.equals("stopped") && !destinations.isEmpty()) {
			if(destinations.getFirst() < destination && currentPosition < destinations.getFirst()) {
				int temp = destination;
				destination = destinations.removeFirst();
				destinations.addInOrder(temp);
			}
			if(destinations.getLast() > destination && currentPosition > destinations.getLast()) {
				int temp = destination;
				destination = destinations.removeLast();
				destinations.addInOrder(temp);
			}
		}
		move();
	}
	
	/**
	 * An ActionListener that lets our timer run for 1 cycle.
	 */
	class WaitListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			status = "stopped";  // Ready for new orders
			waitTimer.stop();
		}
	
	}
	
}

/**
 * A LinkedList that can insert new elements in order. No duplicate elements allowed.
 */
class DestinationList extends LinkedList<Integer> {
	
	public void addInOrder(Integer value) {
		ListIterator<Integer> iterator = listIterator();
		while(iterator.hasNext()) {
			int next = iterator.next();
			if(next == value) return;
			if(next > value) {
				iterator.previous();
				iterator.add(value);
				return;
			}
		}
		add(value);
	}
	
}