package io.github.heisenberguwu.myrocketmq.attribute;

import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.Map;

public class AttributeParser {
    public static final String ATTR_ARRAY_SEPARATOR_COMMA = ",";

    public static final String ATTR_KEY_VALUE_EQUAL_SIGN = "=";

    public static final String ATTR_ADD_PLUS_SIGN = "+";

    public static final String ATTR_DELETE_MINUS_SIGN = "-";

    public static Map<String,String> parseToMap(String attributesModification)
    {
        if(Strings.isNullOrEmpty(attributesModification))
        {
            return new HashMap<>();
        }


    }
}
