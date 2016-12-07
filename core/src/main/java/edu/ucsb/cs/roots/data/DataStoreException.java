package edu.ucsb.cs.roots.data;

public class DataStoreException extends Exception {

    public DataStoreException(String message) {
        super(message);
    }

    public DataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
