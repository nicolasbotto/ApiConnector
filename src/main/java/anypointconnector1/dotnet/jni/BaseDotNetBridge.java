package anypointconnector1.dotnet.jni;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.mule.util.ExceptionUtils;

import anypointconnector1.dotnet.DotNetInvoker;

public abstract class BaseDotNetBridge {

	private static final Logger LOGGER = Logger.getLogger(DotNetBridge.class);
//	private static final DotNetNotificationManager NOTIFICATION_MANAGER = new DotNetNotificationManager();
	private static Class<?> bridgeClass;
	protected static Class<?> requestClass;
	private static Class<?> responseClass;
	protected static Class<?> javaCallbackClass;
	protected static Class<?> dotNetReferenceClass;
	protected static Object bridgeInstance;
	
	protected static Method setConnectorAssemblyFilePathMethod;
	protected static Method setAssemblyFullyQualifiedNameMethod;
	protected static Method setTypeNameMethod;
	protected static Method setMethodNameMethod;
	protected static Method setDotNetInstanceReferenceMethod;
	protected static Method setMethodArgumentsMethod;
	protected static Method setLogMethod;
	protected static Method setFullTrustMethod;
	protected static Method getDotNetReferenceId;
	protected static Method invokeNetMethod;
	protected static Method getResultMethod;
	
	private static Path pidFilePath;
	private static Path workingPath;
	private static ClassLoader connectorClassLoader;
	
	protected static final String SOCKETNAME = "/tmp/apiconnector";
	private static String jniBridgePrefix = "JniBridge";
	private static String jniBridgeVersion = "1.0.0.0";
	
	private static final String BRIDGE_DLL_FILENAME_PREFIX = "JniBridge";
	private static Path bridgeDllFilePath;
	private static Path monoServerPath;
	private static Process ps = null;
	private static boolean monoServerStarted = false;
	
	static {
		URI domainCodeSourceLocationUri = null;
		try {
			domainCodeSourceLocationUri = DotNetBridge.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		} catch (URISyntaxException e) {
			log("Error when getting the working path.", e);
		}
		
		Path domainCodeSourceLocationPath = Paths.get(domainCodeSourceLocationUri);
		Path connectorJarFilePath = null;
		// It's embedded in a jar, extract resources to parent path
		if(domainCodeSourceLocationPath.toString().endsWith(".jar")) {
			connectorJarFilePath = domainCodeSourceLocationPath;
			workingPath = domainCodeSourceLocationPath.getParent();
			try {
				extractResourcesInJar(connectorJarFilePath);
			} catch (IOException e) {
				log("Error trying to extract resources from jar.", e);
			}
		}
		else {
			workingPath = domainCodeSourceLocationPath;
		}

		log("Found working path: " + workingPath);
		

		// Try delete old bridge dll copies 
		try (java.nio.file.DirectoryStream<Path> oldBridgeDllFiles = Files.newDirectoryStream(workingPath, BRIDGE_DLL_FILENAME_PREFIX + "??-*.dll")) {
			for (Path oldBridgeDllFile : oldBridgeDllFiles) {
				try { 
					Files.delete(oldBridgeDllFile); 
				} catch (Exception e) { }
			 }
		} catch (IOException e) { }
		
		// Rename bridge dll (JNI library) to:
		// - allow to load it in different class loaders
		// - allow the redeploy to replace the dll (not possible if dll is loaded in another process)
		String filePosfix = Long.toString(System.nanoTime());
		Path extractedBridgeDllFilePath;
		Path jniBridgeJar;
		
		if(isLinux())
		{
			extractedBridgeDllFilePath = workingPath.resolve(BRIDGE_DLL_FILENAME_PREFIX + ".so");
			bridgeDllFilePath = workingPath.resolve(BRIDGE_DLL_FILENAME_PREFIX + "-" + filePosfix + ".so");
			monoServerPath = workingPath.resolve("anypointmonoserver");
			jniBridgeJar = workingPath.resolve("jniBridge.linux-" + jniBridgeVersion + ".jar");
			pidFilePath = workingPath.resolve("monoserverpid");
		}
		else
		{
			extractedBridgeDllFilePath = workingPath.resolve(BRIDGE_DLL_FILENAME_PREFIX + getRuntime() + ".dll");
			bridgeDllFilePath = workingPath.resolve(BRIDGE_DLL_FILENAME_PREFIX + getRuntime() + "-" + filePosfix + ".dll");
			jniBridgeJar = workingPath.resolve(jniBridgePrefix + "-" + jniBridgeVersion + ".jar");
		}
		
		try {
			Files.copy(extractedBridgeDllFilePath, bridgeDllFilePath);
		} catch (IOException e) {
			log("Error trying to create a copy of Bridge dll file: " + extractedBridgeDllFilePath, e);
		}
		
		if (!Files.exists(bridgeDllFilePath)) {
			log("Error: unable to find bridge file to load '" + bridgeDllFilePath.toString() + "'.");
		}

		// Discover class loader to load the libraries on
		ClassLoader parentClassLoader = DotNetInvoker.class.getClassLoader();
		
		// Find Bridge's jar file
		URL jniBridgeJarUrl = null;
		try 
		{
			jniBridgeJarUrl = jniBridgeJar.toUri().toURL();
			log("jniBridgeJarUrl: " + jniBridgeJarUrl);
		} 
		catch (MalformedURLException e) 
		{
			log("Incorrect jniBridgeJarUrl: " + jniBridgeJar.toString(), e);
		}

		while(parentClassLoader != null)
		{
			// Runtime: shared class loader
			if(parentClassLoader.getClass().getName().startsWith("org.mule.module.launcher.MuleSharedDomainClassLoader"))
			{
				log("Found Mule shared class loader.");
				break;
			}
			
			// Studio class loader
			if(parentClassLoader.getClass().getName().startsWith("org.mule.tooling.core.classloader.MuleClassLoader"))
			{
				// Studio: shared class loader (when the parent is no Mule's)
				if (!parentClassLoader.getParent().getClass().getName().startsWith("org.mule.tooling.core.classloader.MuleClassLoader")) {
					log("Found Studio shared class loader.");
					break;
				}
			}
			
			parentClassLoader = parentClassLoader.getParent();
		}
		
		if(parentClassLoader == null)
		{
			log("Can't Mule or Studio shared class loader. For unit testing purposes, will use the default one.");
			parentClassLoader = DotNetInvoker.class.getClassLoader();
		}
			
		connectorClassLoader = new URLClassLoader(new URL[] { jniBridgeJarUrl }, parentClassLoader);
			
		// Load Jni Bridge jar in class loader
		log("Loading Jni Bridge jar into class loader from: " + jniBridgeJarUrl.toString());
		try 
		{
			bridgeClass = connectorClassLoader.loadClass("org.mule.api.jni.Bridge");
			log("Jni Bridge class loaded correctly.");
		} 
		catch (ClassNotFoundException e) 
		{
			log("Error loading Jni Bridge class.", e);
		}
			
		if (bridgeInstance == null) 
		{
			try 
			{
				bridgeInstance = bridgeClass.newInstance();
				log("Jni Bridge instance created correctly.");
			}
			catch (InstantiationException | IllegalAccessException e) 
			{
				log("Error creating a Jni Bridge instance.", e);
			}
		}
		
		if(isLinux())
		{
			try 
			{
				dotNetReferenceClass = connectorClassLoader.loadClass("org.mule.api.jni.DotNetInstanceReference");
				getDotNetReferenceId = dotNetReferenceClass.getMethod("getDotNetInstanceId");
			} 
			catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) 
			{
				log("Error loading DotNetReference class.", e);
			}

			final File pidTmp = new File(pidFilePath.toString());
			
			try
			{
				if(pidTmp.exists())
				{
					String pidContent = new String(Files.readAllBytes(pidFilePath), StandardCharsets.UTF_8);
					
					try
					{
						Runtime.getRuntime().exec("kill " + pidContent);
					}
					catch(java.io.IOException e)
					{
						log("Error killing process: " + pidContent + " : " + e.getMessage());
					}
					
					pidTmp.delete();
				}
			}
			catch(java.io.IOException e)
			{
				log("Error reading pid process file: " + pidFilePath.toString());
			}
			
			// Init IPC server
			File file = new File(monoServerPath.toString());
			
			if(!file.canExecute())
			{
				file.setExecutable(true);
			}
			
			// run server
			
			try
			{
				Timer timer = new Timer();
	            timer.schedule(new TimerTask()
            	{
            		@Override
	                public void run() 
            		{
            			if(!monoServerStarted)
	                    {
            				log("Mono Server didn't start.");
	                    }
        			}
	             }, 5000);
	             
				ProcessBuilder pb = null;
				pb = new ProcessBuilder(monoServerPath.toString(), workingPath.toString(), SOCKETNAME);
				pb.redirectErrorStream(true);
				final Process ps = pb.start();
				
	            String line;
	            BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream(), "UTF-8"));
	            
	            StringBuilder sb = new StringBuilder();
	            
	            while ((line = reader.readLine()) != null)
	            {
	                if(line.equalsIgnoreCase("Server started"))
	                {
	                	monoServerStarted = true;
	                	break;
	                }
	                else
	                {
	                	sb.append(line);
	                }
	            }

	            reader.close();
	            
	            if(!monoServerStarted)
	            {
	            	throw new Exception(sb.toString());
	            }
	            
	            // attach to JVM shutdown to remove process and file
	            Runtime.getRuntime().addShutdownHook(
	            		new Thread(
	            			new Runnable() 
            				{
				                public void run() 
				                {
				                	if(ps != null)
				                	{
				                        ps.destroy();
				                    }
				                	
				                	if(pidTmp != null)
				                	{
				                		pidTmp.delete();
				                	}
				                }
            				}));
	            
	            try 
	            {
					Field f = ps.getClass().getDeclaredField("pid");
					f.setAccessible(true);
					int pid = f.getInt(ps);
					
					List<String> pidLine = Arrays.asList(pid+"");
					
					// save to file
					Files.write(pidFilePath, pidLine, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
	            } 
	            catch (Throwable e) 
	            { 
	            	log("Error writing pid process file: " + pidFilePath.toString() + " : " + e.getMessage());
	            }
	            
			}
			catch(Exception e)
			{
				log("Error starting server: " + e.getMessage());
			}
			
			try
			{
				invokeNetMethod = bridgeClass.getMethod("invokeNetMethod", String.class, byte[].class);
			} 
			catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) 
			{
				log("Error obtaining required method invokeNetMethod.", e);
			}
		}
		else
		{
			// Set the Logger
			try 
			{
				Class<?> loggerClass = connectorClassLoader.loadClass("org.mule.api.jni.JniLogger");
				Object jniLogger = loggerClass.getConstructor(org.apache.log4j.Logger.class).newInstance(LOGGER);
				Method setLogger = bridgeClass.getMethod("setLogger", loggerClass);
				Object[] paramLogger = new Object[] { jniLogger };
				setLogger.invoke(bridgeInstance, paramLogger);
			} 
			catch (Exception e) 
			{
				log("Error trying to configure logger in Jni Bridge.", e);
			}
			
			try 
			{
				responseClass = connectorClassLoader.loadClass("org.mule.api.jni.Response");
				getResultMethod = responseClass.getMethod("getResult");
				javaCallbackClass = connectorClassLoader.loadClass("org.mule.api.jni.JavaCallback");
			} 
			catch (Exception e) 
			{
				log("Error obtaining required method from Jni Response", e);
			}
			
			try 
			{
				requestClass = connectorClassLoader.loadClass("org.mule.api.jni.Request");
				setConnectorAssemblyFilePathMethod = requestClass.getMethod("setConnectorAssemblyFilePath", String.class);
				setAssemblyFullyQualifiedNameMethod = requestClass.getMethod("setAssemblyFullyQualifiedName", String.class);
				setTypeNameMethod = requestClass.getMethod("setTypeName", String.class);
				setMethodNameMethod = requestClass.getMethod("setMethodName", String.class);
				setDotNetInstanceReferenceMethod = requestClass.getMethod("setDotNetInstanceReference", Object.class);
				setMethodArgumentsMethod = requestClass.getMethod("setMethodArguments", Map.class);
				setLogMethod = requestClass.getMethod("setLog", boolean.class);
				setFullTrustMethod = requestClass.getMethod("setFullTrust", boolean.class);
			} 
			catch (Exception e) 
			{
				log("Error obtaining required method from Jni Request", e);
			}
			
			try
			{
				invokeNetMethod = bridgeClass.getMethod("invokeNetMethod", requestClass);
			} 
			catch (Exception e) 
			{
				log("Error obtaining required method invokeNetMethod", e);
			}
		}	
		
		// Set the notification manager
//			try {
//				Method setJniNotification = bridgeClass.getMethod("setInstrumentationManager", Object.class);
//				setJniNotification.invoke(bridgeInstance, new Object[] { NOTIFICATION_MANAGER });
//			} catch (Exception e) {
//				log("Error trying to configure intrumentation manager in Jni Bridge.", e);
//			}

		// Initialize the Bridge
		log("Jni Bridge initialization start.");
		boolean shouldRetryToInitBridge = false;
		try 
		{
			initJniBridge(bridgeDllFilePath);
			log("Jni Bridge initialized successfully.");
		} 
		catch (Exception e) 
		{
			if (ExceptionUtils.getDeepestOccurenceOfType(e, UnsatisfiedLinkError.class) != null) {
				log("UnsatisfiedLinkError initializing Jni Bridge. This is most commonnly caused by missing C++ runtime. Will extract the missing libraries and try again.");
				shouldRetryToInitBridge = true;
			} else {
				log("Error initializing Jni Bridge.", e);
			}
		}
		
		if(!isLinux())
		{
			if (shouldRetryToInitBridge) 
			{
				log("Load VC++ Runtime libraries.");
				loadVCRuntime(workingPath);
				
				try 
				{
					initJniBridge(bridgeDllFilePath);
					log("Jni Bridge initialized successfully.");
				} 
				catch (Exception e) 
				{
					log("Error initializing Jni Bridge.", e);
				}
			}
		}
	}

	public static boolean isLinux()
	{
		String os = System.getProperty("os.name");
		
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0 );
	}
	
	protected static void initJniBridge(Path bridgeDllFilePath) throws Exception {
		Method initJniMethod = bridgeClass.getMethod("initJni", String.class);
		initJniMethod.invoke(bridgeInstance, new Object[] { bridgeDllFilePath.toString() } );
	}
	
	public Path getWorkingPath() {
		return workingPath;
	}
	
	private static String getRuntime()
	{
		return System.getProperty("sun.arch.data.model");
	}
	
	private static void loadVCRuntime(Path workingPath) {
		String platformFolderName = "VC" + getPlatformVersion();
		Path vcRuntimeLibrariesPath = workingPath.resolve(platformFolderName);
		log("VC++ libraries path: " + vcRuntimeLibrariesPath);
		
		if (!Files.exists(vcRuntimeLibrariesPath) || !Files.isDirectory(vcRuntimeLibrariesPath)) {
			log("VC++ libraries path does not exist or is not a directory: " + vcRuntimeLibrariesPath);
		}
		
		String[] vcRuntime = { "msvcr120.dll", "msvcp120.dll" };
		for (int i = 0; i < vcRuntime.length; i++) {
			Path runtimeLibraryPath = vcRuntimeLibrariesPath.resolve(vcRuntime[i]);
			if (!Files.exists(runtimeLibraryPath)) {
				log("VC++ library does not exist: " + runtimeLibraryPath);
			}
			
			System.load(runtimeLibraryPath.toString());
		}
	}
	
	private static String getPlatformVersion()
	{
		String platform = System.getProperty("sun.arch.data.model");
		if(platform.equalsIgnoreCase("32"))
		{
			platform = "86";
		}
		
		return platform;
	}
	
	private static void extractResourcesInJar(Path pathToJarFile) throws IOException {
		if(!pathToJarFile.toString().endsWith(".jar") || Files.isDirectory(pathToJarFile)) {
			log("Nothing to extract, this is not a Jar file: " + pathToJarFile);
			return;
		}
		
    	if(!Files.isDirectory(pathToJarFile))  {
    		log(String.format("Extracting from jar file: %s", pathToJarFile.toString()));
    		try (JarFile jarFile = new JarFile(pathToJarFile.toFile())) {
    			Path pathToJar = pathToJarFile.getParent();
	    	    Enumeration<JarEntry> entriesInJar = jarFile.entries();
	    	    while(entriesInJar.hasMoreElements())  {
	    	        JarEntry jarEntry = entriesInJar.nextElement();
	    	        File fileDestination = pathToJar.resolve(jarEntry.getName()).toFile();
	    	        
	    	        if (jarEntry.isDirectory()) {
	    	        	fileDestination.mkdir();
	    	            continue;
	    	        }
	    	        
	    	        if(fileDestination.exists()) {
	    	        	if (isTheSameFile(jarEntry, fileDestination)) {
	    	        		continue;
	    	        	}
	    	        	
	    	        	fileDestination.delete();
	    	        }
	    	        
	        		try (InputStream  input = jarFile.getInputStream(jarEntry);
	        			 FileOutputStream output = new FileOutputStream(fileDestination)) {
	    				while (input.available() > 0) {
	    					output.write(input.read());
	    				}
	        		}
	        		
	        		fileDestination.setLastModified(jarEntry.getTime());
    	            log(String.format("Extracted file: %s", fileDestination.toString()));
	    	    }
    		}
    	}
	}

	private static boolean isTheSameFile(JarEntry jarEntry, File fileDestination) {
		return 
				jarEntry.getTime() == fileDestination.lastModified() &&
				jarEntry.getSize() == fileDestination.length();
	}

	protected static void log(String data)
    {
		log(data, null);
    }
	
	protected static void log(String data, Throwable t) {
    	if(LOGGER.isDebugEnabled())
		{
			LOGGER.debug(data, t);
		}
	}

	protected Object createRequest() throws InstantiationException, IllegalAccessException
	{
		return requestClass.newInstance();
	}
	
	protected Object createDotNetReference(String id) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException
	{
		return dotNetReferenceClass.getDeclaredConstructor(String.class).newInstance(id);
	}
	
	protected String getDotNetReferenceId(Object dotNetReference) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return (String)getDotNetReferenceId.invoke(dotNetReference, new Object[] {});
	}

	public abstract Object getRequest(String assemblyFullyQualifiedName, String typeName, String methodName, Object dotNetInstanceReference, Map<String, Object> arguments, String connectorAssemblyFileName) throws Exception, IllegalArgumentException, InvocationTargetException;
	public abstract Object processRequest(Object request) throws Exception;
	public abstract Object createJavaCallback(Object obj, Method method) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;
	public abstract Object getResult(Object response) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;
}