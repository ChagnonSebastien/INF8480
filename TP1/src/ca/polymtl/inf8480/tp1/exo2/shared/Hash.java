/*
 * @authors : Sébastien Chagnon (1804702), Pierre To (1734636)
 * TP1 - INF8480
 * Référence : https://memorynotfound.com/calculate-file-checksum-java/ 
 */

package ca.polymtl.inf8480.tp1.exo2.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/*
 * Retourne le checksum d'un fichier avec differentes fonctions de hashage
 */
public enum Hash {

    MD5("MD5"),
    SHA1("SHA1"),
    SHA256("SHA-256"),
    SHA512("SHA-512");

    private String name;

    Hash(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String checksum(File input) {
        try (InputStream in = new FileInputStream(input)) {
            MessageDigest digest = MessageDigest.getInstance(getName());
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return new String(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}