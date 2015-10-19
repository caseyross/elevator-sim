import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A graphical display of the SimulationBase model.
 */
public class SimulationGUI extends JFrame implements Runnable {
	
	private SimulationBase b;
	private BuildingPanel building;
	private ControlPanel controls;
	private int animationDelay = 20;
	private Timer t;
	private boolean forceOneToOneUpdates = true;
	private final Color fgcolor = Color.white;
	private final Color bgcolor = Color.black;
	
	public SimulationGUI(SimulationBase s) {
		b = s;
		building = new BuildingPanel();
		add(building);
		controls = new ControlPanel();
		t = new Timer(animationDelay, new TimerListener());
		setSize(400, 400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * Start or restart the simulation.
	 */
	public void run() {
		if(b.getUpdateFrequency() != animationDelay && forceOneToOneUpdates) b.setUpdateFrequency(animationDelay);
		if(!t.isRunning()) {
			b.run();
			t.restart();
		}
	}
	
	/**
	 * Pause the simulation.
	 */
	public void pause() {
		if(t.isRunning()) {
			b.pause();
			t.stop();
		}
	}
	
	public void setAnimationDelay(int s) {
		animationDelay = s;
	}
	
	private class TimerListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			building.repaint();
			controls.repaint();
		}
		
	}
	
	private class BuildingPanel extends JPanel {
		
		private int floors;
		private ElevatorPanel[] elevators;
		private int[] shaftLocations;
		private int[] personLayout = new int[2];
		private Dimension previousSize = new Dimension(0, 0);
		private LayoutManager layoutManager = new LayoutManager();
		private int personSpacing = 4, personSizeX, personSizeY;
		
		public BuildingPanel() {
			SwingUtilities.invokeLater(layoutManager);
			setBackground(bgcolor);
			floors = b.getNumberOfFloors();
			int numElevators = b.getElevatorList().length;
			elevators = new ElevatorPanel[numElevators];
			for(int i = 0; i < numElevators; i ++) {
				elevators[i] = new ElevatorPanel(b.getElevatorList()[i]);
				add(elevators[i]);
			}
			shaftLocations = new int[elevators.length];
			addMouseListener(new ClickListener());
		}
		
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			checkForNewElevatorSize();
			g.setColor(fgcolor);
			draw(g);
			for(int i = 0; i < elevators.length; i ++) {
				shaftLocations[i] = (int)(getParent().getWidth() * (double)(2*i + 1) / (2 * elevators.length + 1)) + 1;
				elevators[i].setX(shaftLocations[i]);
				elevators[i].setSize((int)(getWidth() / (2 * elevators.length + 1)), (int)(getHeight() / floors));
				elevators[i].repaint();
			}
		}

		/**
		 * Compare the previous size with the current size to see if we need a new layout.
		 */
		private void checkForNewElevatorSize() {
			if(!elevators[0].getSize().equals(previousSize)) {
				layoutManager.getNewLayout();
				previousSize = elevators[0].getSize();
				}
			}
		
		/**
		 * Draw floors and people waiting.
		 * @param g
		 */
		public void draw(Graphics g) {
			
			// Draw average wait time
//			g.drawString(String.valueOf(b.getAverageWaitTime()), getWidth() / 2, 10);
			
			// Draw floor lines
			for(int i = 0; i < floors; i ++) {
				int drawHeight = (int)(((double)(i + 1) / (floors)) * getHeight());
				g.drawLine(0, drawHeight, shaftLocations[0], drawHeight);
//				g.drawString(String.valueOf(b.getNumberOfPeopleOnFloor(i)), shaftLocations[0] / 2, getHeight() - (int)(((i + .5) / (floors)) * getHeight()));
				for(int j = 1; j < elevators.length; j ++) {
					g.drawLine(shaftLocations[j] - elevators[j - 1].getWidth(), drawHeight, shaftLocations[j], drawHeight);
				}
				g.drawLine(getWidth() - elevators[elevators.length - 1].getWidth(), drawHeight, getWidth(), drawHeight);
				
				//Draw people waiting
				if(!b.getPeopleOnFloor(i).isEmpty()) {
					int row = 0, col = 0, pl = 0 /* platform number */, x, y, platformWidth = shaftLocations[0];
					for(Person p : b.getPeopleOnFloor(i)) {
						
						// Draw person
						if(pl < elevators.length) x = shaftLocations[pl] - platformWidth + (col + 1) * personSpacing + col * personSizeX;
						else x = getWidth() - platformWidth + (col + 1) * personSpacing + col * personSizeX;
						y = getHeight() + getHeight() / floors - (drawHeight + ((row + 1) * personSpacing + row * personSizeY) - 1);
						g.drawRect(x, y - personSizeY, personSizeX, personSizeY);
						
						// Check for new platfom, row, column
							if(col < personLayout[1] - 1) col ++;
							else {
								col = 0;
								if(pl < elevators.length) pl ++;
								else {
									pl = 0;
									if(row < personLayout[0] - 1) row ++;
									else row = 0;
								}
							}
						}
					}
				
			}
			
		}
		
		private class ElevatorPanel extends JPanel {
			
			private Elevator e = new Elevator();
			private Border border = BorderFactory.createLineBorder(Color.green);
			private Border waitBorder = BorderFactory.createLineBorder(Color.yellow);
			private int x;
			private int[] layout = new int[2];
			
			public ElevatorPanel(Elevator e) {
				this.e = e;
				setBackground(bgcolor);
				setBorder(border);
			}
			
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.setColor(fgcolor);
				draw(g);
			}
			
			/**
			 * Draw elevator and occupants.
			 * @param g
			 */
			public void draw(Graphics g) {
				
				// Draw elevator
				int parentHeight = getParent().getHeight();
				setLocation(x, (int)(parentHeight - (e.getPosition() + 1) / b.getNumberOfFloors() * parentHeight));
//				g.drawString(String.valueOf(e.getPosition()), getWidth() / 2, getHeight() / 2);
				
				// Draw number of people
//				g.drawString(String.valueOf(e.getOccupants().size()), getWidth() / 2, getHeight() / 2);
				
				// Draw up and down arrows
//				if(e.getDirection() != null) {
//					if(e.getDirection().equals("up")) g.drawString("^", getWidth() / 2, getHeight() / 2 - 10);
//					if(e.getDirection().equals("down")) g.drawString("v", getWidth() / 2, getHeight() / 2 + 10);
//				}
				
				// Draw occupants
				if(!e.getOccupants().isEmpty());
					int i = 0, j = 0, x, y;
					for(Person p : e.getOccupants()) {
						
						// Draw person
						x = (j + 1) * personSpacing + j * personSizeX;
						y = getHeight() - ((i + 1) * personSpacing + i * personSizeY) - 1;
						g.drawRect(x, y - personSizeY, personSizeX, personSizeY);
						
						// Draw destination on person (if large enough to show)
						if(personSizeX > 10 && personSizeY > 15) {
							g.drawString(String.valueOf(p.getDestination()), x + personSizeX / 2 - 3, y - personSizeY / 2 + 4);
						}
						
						// Check for new row, column
						if(j < layout[1] - 1) j ++;
						else {
							j = 0;
							if(i < layout[0] - 1) i ++;
							else i = 0;
						}
					}
				
				// Color border if waiting
				if(e.getStatus().equals("waiting") && getBorder().equals(border)) setBorder(waitBorder);
				if(getBorder().equals(waitBorder) && !e.getStatus().equals("waiting")) setBorder(border);
			}
			
			public void setX(int value) {x = value;}
			
			public void setLayout(int[] l) {layout = l;}
			
		}
		
		/**
		 * Manages the layout of the people inside each elevator.
		 */
		private class LayoutManager extends Thread {
			
			public void run() {
				
			}
			
			public void getNewLayout() {
				int rows = 0, cols = 0, c = elevators[0].e.getCapacity();
				double r = (double)getHeight() / getWidth();
				while(rows < c + 1) {
					rows ++;
					if(c % rows == 0) cols = c / rows;
					if((double)rows / cols >= r && rows * cols == c) break;
				}
				personLayout[0] = rows;
				personLayout[1] = cols;
				for(ElevatorPanel p : elevators) p.setLayout(personLayout);
				updatePersonSize();
			}
			
			private void updatePersonSize() {
				personSizeX = (int)((elevators[0].getWidth() - personSpacing * (personLayout[1] + 1)) / (double)personLayout[1]);
				personSizeY = (int)((elevators[0].getHeight() - personSpacing * (personLayout[0] + 1)) / (double)personLayout[0]);
			}
			
		}
		
		/**
		 * A MouseListener that sends elevators to floors.
		 */
		private class ClickListener implements MouseListener {

			public void mouseClicked(MouseEvent e) {
				int floor = (int)((getParent().getHeight() - (double)e.getY()) / getParent().getHeight() * b.getNumberOfFloors());
				System.out.println(floor);
				if(floor > -1 && floor < b.getNumberOfFloors()) b.callElevatorToFloor(floor);
			}

			public void mouseEntered(MouseEvent e) {
				
			}

			public void mouseExited(MouseEvent e) {
				
			}

			public void mousePressed(MouseEvent e) {
				
			}

			public void mouseReleased(MouseEvent e) {
				
			}
			
		}

	}
	
	private class ControlPanel extends JFrame {
	
		private JSlider spawnIntervalSlider;
		private JSlider updateFrequencySlider;
		private JSlider waitTimeSlider;
		
		private DoubleJSlider groupProbabilitySlider;
		private DoubleJSlider spawnProbabilitySlider;
		private DoubleJSlider timeScaleSlider;
		
		//Slider Labels
		private JLabel groupProbabilityLabel = new JLabel("Group Probability");
		private JLabel spawnIntervalLabel = new JLabel("Spawn Interval");
		private JLabel spawnProbabilityLabel = new JLabel("Spawn Probability");
		private JLabel timeScaleLabel = new JLabel("Time Scale");
		private JLabel updateFrequencyLabel = new JLabel("Update Frequency");
		private JLabel waitTimeLabel = new JLabel("Wait Time");
		
		//Slider Current Values
		private JLabel groupProbabilityValue;
		private JLabel spawnIntervalValue;
		private JLabel spawnProbabilityValue;
		private JLabel timeScaleValue;
		private JLabel updateFrequencyValue;
		private JLabel waitTimeValue;
		
		private JButton pauseButton = new JButton("Pause");
		
		private ControlPanel() {
			
			spawnIntervalSlider = new JSlider(JSlider.HORIZONTAL, b.getMinSpawnInterval(), b.getMaxSpawnInterval(), b.getDefaultSpawnInterval());
			updateFrequencySlider = new JSlider(JSlider.HORIZONTAL, b.getMaxUpdateFrequency(), b.getMinUpdateFrequency(), b.getDefaultUpdateFrequency());
			waitTimeSlider = new JSlider(JSlider.HORIZONTAL, b.getMinLoadingWaitTime(), b.getMaxLoadingWaitTime(), b.getDefaultLoadingWaitTime());
			
			groupProbabilitySlider = new DoubleJSlider(b.getMinGroupMemberProbability(), b.getMaxGroupMemberProbability(), b.getDefaultGroupMemberProbability());
			spawnProbabilitySlider = new DoubleJSlider(b.getMinSpawnProbability(), b.getMaxSpawnProbability(), b.getDefaultSpawnProbability());
			timeScaleSlider = new DoubleJSlider(-b.getMaxTimeScaleFactor(), b.getMaxTimeScaleFactor(), 0);
			
			groupProbabilityValue = new JLabel(String.valueOf((int)(b.getDefaultGroupMemberProbability() * 100)) + "%");
			spawnIntervalValue = new JLabel(String.valueOf(b.getDefaultSpawnInterval() / 1000.0) + "s");
			spawnProbabilityValue = new JLabel(String.valueOf((int)(b.getDefaultSpawnProbability() * 100)) + "%");
			timeScaleValue = new JLabel(String.valueOf(b.getDefaultTimeScale()) + "x");
			updateFrequencyValue = new JLabel(String.valueOf(b.getDefaultUpdateFrequency() / 1000.0) + "s");
			waitTimeValue = new JLabel(String.valueOf(b.getDefaultLoadingWaitTime() / 1000.0) + "s");
			
			//Create and add slider listener
			SliderListener sldListen = new SliderListener();
			spawnIntervalSlider.addChangeListener(sldListen);
			updateFrequencySlider.addChangeListener(sldListen);
			waitTimeSlider.addChangeListener(sldListen);
			groupProbabilitySlider.addChangeListener(sldListen);
			spawnProbabilitySlider.addChangeListener(sldListen);
			timeScaleSlider.addChangeListener(sldListen);
			
			//Create and add button listener
			ButtonListener btnListen = new ButtonListener();
			pauseButton.addActionListener(btnListen);
			
			setLayout();
			
		}
		
		
			
		private class SliderListener implements ChangeListener {
			
			public void stateChanged(ChangeEvent e) {
				if(e.getSource().equals(spawnIntervalSlider)) {
					b.getSpawner().setSpawnInterval(spawnIntervalSlider.getValue());
					if(spawnIntervalSlider.getValue() < 0) spawnIntervalValue.setText("None");
					else spawnIntervalValue.setText(String.valueOf(spawnIntervalSlider.getValue() / 1000.0) +"s");
				}
				else if(e.getSource().equals(updateFrequencySlider)) {
					b.setUpdateFrequency(updateFrequencySlider.getValue());
					updateFrequencyValue.setText(String.valueOf(updateFrequencySlider.getValue() / 1000.0) + "s");
				}
				else if(e.getSource().equals(waitTimeSlider)) {
					b.setLoadingWaitTime(waitTimeSlider.getValue());
					waitTimeValue.setText(String.valueOf(waitTimeSlider.getValue() / 1000.0) + "s");
				}
				else if(e.getSource().equals(groupProbabilitySlider)) {
					b.getSpawner().setGroupMemberProbability(groupProbabilitySlider.getDoubleValue());
					groupProbabilityValue.setText(String.valueOf((int)(groupProbabilitySlider.getDoubleValue() * 100)) + "%");
				}
				else if(e.getSource().equals(spawnProbabilitySlider)) {
					b.getSpawner().setSpawnProbability(spawnProbabilitySlider.getDoubleValue());
					spawnProbabilityValue.setText(String.valueOf((int)(spawnProbabilitySlider.getDoubleValue() * 100)) + "%");
				}
				else if(e.getSource().equals(timeScaleSlider)) {
					b.setTimeScale(Math.pow(2, timeScaleSlider.getDoubleValue()));
					String timeScale = String.valueOf(Math.pow(2, timeScaleSlider.getDoubleValue()));
					if(timeScale.length() > 4) {
						timeScaleValue.setText(timeScale.substring(0,5) + "x");
					}
					else timeScaleValue.setText(timeScale + "x");
				}
			}
			
		}
		
		private class ButtonListener implements ActionListener {
		
			 public void actionPerformed(ActionEvent e) {
                if(t.isRunning()) {
					pause();
					pauseButton.setText("Resume");
				}
				else {
					run();
					pauseButton.setText("Pause");
				}
            }
		}
		
		private void setLayout() {
		
			javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
			getContentPane().setLayout(layout);
			layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
						.addComponent(updateFrequencyLabel)
						.addComponent(waitTimeLabel)
						.addComponent(spawnIntervalLabel)
						.addComponent(groupProbabilityLabel)
						.addComponent(spawnProbabilityLabel)
						.addComponent(timeScaleLabel))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(waitTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(updateFrequencySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(spawnIntervalSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(spawnProbabilitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(groupProbabilitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(timeScaleSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(updateFrequencyValue)
						.addComponent(waitTimeValue)
						.addComponent(spawnIntervalValue)
						.addComponent(spawnProbabilityValue)
						.addComponent(groupProbabilityValue)
						.addComponent(timeScaleValue))
					.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
					.addContainerGap(173, Short.MAX_VALUE)
					.addComponent(pauseButton)
					.addGap(117, 117, 117))
			);
			layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addContainerGap()
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
							.addComponent(waitTimeSlider, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(waitTimeLabel, javax.swing.GroupLayout.Alignment.LEADING))
						.addComponent(waitTimeValue))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(updateFrequencySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(updateFrequencyLabel)
						.addComponent(updateFrequencyValue))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(spawnIntervalSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(spawnIntervalLabel)
						.addComponent(spawnIntervalValue))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(spawnProbabilitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(spawnProbabilityLabel)
						.addComponent(spawnProbabilityValue))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(groupProbabilitySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(groupProbabilityLabel)
						.addComponent(groupProbabilityValue))
					.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
					.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addComponent(timeScaleSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addComponent(timeScaleLabel)
						.addComponent(timeScaleValue))
					.addGap(18, 18, 18)
					.addComponent(pauseButton)
					.addContainerGap())
			);

			pack();
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			setVisible(true);
		}
	}
}
