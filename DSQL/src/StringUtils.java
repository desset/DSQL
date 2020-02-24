import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	//匹配单表中的选择关系
//	public final static Pattern SINGLE_REL_PATTERN = Pattern
//			.compile("(\\w+(?:\\.\\w+)?)\\s?([<=>])\\s?([^\\s\\;\\.]+)");
	public final static Pattern SINGLE_REL_PATTERN = Pattern
			.compile("(\\w+(?:\\.\\w+)?)\\s?([<=>])\\s?([^\\s\\;]+)");
	//匹配多个表的连接关系
	public final static Pattern JOIN_CONNECTION_REL_PATTERN = Pattern
			.compile("(\\w+(?:\\.\\w+)?)\\s?([<=>])\\s?(\\w+\\.\\w+)");
	//创建表解析field字段
	public static Map<String, Field> parseFieldInfo(String properties) {
		Map<String,Field> fieldsMap = new LinkedHashMap<String, Field>();
		String[] fields = properties.trim().split(",");
		for(String field:fields) {
			String[] s=field.trim().split(" ");
			fieldsMap.put(s[0], new Field(s[0],s[1]));
		}
		return fieldsMap;
	}
	//解析where后的过滤语句
	public static List<Map<String,String>> parseWhere(String whereStr){
		String[] whereStrs = whereStr.trim().split("and");
//		for(String s:filterStrs) {
//			System.out.println(s);
//		}
		List<Map<String, String>> filterList = new ArrayList<Map<String,String>>();
		for(String str:whereStrs) {
			Map<String, String> filterMap = new LinkedHashMap<String, String>();
			Matcher matcherWhere=SINGLE_REL_PATTERN.matcher(str.trim());
			if(matcherWhere.find()) {
//				System.out.println("match SINGLE_REL_PATTERN......");
				filterMap.put("field", matcherWhere.group(1));
				filterMap.put("relationship", matcherWhere.group(2));
				filterMap.put("condition", matcherWhere.group(3));
			}	
			filterList.add(filterMap);
		}
		return filterList;
	}

}
