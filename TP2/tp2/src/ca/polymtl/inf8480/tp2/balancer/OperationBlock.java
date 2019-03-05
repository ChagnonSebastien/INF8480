package ca.polymtl.inf8480.tp2.balancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import ca.polymtl.inf8480.tp2.shared.ServerInterface;

public class OperationBlock extends Thread {

	List<JsonObject> operations;
	Map<String, Integer> serversCalled;
	
	ServerInterface toCall;

	public OperationBlock(List<JsonObject> operations) {
		this.operations = operations;
	}
	
	@Override
	public void run() {
		
	}

	public synchronized int getResult(boolean secured) throws Exception {
		if (serversCalled.size() < 2)
			throw new Exception("Deux serveurs n'ont pas encore été contactés.");
		
		List<Integer> results = new ArrayList<Integer>(serversCalled.values());
		if (results.get(0) == results.get(1))
			return results.get(0);

		if (serversCalled.size() < 3)
			throw new Exception("Les deux premiers serveurs appelés n'ont pas le même résultat. Un troixième serveur n'a pas encre été contacté.");
		
		return results.get(2);
	}
}