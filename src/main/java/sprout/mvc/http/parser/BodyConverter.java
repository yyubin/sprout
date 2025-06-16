package sprout.mvc.http.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.exception.BadRequestException;
import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.ResponseCode;

import java.util.Map;

@Component
public class BodyConverter {
    private final ObjectMapper om;
    public BodyConverter(ObjectMapper om) { this.om = om; }
    public Map<String,Object> toMap(String body, HttpMethod method) {
        if (body == null || body.isBlank()) return null;
        try {
            return om.readValue(body.trim(), new TypeReference<Map<String,Object>>(){});
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse JSON body", ResponseCode.BAD_REQUEST);
        }
    }
}