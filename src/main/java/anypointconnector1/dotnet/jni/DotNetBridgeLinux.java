package anypointconnector1.dotnet.jni;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

public class DotNetBridgeLinux extends BaseDotNetBridge {
	
	@Override
	public Object getRequest(String assemblyFullyQualifiedName,
			String typeName, String methodName, Object dotNetInstanceReference,
			Map<String, Object> arguments, String connectorAssemblyFileName)
			throws Exception, IllegalArgumentException,
			InvocationTargetException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
        JsonGenerator jsonGenerator = new JsonFactory().createJsonGenerator(output);
        //for pretty printing
        jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter());

        jsonGenerator.writeStartObject(); // start root object

        jsonGenerator.writeStringField("assemblyFullyQualifiedName", assemblyFullyQualifiedName);
        jsonGenerator.writeStringField("connectorAssemblyFilePath", getWorkingPath().resolve(connectorAssemblyFileName).toString());
        jsonGenerator.writeStringField("typeName", typeName);
        jsonGenerator.writeStringField("methodName", methodName);
        jsonGenerator.writeBooleanField("log", true);
        
        String referenceId = null;
        
        if(dotNetInstanceReference != null)
        {
	        String className = dotNetInstanceReference.getClass().getName();
	        
	        if(className.contains("DotNetInstanceReference"))
	        {
        		referenceId = getDotNetReferenceId(dotNetInstanceReference);
	        }
	        else
	        {
	        	referenceId = dotNetInstanceReference.toString();
	        }

        }
        
        jsonGenerator.writeStringField("dotNetInstanceReference", referenceId);
        jsonGenerator.writeBooleanField("fulltrust", true);

        if (arguments != null) {
            jsonGenerator.writeFieldName("methodArguments");
            ToJsonObject(jsonGenerator, arguments);
        }

        jsonGenerator.writeEndObject(); //closing root object

        jsonGenerator.flush();
        jsonGenerator.close();

        byte[] data = output.toByteArray();
		return data;
	}

	@Override
	public Object processRequest(Object request) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Object val = invokeNetMethod.invoke(bridgeInstance, new Object[] { request } );
		return val;
	}

	@Override
	public Object createJavaCallback(Object obj, Method method)
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getResult(Object payload) {
		ObjectMapper mapper = new ObjectMapper();
		
		JniResponse response;
		try 
		{
			response = mapper.readValue(payload.toString(), JniResponse.class);

			if(response.failed())
			{
				throw new Exception(response.getException());
			}
			
			if(response.getPayload() != null) 
			{
				switch(response.getPayload().getJni_Type())
				{
					//case "NULL":
					//	return msg.getOriginalPayload();
					case "DotNetInstanceReference":
						return createDotNetReference(response.getPayload().getJni_Value().toString());
					case "java.lang.String":
						String dotNetResult = response.getPayload().getJni_Value().toString();
//						String contentType = "text/xml";
//						if(isStringJsonRepresentation(dotNetResult))
//						{
//							contentType = "application/json";
//						}
						
						//msg.setProperty("Content-Type", contentType, PropertyScope.OUTBOUND);
						
						return dotNetResult;
					case "[B":
						ArrayList data = (ArrayList)response.getPayload().getJni_Value();
						byte[] returnValue = new byte[data.size()];
						
						for(int i=0; i<data.size(); i++)
						{
							returnValue[i] = ((Integer)data.get(i)).byteValue();
						}
						
						return returnValue;
		
					default:
						return response;
						//return msg.getOriginalPayload();
				}
			}
			else
			{
				return null;
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
//	private static boolean isStringJsonRepresentation(String data) {
//		String payloadContent = data.trim();
//		
//		return (payloadContent.startsWith("[") && payloadContent.endsWith("]")) ||
//				payloadContent.startsWith("{") && payloadContent.endsWith("}");
//	}

	private void ToJsonObject(JsonGenerator writer, Object obj) throws IOException, IllegalAccessException, InvocationTargetException {

        writer.writeStartObject();

        String className = obj.getClass().getName();
        
        if(className.contains("DotNetInstanceReference"))
        {
        	String referenceId = getDotNetReferenceId(obj);
        	writer.writeStringField("Jni_Type", "DotNetInstanceReference");
        	writer.writeStringField("Jni_Value", referenceId);
        }

        if (className.equalsIgnoreCase("java.lang.Integer")) {
            writer.writeStringField("Jni_Type", "Int32");
            writer.writeNumberField("Jni_Value", (int) obj);
        }

        if (className.equalsIgnoreCase("java.lang.String")) {
            writer.writeStringField("Jni_Type", "String");
            writer.writeStringField("Jni_Value", obj.toString());
        }

        if (className.equalsIgnoreCase("java.lang.Boolean")) {
            writer.writeStringField("Jni_Type", "Boolean");
            writer.writeBooleanField("Jni_Value", (boolean) obj);
        }

        if (className.equalsIgnoreCase("java.lang.Character")) {
            writer.writeStringField("Jni_Type", "Char");
            writer.writeFieldName("Jni_Value");
            writer.writeString(obj.toString());
        }

        if (className.equalsIgnoreCase("java.lang.Long")) {
            writer.writeStringField("Jni_Type", "Int64");
            writer.writeNumberField("Jni_Value", (long) obj);
        }

        if (className.equalsIgnoreCase("java.lang.Short")) {
            writer.writeStringField("Jni_Type", "Int16");
            writer.writeNumberField("Jni_Value", (short) obj);
        }

        if (className == "java.lang.Byte") {
            writer.writeStringField("Jni_Type", "Byte");
            writer.writeBinaryField("Jni_Value", new byte[]{(byte) obj});
        }

        if (className.equalsIgnoreCase("java.lang.Double")) {
            writer.writeStringField("Jni_Type", "Double");
            writer.writeNumberField("Jni_Value", (double) obj);
        }

        if (className.equalsIgnoreCase("java.lang.Float")) {
            writer.writeStringField("Jni_Type", "Single");
            writer.writeNumberField("Jni_Value", (float) obj);
        }

        if (className.equalsIgnoreCase("[I")) {
            writer.writeStringField("Jni_Type", "Int32[]");
            writer.writeFieldName("Jni_Value");

            int[] value = (int[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[B")) {
            writer.writeStringField("Jni_Type", "Byte[]");
            writer.writeFieldName("Jni_Value");
            writer.writeBinary((byte[]) obj);
        }

        if (className.equalsIgnoreCase("[C")) {
            writer.writeStringField("Jni_Type", "Char[]");
            writer.writeFieldName("Jni_Value");
            writer.writeString(new String((char[]) obj));
        }

        if (className.equalsIgnoreCase("[D")) {
            writer.writeStringField("Jni_Type", "Double[]");
            writer.writeFieldName("Jni_Value");

            double[] value = (double[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[Z")) {
            writer.writeStringField("Jni_Type", "Bool[]");
            writer.writeFieldName("Jni_Value");

            boolean[] value = (boolean[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeBoolean(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[S")) {
            writer.writeStringField("Jni_Type", "Short[]");
            writer.writeFieldName("Jni_Value");

            short[] value = (short[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[J")) {
            writer.writeStringField("Jni_Type", "Long[]");
            writer.writeFieldName("Jni_Value");

            long[] value = (long[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[F")) {
            writer.writeStringField("Jni_Type", "Float[]");
            writer.writeFieldName("Jni_Value");

            float[] value = (float[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeNumber(value[i]);
            }

            writer.writeEndArray();
        }

        if (className.equalsIgnoreCase("[Ljava.lang.String;")) {
            writer.writeStringField("Jni_Type", "String[]");
            writer.writeFieldName("Jni_Value");

            String[] value = (String[]) obj;

            int vectorSize = value.length;

            writer.writeStartArray();

            for (int i = 0; i < vectorSize; i++) {
                writer.writeString(value[i]);
            }

            writer.writeEndArray();
        }

        if (obj instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                writer.writeFieldName(entry.getKey());

                boolean isMap = entry.getValue() instanceof Map<?, ?>;

                /* check if it's a map to construct json object*/
                if (isMap) {
                    writer.writeStartObject();
                    writer.writeFieldName("Jni_Type");
                    writer.writeString("Map");
                    writer.writeFieldName("Jni_Value");
                }

                ToJsonObject(writer, entry.getValue());

                if (isMap) {
                    writer.writeEndObject();
                }
            }
        }

        writer.writeEndObject();
    }

}