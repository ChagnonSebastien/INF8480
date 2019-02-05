package ca.polymtl.inf8480.tp1.exo2.shared;

import java.io.Serializable;

import com.google.gson.JsonElement;

public class SerializableJson extends JsonElement implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6492848308085807149L;

	@Override
	public JsonElement deepCopy() {
		return null;
	}

}
