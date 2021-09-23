package com.mantikafasi.plugins;
import java.lang.reflect.Field;

public class ClassUtils {
    Object classObj;
    public ClassUtils(Object classObj){

       this.classObj = classObj;

    }
    public Object getPrivateField(String fieldName)
    {
        try {
            Field f = classObj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(classObj);
        } catch (Exception e){return e.toString();}
    }

}
