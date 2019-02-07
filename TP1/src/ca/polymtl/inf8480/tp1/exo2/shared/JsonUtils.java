/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP1 - INF8480
 */

package ca.polymtl.inf8480.tp1.exo2.shared;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class JsonUtils {

	/*
	 * Ecrit la chaine object dans le fichier file
	 */
	public static void writeToFile(String object, File file) {
		FileWriter writer;
		
		try {
			writer = new FileWriter(file, false);
			PrintWriter print_line = new PrintWriter(writer);
			print_line.println(object);
			print_line.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
