package anypointconnector1.dotnet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import anypointconnector1.dotnet.jni.BaseDotNetBridge;
import anypointconnector1.dotnet.jni.DotNetBridge;
import anypointconnector1.dotnet.jni.DotNetBridgeLinux;

public class DotNetInvoker {
	private static final String CONNECTOR_ASSEMBLY_FILE_NAME = "AnyPointConnector1.dll";
	
	private static DotNetInvoker instance = new DotNetInvoker();
	public static DotNetInvoker getInstance() {
		return instance;
	}
	
	private static final BaseDotNetBridge DOT_NET_BRIDGE;
	static {
		// Check which one create depending on platform
		if(BaseDotNetBridge.isLinux())
		{
			DOT_NET_BRIDGE = new DotNetBridgeLinux();
		}
		else
		{
			DOT_NET_BRIDGE = new DotNetBridge();
		}
	}
	
	public Object execute(String assemblyFullyQualifiedName, String typeName, String methodName, Object dotNetInstanceReference, Map<String, Object> arguments)  {	
		try {
			Object request = getRequest(assemblyFullyQualifiedName, typeName, methodName, dotNetInstanceReference, arguments);
			Object result = DOT_NET_BRIDGE.processRequest(request);
			
			return processOutput(result);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public Object createJavaCallback(Object obj, Method method) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return DOT_NET_BRIDGE.createJavaCallback(obj, method);
	}

	private Object getRequest(String assemblyFullyQualifiedName, String typeName, String methodName, Object dotNetInstanceReference, Map<String, Object> arguments) throws Exception {
	    return DOT_NET_BRIDGE.getRequest(assemblyFullyQualifiedName, typeName, methodName, dotNetInstanceReference, arguments, CONNECTOR_ASSEMBLY_FILE_NAME);
	}
	
	private static Object processOutput(Object result) throws Exception {
		Object payload = DOT_NET_BRIDGE.getResult(result);
		if(payload instanceof String) {
			String dotNetResult = payload.toString();
			return dotNetResult;
		}
		
		return payload;
	}
}