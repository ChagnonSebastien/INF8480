package ca.polymtl.inf8480.tp2.balancer;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OperationBlock extends ArrayList<JsonObject> {

	int value = -1;
	OperationBlock parent;
	OperationBlock firstChild;
	OperationBlock secondChild;

	private static final long serialVersionUID = 7281619264355830643L;

	public OperationBlock(JsonArray operations) {
		for (JsonElement operation : operations) {
			this.add(operation.getAsJsonObject());
		}
	}

	public OperationBlock(List<JsonObject> subList, OperationBlock parent) {
		super(subList);
		this.parent = parent;
	}

	public synchronized int getValue() {
		if (value != -1) {
			return value % 5000;
		} else if (firstChild != null && secondChild != null) {
			return (firstChild.getValue() + secondChild.getValue()) % 5000;
		} else {
			return -1;
		}
	}

	public void split(int count) {
		this.firstChild = new OperationBlock(this.subList(0, count), this);
		this.secondChild = new OperationBlock(this.subList(count, this.size() - 10), this);
	}
}