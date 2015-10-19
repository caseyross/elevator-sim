
public class Person {
		
		private int destination;
		private long timeCreated;
		
		public Person(int d) {
			destination = d;
			timeCreated = System.currentTimeMillis();
		}

		public int getDestination() {
			return destination;
		}
		
		public long getTimeSinceBirth() {
			return System.currentTimeMillis() - timeCreated;
		}

		public void setDestination(int destination) {
			this.destination = destination;
		}
		
	}