package anypointconnector1.config;

import anypointconnector1.dotnet.DotNetObject;

public abstract class BaseConnectorConfig extends DotNetObject {

	public BaseConnectorConfig(String assemblyFullyQualifiedName, String typeName) {
		super(assemblyFullyQualifiedName, typeName);
	}
}