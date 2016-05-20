package anypointconnector1.config;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.annotations.*;
import org.mule.api.annotations.components.*;
import org.mule.api.annotations.display.*;
import org.mule.api.annotations.param.*;

import anypointconnector1.config.BaseConnectorConfig;
import anypointconnector1.exceptions.ApiConnectorRuntimeException;

@Configuration(configElementName="configuration2", friendlyName="Configuration 2 for Api Connector")
public class ApiConnectorConfig2 extends BaseConnectorConfig {

	public ApiConnectorConfig2() {
		super("AnyPointConnector1, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "AnyPointConnector1.Config.ApiConnectorConfig2");

		Map<String, Object> arguments = new HashMap<String, Object>();
    	this.callDotNetConstructor(".ctor()", arguments);
	}
	
	
	@Configurable
	@FriendlyName("Prefix")
	private String prefix;

	public String getPrefix() {
		String methodName = "get_Prefix()";

		Map<String, Object> arguments = new HashMap<String, Object>();
		try {
			Object executeResult = this.callMethod(methodName, arguments);
			return executeResult.toString();
		} catch (Exception e) {
			throw new ApiConnectorRuntimeException(e);
		}
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;

		String methodName = "set_Prefix(System.String value)";

		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("value", prefix);

		try {
			this.callMethod(methodName, arguments);
		} catch (Exception e) {
			throw new ApiConnectorRuntimeException(e);
		}
	}
	
	
	@Configurable
	@Default("true")
	private boolean addPrefix;

	public boolean getAddPrefix() {
		String methodName = "get_AddPrefix()";

		Map<String, Object> arguments = new HashMap<String, Object>();
		try {
			Object executeResult = this.callMethod(methodName, arguments);
			return new org.codehaus.jackson.map.ObjectMapper().readValue(executeResult.toString(), boolean.class);
		} catch (Exception e) {
			throw new ApiConnectorRuntimeException(e);
		}
	}
	
	public void setAddPrefix(boolean addPrefix) {
		this.addPrefix = addPrefix;

		String methodName = "set_AddPrefix(System.Boolean value)";

		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("value", addPrefix);

		try {
			this.callMethod(methodName, arguments);
		} catch (Exception e) {
			throw new ApiConnectorRuntimeException(e);
		}
	}
	
}