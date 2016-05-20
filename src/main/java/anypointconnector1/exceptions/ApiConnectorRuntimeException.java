package anypointconnector1.exceptions;

public class ApiConnectorRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ApiConnectorRuntimeException(Throwable cause) {
        super(cause);
    }
}
