package anypointconnector1.dotnet.jni;

import org.codehaus.jackson.annotate.JsonProperty;

public class JniType {
	public JniType() {
    }

    public JniType(String type, Object value) {
        this.jni_Type = type;
        this.jni_Value = value;
    }

    private String jni_Type;
    private Object jni_Value;

    @JsonProperty("Jni_Type")
    public String getJni_Type() {
        return jni_Type;
    }

    @JsonProperty("Jni_Type")
    public void setJni_Type(String jni_Type) {
        this.jni_Type = jni_Type;
    }

    @JsonProperty("Jni_Value")
    public Object getJni_Value() {
        return jni_Value;
    }

    @JsonProperty("Jni_Value")
    public void setJni_Value(Object jni_Value) {
        this.jni_Value = jni_Value;
    }
}
