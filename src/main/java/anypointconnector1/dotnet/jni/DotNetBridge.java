package anypointconnector1.dotnet.jni;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class DotNetBridge extends BaseDotNetBridge {
	
	@Override
	public Object getRequest(String assemblyFullyQualifiedName,
			String typeName, String methodName, Object dotNetInstanceReference,
			Map<String, Object> arguments, String connectorAssemblyFileName) throws Exception, IllegalArgumentException, InvocationTargetException 
	{
		Object request = requestClass.newInstance();
		setConnectorAssemblyFilePathMethod.invoke(request, new Object[] { getWorkingPath().resolve(connectorAssemblyFileName).toString()});
		setAssemblyFullyQualifiedNameMethod.invoke(request, new Object[] { assemblyFullyQualifiedName });
		setTypeNameMethod.invoke(request, new Object[] { typeName });
		setMethodNameMethod.invoke(request, new Object[] { methodName });
		setDotNetInstanceReferenceMethod.invoke(request, new Object[] { dotNetInstanceReference });
		setMethodArgumentsMethod.invoke(request, new Object[] { arguments });
		setLogMethod.invoke(request, new Object[] { true });
		setFullTrustMethod.invoke(request, new Object[] { true });
		
		return request;
	}

	@Override
	public Object processRequest(Object request) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Object val = invokeNetMethod.invoke(bridgeInstance, new Object[] { request } );
		return val;	
	}

	@Override
	public Object createJavaCallback(Object obj, Method method) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return javaCallbackClass.getConstructor(Object.class, Method.class).newInstance(obj, method);
	}
	
	@Override
	public Object getResult(Object response) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return getResultMethod.invoke(response);
	}

}