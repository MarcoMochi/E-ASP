package application.model.debugger;

import java.util.Objects;

public class CostLevel {
	
	private String level;
	private int cost;
	
	public CostLevel(String level, int cost) {
		this.level = level;
		this.cost = cost;			
	}
	
	public String toText() {
		return "Cost of " + this.getCost() + " at level " +this.getLevel();
	}
	
	public int getCost() {
		return cost;
	}
	
	public String getLevel() {
		return level;
	}
	
	
	@Override
	public String toString() {
		return toText();
	}
	

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CostLevel costLevel = (CostLevel) o;
		return level == costLevel.level && Objects.equals(cost, costLevel.cost);
	}

	@Override
	public int hashCode() {
		return Objects.hash(level, cost);
	}
}
