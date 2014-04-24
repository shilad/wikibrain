package org.wikibrain.wikidata;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Shilad Sen
 */
public class WikidataValue implements Serializable {

    public enum Type {ITEM, TIME, STRING, INT, NOVALUE, SOMEVALUE, OTHER};

    private Type type;
    private String typeName;
    private Object value;
    private JsonElement jsonValue;

    public WikidataValue(String typeName, Object value, JsonElement jsonValue) {
        for (Type t : Type.values()) {
            if (t.toString().toLowerCase().equals(typeName.toLowerCase())) {
                this.type = t;
                this.typeName = t.toString();
            }
        }
        if (this.type == null) {
            this.type = Type.OTHER;
            this.typeName = typeName;
        }
        this.value = value;
        this.jsonValue = jsonValue;
    }

    public WikidataValue(Type type, Object value, JsonElement jsonValue) {
        this.type = type;
        this.typeName =  type.toString();
        this.value = value;
        this.jsonValue = jsonValue;
    }


    private void writeObject(ObjectOutputStream o)
            throws IOException {
        o.writeObject(type);
        o.writeObject(typeName);
        o.writeObject(value);
        o.writeObject(jsonValue.toString());
    }

    private void readObject(ObjectInputStream o)
            throws IOException, ClassNotFoundException {
        type = (Type) o.readObject();
        typeName = (String) o.readObject();
        value = o.readObject();
        jsonValue = new JsonParser().parse((String)o.readObject());
    }

    public Type getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String getTypeName() {
        return typeName;
    }

    public Date getTimeValue() {
        return (Date)value;
    }

    public String getStringValue() {
        return value.toString();
    }

    public int getItemValue() {
        return (Integer)value;
    }

    public int getIntValue() {
        return (Integer)value;
    }

    public JsonElement getJsonValue() {
        return jsonValue;
    }

    @Override
    public String toString() {
        return "WikidataValue{" +
                "typeName='" + typeName + '\'' +
                ", value=" + value +
                '}';
    }
}
