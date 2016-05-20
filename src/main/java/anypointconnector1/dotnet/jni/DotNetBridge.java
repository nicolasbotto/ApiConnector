package anypointconnector1.dotnet.jni;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import anypointconnector1.dotnet.DotNetInvoker;











//import org.mule.modules.dotnet.instrumentation.DotNetNotificationManager;
import org.mule.util.ExceptionUtils;

public class DotNetBridge extends BaseDotNetBridge 
{
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