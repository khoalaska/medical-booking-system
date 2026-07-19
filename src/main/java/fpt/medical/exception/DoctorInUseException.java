package fpt.medical.exception;

public class DoctorInUseException extends RuntimeException {
    public DoctorInUseException(String message) {
        super(message);
    }
}
