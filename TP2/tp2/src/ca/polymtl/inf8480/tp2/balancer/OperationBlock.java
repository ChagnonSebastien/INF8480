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
			throw new Exception("Deux serveurs n'ont pas encore �t� contact�s.");
		
		List<Integer> results = new ArrayList<Integer>(serversCalled.values());
		if (results.get(0) == results.get(1))
			return results.get(0);

		if (serversCalled.size() < 3)
			throw new Exception("Les deux premiers serveurs appel�s n'ont pas le m�me r�sultat. Un troixi�me serveur n'a pas encre �t� contact�.");
		
		return results.get(2);
	}
}