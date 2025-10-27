# Elevator
Java code that simulates an elevator

# Assumptions:
* All elevators are controlled by one central elevator controller
* Regardless of how the floors are labeled, they are passed into the elevator functions based on the total number of floors, 0-indexed. 
  * For example, if there are 6 floors: B, G, 1, M, 2, 3, then B = floor 0, G = floor 1, etc.
* Elevators generally try to head in one direction whenever possible, picking up and dropping people off along the way. The exception is when an elevator finishes its route. Then it goes to service the person who has been waiting the longest to try and reduce the time spent waiting for an elevator.
* People inside the elevator get priority over people outside the elevator when it comes to choosing floors
  * For example, if someone pushes a button inside the elevator, the elevator will focus on delivering that person to the floor over picking up waiting people on the outside
* When an elevator goes idle, it stays on the floor it is on
* Elevators don’t claim any floor outside of a maximum threshold set by the elevator controller. This gives another idle elevator a chance to get passengers who are further away
* The elevator to pick up someone should be the closest elevator going in the compatible direction, or the closest idle elevator if there is no moving elevator
* Elevators can only go up or down or stay on the same floor
* Elevators are either idle, moving, or out of service
* Elevators wait for 7 seconds with the door open and also wait an additional 2 seconds when opening and closing doors (for presumable mechanical safety checks)
* The elevators properly handle concurrency to avoid race conditions

# Not yet implemented:
* Anything interfacing with the mechanic side of elevators such as code to open and close doors
* Emergency stop/call functionality
* Elevator customizability
  * Elevator "home floor" where elevators return to a set floor instead of idling at the current floor
  * Varying permissions for different floors (such as keycard access)
  * Different types of elevators (service elevator, normal elevator, etc)
  * Ability to program an elevator schedule to dynamically change the home floor based on time of day
* Properly handling concurrency for multiple elevators
