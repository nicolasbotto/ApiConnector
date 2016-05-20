package anypointconnector1.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.*;
import org.mule.api.annotations.components.*;
import org.mule.api.annotations.display.*;
import org.mule.api.annotations.param.*;

import anypointconnector1.config.BaseConnectorConfig;
import anypointconnector1.exceptions.ApiConnectorRuntimeException;

@ConnectionManagement(friendlyName="Configuration for Api Connector")
public class ApiConnectorConfig extends BaseConnectorConfig {

    private static final Log log = LogFactory.getLog(ApiConnectorConfig.class);

	public ApiConnectorConfig() {
		super("AnyPointConnector1, Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", "AnyPointConnector1.Config.ApiConnectorConfig");

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
	

    @Connect
    public void connect(@ConnectionKey String title, @Password String password)
            throws ConnectionException {
		String methodName = "Connect(System.String title, System.String password)";
		
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("title", title);
		arguments.put("password", password);
		try {
			this.callMethod(methodName, arguments);
		} catch (Exception e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, "", e.getMessage(), e);
		}
    }
    
    @TestConnectivity
    public void testConnectivity(@ConnectionKey String title, @Password String password)
            throws ConnectionException {
		String methodName = "Connect(System.String title, System.String password)";
		
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("title", title);
		arguments.put("password", password);
		try {
			this.callMethod(methodName, arguments);
		} catch (Exception e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, "", e.getMessage(), e);
		}
    }
    
    @Disconnect
    public void disconnect() {
		String methodName = "Disconnect()";
		
		Map<String, Object> arguments = new HashMap<String, Object>();
		try {
			this.callMethod(methodName, arguments);
		} catch (Exception e) {
			log.debug("Error during @Disconnect.", e);
		}
    }
    
    @ConnectionIdentifier
    public String getConnectionIdentifier() {
		String methodName = "GetConnectionIdentifier()";
		
		Map<String, Object> arguments = new HashMap<String, Object>();
		Object executeResult = this.callMethod(methodName, arguments);
		return executeResult.toString();
    }

    @ValidateConnection
    public boolean validateConnection() {
		String methodName = "IsConnected()";
		
		Map<String, Object> arguments = new HashMap<String, Object>();
		Object executeResult = this.callMethod(methodName, arguments);
		return Boolean.parseBoolean(executeResult.toString());
    }
}