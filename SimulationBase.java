import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * The simulation model itself.
 */
public class SimulationBase extends Thread {

	private Elevator[] elevators;
	private int floors;
	private double averageWaitTime = 0;
	private final int MIN_LOADING_WAIT_TIME = 0, MAX_LOADING_WAIT_TIME = 30000, DEFAULT_LOADING_WAIT_TIME = 3000;
	private final int MIN_UPDATE_FREQUENCY = 500, MAX_UPDATE_FREQUENCY = 1, DEFAULT_UPDATE_FREQUENCY = 20;
	private final int MIN_SPAWN_INTERVAL = -1 /* No spawn */, MAX_SPAWN_INTERVAL = 500, DEFAULT_SPAWN_INTERVAL = 100;
	private final int MAX_TIME_SCALE_FACTOR = 5; // Fastest speed = 2 ^ n, slowest 2 ^ -n
	private final double MIN_SPAWN_PROBABILITY = 0, MAX_SPAWN_PROBABILITY = .99, DEFAULT_SPAWN_PROBABILITY = .05;
	private final double MIN_GROUP_MEMBER_PROBABILITY = 0, MAX_GROUP_MEMBER_PROBABILITY = .99, DEFAULT_GROUP_MEMBER_PROBABILITY = .5;
	private final double MIN_TIME_SCALE = Math.pow(2, -MAX_TIME_SCALE_FACTOR), MAX_TIME_SCALE = Math.pow(2, MAX_TIME_SCALE_FACTOR), DEFAULT_TIME_SCALE = 1;
	private Timer t;
	private Scheduler scheduler = new Scheduler();
	private Spawner spawner = new Spawner();
	private ArrayList<LinkedList<Person>> people = new ArrayList<LinkedList<Person>>();
	private int updateFrequency = DEFAULT_UPDATE_FREQUENCY;
	private double timeScale = DEFAULT_TIME_SCALE; // Ratio of simulation time to real time
	
	public SimulationBase(int numElevators, int floors) {
		elevators = new Elevator[numElevators];
		for(int i = 0; i < numElevators; i ++) {
			elevators[i] = new Elevator();
			elevators[i].calibrateTimeScale((double)DEFAULT_UPDATE_FREQUENCY / 1000);
			elevators[i].setWaitTime(DEFAULT_LOADING_WAIT_TIME);
		}
		this.floors = floors;
		for(int i = 0; i < floors; i ++) people.add(new LinkedList<Person>());
		t = new Timer(DEFAULT_UPDATE_FREQUENCY, new TimerListener());
		spawner.setSpawnInterval(DEFAULT_SPAWN_INTERVAL);
		spawner.setSpawnProbability(DEFAULT_SPAWN_PROBABILITY);
		spawner.setGroupMemberProbability(DEFAULT_GROUP_MEMBER_PROBABILITY);
	}
	
	public void run() {
		if(!scheduler.isAlive()) SwingUtilities.invokeLater(scheduler);
		if(!spawner.isAlive()) SwingUtilities.invokeLater(spawner);
		if(!t.isRunning()) {
			t.restart();
			spawner.run();
		}
	}
	
	public void pause() {
		if(t.isRunning()) {
			spawner.pause();
			t.stop();
		}
	}
	
	public double getAverageWaitTime() {
		return averageWaitTime;
	}
	
	public Elevator[] getElevatorList() {
		return elevators;
	}
	
	LinkedList<Person> getPeopleOnFloor(int f) {
		return people.get(f);
	}

	public int getNumberOfFloors() {
		return floors;
	}
	
	public int getNumberOfPeopleOnFloor(int f) {
		return people.get(f).size();
	}
	
	public int getMinLoadingWaitTime() {
		return MIN_LOADING_WAIT_TIME;
	}

	public int getMaxLoadingWaitTime() {
		return MAX_LOADING_WAIT_TIME;
	}
	
	public int getDefaultLoadingWaitTime() {
		return DEFAULT_LOADING_WAIT_TIME;
	}
	
	public void setLoadingWaitTime(int t) {
		for(Elevator e : elevators) e.setWaitTime(t);
	}
	
	public int getMinUpdateFrequency() {
		return MIN_UPDATE_FREQUENCY;
	}
	
	public int getMaxUpdateFrequency() {
		return MAX_UPDATE_FREQUENCY;
	}
	
	public int getDefaultUpdateFrequency() {
		return DEFAULT_UPDATE_FREQUENCY;
	}
	
	public int getUpdateFrequency() {
		return updateFrequency;
	}
	
	public void setUpdateFrequency(int f) {
		updateFrequency = f;
		for(Elevator e : elevators) e.calibrateTimeScale(f / 1000);
	}
	
	public int getMinSpawnInterval() {
		return MIN_SPAWN_INTERVAL;
	}
	
	public int getMaxSpawnInterval() {
		return MAX_SPAWN_INTERVAL;
	}
	
	public int getDefaultSpawnInterval() {
		return DEFAULT_SPAWN_INTERVAL;
	}
	
	public double getMinSpawnProbability() {
		return MIN_SPAWN_PROBABILITY;
	}
	
	public double getMaxSpawnProbability() {
		return MAX_SPAWN_PROBABILITY;
	}
	
	public double getDefaultSpawnProbability() {
		return DEFAULT_SPAWN_PROBABILITY;
	}
	
	public double getMinGroupMemberProbability() {
		return MIN_GROUP_MEMBER_PROBABILITY;
	}
	
	public double getMaxGroupMemberProbability() {
		return MAX_GROUP_MEMBER_PROBABILITY;
	}
	
	public double getDefaultGroupMemberProbability() {
		return DEFAULT_GROUP_MEMBER_PROBABILITY;
	}
	
	public double getMaxTimeScaleFactor() {
		return MAX_TIME_SCALE_FACTOR;
	}
	
	public double getMinTimeScale() {
		return MIN_TIME_SCALE;
	}
	
	public double getMaxTimeScale() {
		return MAX_TIME_SCALE;
	}
	
	public double getDefaultTimeScale() {
		return DEFAULT_TIME_SCALE;
	}
	
	/**
	 * Adjust the simulation time : real time ratio.
	 * @param t ratio of sim time to real time
	 */
	public void setTimeScale(double t) {
		spawner.setSpawnInterval((int)(spawner.getSpawnInterval() / (t / timeScale)));
		for(Elevator e : elevators) e.setTimeScale(t);
		timeScale = t;
	}
	
	public Spawner getSpawner() {
		return spawner;
	}
	
	/**
	 * Ask the Scheduler to send an elevator to a floor.
	 * @param f the floor
	 */
	public void callElevatorToFloor(int f) {
		scheduler.callBestElevator(f);
	}

	/**
	 * Ask the Scheduler to send an elevator going in the specified direction to a floor.
	 * @param f the floor
	 * @param direction "up" or "down"
	 */
	public void callElevatorToFloor(int f, String direction) {
		scheduler.callBestElevator(f, direction);
	}
	
	/**
	 * Move people from the building to an elevator.
	 * @param e the elevator to load
	 */
	private void load(Elevator e) {
		int currentFloor = (int)e.getPosition();
		
		// While we have people waiting on the elevator's current floor and space in the elevator, move them to the elevator
		Iterator<Person> iter = people.get(currentFloor).iterator();
		while(iter.hasNext() && e.getOccupants().size() < e.getCapacity()) {
			Person p = iter.next();
			if(e.getDirection() == null || (p.getDestination() > currentFloor && e.getDirection().equals("up")) || (p.getDestination() < currentFloor && e.getDirection().equals("down"))) {
				e.addOccupant(p);
				iter.remove();
			}
		}
		
	}
	
	/**
	 * The core TimerListener of the simulation. Responsible for updating each elevator.
	 */
	private class TimerListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			
			// Update average wait time
/*			for(int i = 0; i < people.size(); i ++) {
				for(Person p : people.get(i)) {
					averageWaitTime += ((double)p.getTimeSinceBirth() / 1000 - averageWaitTime) / spawner.getNumberSpawned() * updateFrequency / 1000 * timeScale;
				}
			}
*/			
			// Update elevators
			for(int i = 0; i < elevators.length; i ++) {
				elevators[i].update();
				if(elevators[i].getSpeed() == 0) load(elevators[i]);
			}
		}
		
	}
	
	/**
	 * An algorithm that chooses which elevator responds to each call.
	 */
	private class Scheduler extends Thread {
		
		public void run() {
			
		}
		
		public void callBestElevator(int floor) {
			getBestElevator(floor).goTo(floor);
		}
		
		public void callBestElevator(int floor, String direction) {
			if(getBestElevator(floor, direction) != null) getBestElevator(floor, direction).goTo(floor);
		}
		
		public Elevator getBestElevator(int floor) {
			LinkedList<Elevator> candidates = new LinkedList<Elevator>();
			
			// Find an elevator with no further destinations
			for(Elevator e : elevators) if(e.getDirection() == null) candidates.add(e);
			if(getClosestElevator(floor, candidates) != null) return getClosestElevator(floor, candidates);
			
			// Find an elevator moving in the right direction
			for(Elevator e : elevators) {
				if(floor > e.getPosition() && e.getDirection().equals("up")) candidates.add(e);
				if(floor < e.getPosition() && e.getDirection().equals("down")) candidates.add(e);
				}
			if(getClosestElevator(floor, candidates) != null) return getClosestElevator(floor, candidates);
			
			// Find an elevator moving in the wrong direction
			else {
				for(Elevator e : elevators) candidates.add(e);
				Elevator leastBusyElevator = new Elevator();
				int shortestQueue = floors;
				for(Elevator c : candidates) {
					if(c.getDestinations().size() < shortestQueue) {
						shortestQueue = c.getDestinations().size();
						leastBusyElevator = c;
					}
				}
				return leastBusyElevator;
			}
		}
		
		public Elevator getBestElevator(int floor, String direction) {
			LinkedList<Elevator> candidates = new LinkedList<Elevator>();
			
			// Find an elevator with no further destinations
			for(Elevator e : elevators) if(e.getDirection() == null) candidates.add(e);
			if(getClosestElevator(floor, candidates) != null) return getClosestElevator(floor, candidates);
			
			// Find an elevator moving in the right direction
			for(Elevator e : elevators) {
				if(!direction.equals("down") && floor > e.getPosition() && e.getDirection().equals("up")) candidates.add(e);
				else if(!direction.equals("up") && floor < e.getPosition() && e.getDirection().equals("down")) candidates.add(e);
				}
			if(getClosestElevator(floor, candidates) != null) return getClosestElevator(floor, candidates);
			
			return null;
			
		}
		
		private Elevator getClosestElevator(int floor, LinkedList<Elevator> candidates) {
			if(candidates.size() == 1) return candidates.getFirst();
			if(candidates.size() > 1) {
				Elevator closestElevator = candidates.getFirst();
				for(Elevator c : candidates) if(Math.abs(c.getPosition() - floor) < Math.abs(closestElevator.getPosition() - floor)) closestElevator = c;
				return closestElevator;
			}
			return null;
		}
		
	}
	
	/**
	 * Creates people randomly in the building.
	 */
	protected class Spawner extends Thread {
		
		private int count = 0;
		private double spawnProbability, groupMemberProbability; // Probability of creating each additional group and additional group member
		private Timer spawnTimer = new Timer(0, new TimerListener());
		
		public int getNumberSpawned() {
			return count;
		}
		
		public int getSpawnInterval() {
			return spawnTimer.getDelay();
		}
		
		public void run() {
			if(!spawnTimer.isRunning()) spawnTimer.restart();
		}
		
		public void pause() {
			if(spawnTimer.isRunning()) spawnTimer.stop();
		}
		
		public void setGroupMemberProbability(double p) {
			groupMemberProbability = p;
		}
		
		public void setSpawnInterval(int f) {
			if(f > 0) {
				run();
				spawnTimer.setDelay((int)(f * timeScale));
			}
			else pause();
		}
		
		public void setSpawnProbability(double p) {
			spawnProbability = p;
		}
		
		public void spawn() {
			
			// Generate successive groups
			while(Math.random() < spawnProbability) {
				
				// Create either:
				// A person at floor 0 that wants to go to another floor or
				// A person at another floor that wants to go to floor 0
				int startingFloor = 0;
				if(Math.random() < .5) startingFloor = (int)(Math.random() * (floors - 1)) + 1;
				int destination = 0;
				if(startingFloor == 0) destination = (int)(Math.random() * (floors - 1)) + 1;
				
				// Generate successive group members
				do {
					people.get(startingFloor).add(new Person(destination));
					count ++;
				}
				while(Math.random() < groupMemberProbability);

				// Order pickup
				callElevatorToFloor(startingFloor, (startingFloor < destination) ? "up" : "down");
			}
		}
		
		private class TimerListener implements ActionListener {
			
			public void actionPerformed(ActionEvent e) {
				spawn();
			}
			
		}
		
	}
	
}
