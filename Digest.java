/**
 * Computing MD5 and SHA256 hashes
 */

import java.io.*;
import java.security.*;
import java.math.BigInteger;

public class Digest{
    /* 
     * Given a file name and the algorithm to be used, it returns a byte array
     * with the digest. 
     */
    public static byte[] diggest(String fname, String algorithm)
            throws IOException, NoSuchAlgorithmException{
            byte[] digest=null; // the result
            FileInputStream in = new FileInputStream(fname);
            // Selecting the algorithm to be used
            MessageDigest sha = MessageDigest.getInstance(algorithm);
            // Reading the file
            DigestInputStream din = new DigestInputStream(in, sha);
            while (din.read() != -1) ; // read entire file
            din.close();
            // Computing the digest
            digest = sha.digest();
            return digest;
    }

    // Computing MD5
    public static byte[] md5(String fname)
            throws IOException, NoSuchAlgorithmException{
            return Digest.diggest(fname, "MD5");
    }

    // Computing SHA-256
    public static byte[] sha256(String fname)
            throws IOException, NoSuchAlgorithmException{
            return Digest.diggest(fname, "SHA-256");
    }

    /**
     * A main for testing purposes. 
     * It is expected that the first argument is the path to the file to be
     * analyzed. 
     */

    public static void main(String arg[]){
        if (arg.length !=1){
            System.out.println("[Error] No input file.\n Try java Digest filename");
            return;
        }
        try{
            String hex = new BigInteger(1, Digest.md5(arg[0])).toString(16);
            System.out.println(hex);
        } catch (IOException ex) {
            System.err.println(ex);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println(ex);
        }
    }
}
