package mycontroller;

public class ConserveHealthStrategy implements IMovementStrategy {
	
	MyAutoController myAutoController;

	public ConserveHealthStrategy(MyAutoController myAutoController) {
		this.myAutoController = myAutoController;
	}

	@Override
	public void move() {
		System.out.println(myAutoController.checkEast(myAutoController.getView()));
		myAutoController.applyForwardAcceleration();
	}


}
