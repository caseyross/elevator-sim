# elevator-sim
Simulation and visualization of elevator movement in a building

## disclaimer
This repository is here for the purpose of historical preservation only. The code has been frozen in the state it was turned in.

## notes
This was the final project for my CS 2 class in 2012. I did everything except for the simulation's control panel.

There is an outstanding bug where speeding up time can cause elevators to fly out of the building.

The final grade for the project was 96. This was likely because the assignment requested 5 elevators but ours defaulted to 4 (with option to go arbitrarily high).

There are an excess of getters and setters. I believe this is one of my teammates' faults. Other than that, I think the project is well architected in OO style. The simulation math is completely separate from the graphics code and can run independently.

Clicking a floor will request an elevator there.