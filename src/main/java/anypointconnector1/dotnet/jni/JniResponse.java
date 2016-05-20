package anypointconnector1.dotnet.jni;

public class JniResponse {
	private JniType payload;
    private JniType invocationProperties;
    private JniType sessionProperties;
    private JniType outboundProperties;
    private String exception;

    public JniType getPayload() {
        return payload;
    }

    public void setPayload(JniType payload) {
        this.payload = payload;
    }

    public JniType getInvocationProperties() {
        return invocationProperties;
    }

    public void setInvocationProperties(JniType invocationProperties) {
        this.invocationProperties = invocationProperties;
    }

    public JniType getSessionProperties() {
        return sessionProperties;
    }

    public void setSessionProperties(JniType sessionProperties) {
        this.sessionProperties = sessionProperties;
    }

    public JniType getOutboundProperties() {
        return outboundProperties;
    }

    public void setOutboundProperties(JniType outboundProperties) {
        this.outboundProperties = outboundProperties;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public boolean failed() {
        return this.exception != null && this.exception.length() > 0;
    }
}
