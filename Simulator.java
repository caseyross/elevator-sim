import javax.swing.SwingUtilities;

/**
 * Runs the simulation.
 * Change parameters here to adjust # of elevators and floors.
 */
public class Simulator {
	
	private static SimulationBase base = new SimulationBase(4, 10); // elevators, floors

	public static void main(String[] args) {
		SwingUtilities.invokeLater(base);
		SwingUtilities.invokeLater(new SimulationGUI(base));
	}
	
}