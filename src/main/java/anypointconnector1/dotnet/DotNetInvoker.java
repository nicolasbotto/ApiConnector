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
		Object request = DOT_NET_BRIDGE.getRequest(assemblyFullyQualifiedName, typeName, methodName, 
				dotNetInstanceReference, arguments, CONNECTOR_ASSEMBLY_FILE_NAME);
		
		return request;
	}
	
	private static Map<String, Object> transformArguments(Map<String, Object> arguments) {
		if(arguments != null) {
			for (Map.Entry<String, Object> entry : arguments.entrySet()) {
				Object value = entry.getValue();
				if(value instanceof Map<?,?>) {
					value = getArgumentInJson(entry);
					arguments.put(entry.getKey(), value);
				}
	        }
		}
		
		return arguments;
	}
	
	@SuppressWarnings("unchecked")
	private static Object getArgumentInJson(Map.Entry<String, Object> entry) {
		String jsonFormat = "\"%s\" : %s";
		
		Object value = entry.getValue();
		if(value instanceof Map<?,?>) {
			Map<String, Object> complex = (Map<String, Object>)value;
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for (Map.Entry<String, Object> param : complex.entrySet()) {
				sb.append(String.format(jsonFormat, param.getKey(), getArgumentInJson(param)));
				sb.append(", ");
	        }

			if(sb.length() > 1) {
				sb = sb.delete(sb.length()-2, sb.length());
            }
			
			sb.append("}");
			value = sb.toString();
		}
		else {
			value = "\"" + value.toString() + "\"";
		}
		
		return value;
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