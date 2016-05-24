
package anypointconnector1;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.annotations.*;
import org.mule.api.callback.*;
import org.mule.api.annotations.components.*;
import org.mule.api.annotations.display.*;
import org.mule.api.annotations.param.*;
import org.mule.api.annotations.licensing.RequiresEnterpriseLicense;

import anypointconnector1.config.BaseConnectorConfig;
import anypointconnector1.dotnet.*;
import anypointconnector1.exceptions.ApiConnectorException;

@Connector(name="api", friendlyName="Api Connector", minMuleVersion="3.5.0")
public class ApiConnector extends DotNetObject {

    @Config private BaseConnectorConfig config;
	
    public ApiConnector() {
		super("AnyPointConnector1, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "AnyPointConnector1.ApiConnector");

		Map<String, Object> arguments = new HashMap<String, Object>();
    	this.callDotNetConstructor(".ctor()", arguments);
    }

	
	@Processor()
	public String SendMessage(String payload) throws ApiConnectorException {
		String methodName = "SendMessage(System.String payload)";
		
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("payload", payload);
		try {
			Object executeResult = this.callMethod(methodName, arguments);
			return executeResult.toString();
		} catch (Exception e) {
			throw new ApiConnectorException(e);
		}
	}
	
	
	@Processor()
	public void MarkCompleted(String message) throws ApiConnectorException {
		String methodName = "MarkCompleted(System.String message)";
		
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("message", message);
		try {
			this.callMethod(methodName, arguments);
		} catch (Exception e) {
			throw new ApiConnectorException(e);
		}
	}
	
	
	
	private JavaSourceCallback SourceMethodJavaCallback;

	@Source(threadingModel=SourceThreadingModel.NONE, sourceStrategy=SourceStrategy.NONE)
	public StopSourceCallback SourceMethod(SourceCallback callback) throws ApiConnectorException {
		String methodName = "SourceMethod(Org.Mule.Api.Devkit.Callback.SourceCallback callback)";
		


		Map<String, Object> arguments = new HashMap<String, Object>();
		try {
			this.SourceMethodJavaCallback = new JavaSourceCallback(callback); 
			arguments.put("callback", this.SourceMethodJavaCallback.getDotNetInstanceReference());

			this.callMethod(methodName, arguments);
			return this.SourceMethodJavaCallback;
		} catch (Exception e) {
			throw new ApiConnectorException(e);
		}
	}
	

    public BaseConnectorConfig getConfig() {
        return config;
    }

    public void setConfig(BaseConnectorConfig config) {
        this.config = config;
        
		String methodName = "set_Config(AnyPointConnector1.Config.BaseConfig value)";

        Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("value", config.getDotNetInstanceReference());
		this.callMethod(methodName, arguments);
    }
}