package anypointconnector1.dotnet;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class DotNetObject {
	private static final Log log = LogFactory.getLog(DotNetObject.class);
			
	private String assemblyFullyQualifiedName;
	private String typeName;
	private Object dotNetInstanceReference;

	public DotNetObject(String assemblyFullyQualifiedName, String typeName) {
		this.assemblyFullyQualifiedName = assemblyFullyQualifiedName;
		this.typeName = typeName;
		this.dotNetInstanceReference = null;
	}
	
	protected void callDotNetConstructor(String ctorMethodName, Map<String, Object> ctorArguments) {
		this.dotNetInstanceReference = this.callMethod(ctorMethodName, ctorArguments);
	}
	
	protected Object callMethod(String methodName, Map<String, Object> arguments) {
		return DotNetInvoker.getInstance().execute(this.assemblyFullyQualifiedName, this.typeName, methodName, this.dotNetInstanceReference, arguments);
	}
	
    public Object getDotNetInstanceReference() {
        return this.dotNetInstanceReference;
    }

    @Override
    public void finalize() {
	    try {
			String methodName = ".destroy()";
			
			Map<String, Object> arguments = new HashMap<String, Object>();
			this.callMethod(methodName, arguments);
			this.dotNetInstanceReference = null;
	    } catch (Exception e) {
	    	log.debug("Error during finalize() of type: '" + this.typeName + "' from assembly '" + this.assemblyFullyQualifiedName + "'.", e);
	    }
    }
}