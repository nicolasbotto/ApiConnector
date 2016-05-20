package anypointconnector1.dotnet;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.mule.api.callback.*;

public class JavaSourceCallback extends DotNetObject implements StopSourceCallback {
	private SourceCallback sourceCallback;
	private Object javaCallback;

		public JavaSourceCallback(SourceCallback sourceCallback) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		super("Org.Mule.Api.Devkit, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "Org.Mule.Api.Devkit.Callback.SourceCallback");

		this.sourceCallback = sourceCallback;

		java.lang.reflect.Method method = this.getClass().getDeclaredMethod("SourceMethodCallback", Object.class);
		this.javaCallback = DotNetInvoker.getInstance().createJavaCallback(this, method);

		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("onJavaCallback", this.javaCallback);
		this.callDotNetConstructor(".ctor(System.Action`1[System.Object] onJavaCallback)", arguments);
	}
	
	public void SourceMethodCallback(Object executeResult) throws Exception {
		if (executeResult != null) {
			this.sourceCallback.process(executeResult.toString());
		}
	}

	@Override
	public void stop() throws Exception {
		Map<String, Object> arguments = new HashMap<String, Object>();
		this.callMethod("Stop()", arguments);
	}
}