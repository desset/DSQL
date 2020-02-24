import java.util.HashMap;
import java.util.Map;

public class Field {
	String name;
	String type;
	public static Map<String, Class> type_map=new HashMap<String, Class>();
	static {
		type_map.put("int",Integer.class);
		type_map.put("double", Double.class);
		type_map.put("varchar",String.class);
	}
	public Field(String name, String type) {
		super();
		this.name = name;
		this.type = type;
	}
	@Override
	public String toString() {
		return "Field [name=" + name + ", type=" + type + "]";
	}

}
