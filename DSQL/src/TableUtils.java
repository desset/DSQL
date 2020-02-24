import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class TableUtils {
	public static void createTable(String name, Map<String, Field> fieldMap) {
//		parentPath=new File("/db1");
//		parentPath.mkdirs();
		File folder = new File("dir" + "/" + "db1" + "/" + name);
		File dataFolder = new File("dir" + "/" + "db1" + "/" + name + "/" + "data");
		folder.mkdirs();
		dataFolder.mkdirs();
		File dictFile = new File(folder, name + ".dict");
		try (FileWriter fw = new FileWriter(dictFile, true); PrintWriter pw = new PrintWriter(fw);) {
			for (Map.Entry<String, Field> field : fieldMap.entrySet()) {
				// 判断field是否合法
				if (!Field.type_map.keySet().contains(field.getValue().type)) {
					System.out.println("no type named " + field.getValue().type);
					return;
				}
				// 写入.dict文件
				pw.println(field.getKey() + " " + field.getValue().type + " " + "^");

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static Table getTable(String tableName) {
		Table table = new Table();
		table.name = tableName;
		table.parentPath = new File("dir" + "/" + "db1");
		table.tableFolder = new File(table.parentPath, tableName);
		if (!table.tableFolder.exists()) {
//			System.out.println("Table named " + tableName + " not exists!");
			return null;
		}
		// 读取表的字段信息
		table.dictFile = new File(table.tableFolder, tableName + ".dict");
		table.fieldMap = new LinkedHashMap<String, Field>();
		try (FileReader fr = new FileReader(table.dictFile); BufferedReader br = new BufferedReader(fr);) {
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line == null || line.length() == 0) {
					continue;
				}
				String[] properties = line.trim().split(" ");
				table.fieldMap.put(properties[0], new Field(properties[0], properties[1]));
//				System.out.println(properties[0]+" "+properties[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 读取表中的数据
		File dataFolder = new File("dir" + "/" + "db1" + "/" + tableName + "/" + "data");
		table.fileSet = new LinkedHashSet<File>();
		table.data = new ArrayList<Map<String, String>>();
		File[] fileSet = dataFolder.listFiles();
		if (fileSet != null && fileSet.length != 0) {
			for (File file : fileSet) {
				table.fileSet.add(file);
				try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr);) {
					String line = null;
					// 使用br.readLine()!= null判断末尾时会多读一行，即会将换行后的空行读入
					while ((line = br.readLine()) != null) {
						// 使用br.readLine()!= null判断末尾时可能会多读一行，即会将换行后的空行读入
						// 需要判断一下跳过空行
						if (line == null || line.length() == 0) {
							continue;
						}
						Map<String, String> dataLine = new LinkedHashMap<String, String>();
						// 技巧，Map中没有序号索引，可以创建其key的迭代器来实现遍历
						Iterator<String> fieldName = table.fieldMap.keySet().iterator();
						String[] datas = line.trim().split(" ");
						for (String data : datas) {
							if (!data.equals("^")) {
								String dataName = fieldName.next();
								dataLine.put(dataName, data);
							}

						}
						table.data.add(dataLine);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
//			System.out.println(table.data.get(2).get("age"));
		}
		return table;
	}

	public static void insert(Table table, Map<String, String> insertData) {
		Map<String, String> data = new LinkedHashMap<String, String>();
		// 检查字段长度
		if (insertData.size() > table.fieldMap.size()) {
			System.out.println("Invalid format:too much fields");
			return;
		}
		// 检查类型
		for (Map.Entry<String, String> column : insertData.entrySet()) {
			// 若table中的字段集合中不包含插入的字段，报错
			if (!table.fieldMap.keySet().contains(column.getKey())) {
				System.out.println("Invalid format:no such field :" + column.getKey());
				return;
			}
			// 加上类型检查
			switch (table.fieldMap.get(column.getKey()).type) {
			case "int":
				if (!column.getValue().matches("^(-|\\+)?\\d+$")) {
					System.out.println("Invalid format:" + column.getValue() + " type error");
					return;
				}
				break;
			case "double":
				if (!column.getValue().matches("^(-|\\+)?\\d*\\.?\\d+$")) {
					System.out.println("Invalid format:" + column.getValue() + " type error");
					return;
				}
				break;
			case "varchar":
				break;
			default:
				System.out.println("Invalid format:no such field type :" + table.fieldMap.get(column.getKey()).type);
				return;
			}
		}

		// 为没有赋值的字段填充[NULL]
		for (Map.Entry<String, Field> field : table.fieldMap.entrySet()) {
			if (insertData.get(field.getKey()) != null) {
				data.put(field.getKey(), insertData.get(field.getKey()));
			} else {
				data.put(field.getKey(), "[NULL]");
			}
		}
//		for(Map.Entry<String, String> col:data.entrySet()) {
//			System.out.println(col.getKey()+" "+col.getValue());
//		}
		File dataFolder = new File(table.tableFolder, "data");
		File lastFile = new File(dataFolder, table.fileSet.size() + ".data");
		if (table.fileSet.size() == 0) {
			lastFile = new File(dataFolder, "1.data");
		} else if (getLineNum(lastFile) >= table.Max_LineNum) {
			lastFile = new File(dataFolder, (table.fileSet.size() + 1) + ".data");
		}
		try (FileWriter fw = new FileWriter(lastFile, true); PrintWriter pw = new PrintWriter(fw);) {
			for (Map.Entry<String, String> dataColumn : data.entrySet()) {
				pw.print(dataColumn.getValue() + " ");
			}
			pw.println("^");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int getLineNum(File lastFile) {
		int num = 0;
		try (FileReader fr = new FileReader(lastFile); BufferedReader br = new BufferedReader(fr);) {
			while (br.readLine() != null) {
				num++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return num;
	}

	public static void delete(Table table, List<Filter> filterList) {
		// 打印table中所有数据
		for (Map<String, String> d : table.data) {
			for (Map.Entry<String, String> e : d.entrySet()) {
				System.out.println(e.getKey() + " " + e.getValue());
			}
		}
		List<Map<String, String>> filterData = dataFilter(table, filterList);
		System.out.println("filterData.size: " + filterData.size());
		table.data.removeAll(filterData);
//		System.out.println(table.data.size());
		writeData(table);
	}

	// 重写所有数据(只有数据即table.data发生变化)
	private static void writeData(Table table) {
		// 删除所有的.data文件，并清空table.fileSet
		for (File file : table.fileSet) {
			file.delete();
		}
		table.fileSet.clear();
		if (table.data != null || table.data.size() != 0) {
			// 获取data文件夹并逐条插入数据
			File dataFolder = new File(table.tableFolder, "data");
			for (Map<String, String> data : table.data) {
				File[] fileSet = dataFolder.listFiles();
				if (fileSet != null && fileSet.length != 0) {
					for (File file : fileSet) {
						table.fileSet.add(file);
					}
				}
				File lastFile = new File(dataFolder, table.fileSet.size() + ".data");
				if (table.fileSet.size() == 0 || table.fileSet == null) {
					lastFile = new File(dataFolder, "1.data");
				} else if (getLineNum(lastFile) >= table.Max_LineNum) {
					lastFile = new File(dataFolder, (table.fileSet.size() + 1) + ".data");
				}
				try (FileWriter fw = new FileWriter(lastFile, true); PrintWriter pw = new PrintWriter(fw);) {
					for (Map.Entry<String, String> dataColumn : data.entrySet()) {
						pw.print(dataColumn.getValue() + " ");
					}
					pw.println("^");

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return;
	}

	public static List<Map<String, String>> dataFilter(Table table, List<Filter> filterList) {
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		boolean flag = false;
		for (Map<String, String> srcData : table.data) {
			flag = true;
			for (Filter filter : filterList) {
				if (matchCondition(srcData, filter) == false) {
					flag = false;
					break;
				}
			}
			if (flag == true) {
				data.add(srcData);
			}
		}

		return data;
	}

	private static boolean matchCondition(Map<String, String> srcData, Filter filter) {
		String dataValue = srcData.get(filter.field.name);
		Class typeClass = Field.type_map.get(filter.field.type);
//		System.out.println(typeClass.getName());
		// 如果要和过滤器比较的字段为空，返回不匹配
		// String类型比较时不能用==而应该用.equals()
		// 由于类型为int和double的数据值也可能为[NULL]，所以需要单独处理
		if ((dataValue.equals("[NULL]")) != (filter.condition.equals("[NULL]"))) {
			return false;
		}
		if (dataValue.equals("[NULL]") && filter.condition.equals("[NULL]")) {
			return true;
		}
		if (typeClass == null) {
			System.err.println("type error!");
			return false;
		}
		try {
			Method method_CompareTo = typeClass.getMethod("compareTo", typeClass);
			Constructor constructor = typeClass.getDeclaredConstructor(String.class);
			Object data = constructor.newInstance(dataValue);
			Object condition = constructor.newInstance(filter.condition);
			Integer result = (Integer) method_CompareTo.invoke(data, condition);
			return compareResult(filter.relationship, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private static boolean compareResult(Relationship relationship, Integer result) {
		switch (relationship) {
		case LESS_THAN:
			if (result < 0) {
				return true;
			} else {
				return false;
			}
		case MORE_THAN:
			if (result > 0) {
				return true;
			} else {
				return false;
			}
		case EQUAL_TO:
			if (result == 0) {
				return true;
			} else {
				return false;
			}
		default:
			try {
				throw new Exception("no such relationship " + relationship);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void main(String[] args) {
		Table t1 = getTable("tab");
//		Table t2=getTable("tab1");
//		System.out.println(t1.dictFile);
//		System.out.println(t2.dictFile);
	}

	public static void update(Table table, List<Filter> filterList, Map<String, String> setData) {
		for (Map<String, String> d : table.data) {
			for (Map.Entry<String, String> e : d.entrySet()) {
				System.out.println(e.getKey() + " " + e.getValue());
			}
		}
		List<Map<String, String>> filterData = dataFilter(table, filterList);
		System.out.println("filterData.size: " + filterData.size());

		table.data.removeAll(filterData);
		for (Map.Entry<String, String> column : setData.entrySet()) {
			// 判断set语句field的合法性，即table.fieldMap中是否包含set语句中的field
			if (table.fieldMap.get(column.getKey()) == null) {
				System.out.println("no field named " + column.getKey());
				return;
			}
			// 加上类型检查
			if (!column.getValue().equals("[NULL]")) {
				switch (table.fieldMap.get(column.getKey()).type) {
				case "int":
					if (!column.getValue().matches("^(-|\\+)?\\d+$")) {
						System.out.println("Invalid format:" + column.getValue() + " type error");
						return;
					}
					break;
				case "double":
					if (!column.getValue().matches("^(-|\\+)?\\d*\\.?\\d+$")) {
						System.out.println("Invalid format:" + column.getValue() + " type error");
						return;
					}
					break;
				case "varchar":
					break;
				default:
					System.out
							.println("Invalid format:no such field type :" + table.fieldMap.get(column.getKey()).type);
					return;
				}
			}
			// Map是多对一的，一个键只能对应一个值，但多个键可对应同一个值
			for (Map<String, String> data : filterData) {
				data.put(column.getKey(), column.getValue());
			}
		}
		table.data.addAll(filterData);
		writeData(table);
	}

	public static void alterTableAdd(Table table, Map<String, Field> fieldMap) {
		try (FileWriter fw = new FileWriter(table.dictFile, true); PrintWriter pw = new PrintWriter(fw);) {
			for (Map.Entry<String, Field> field : fieldMap.entrySet()) {
				// 判断field的类型是否合法
				if (!Field.type_map.keySet().contains(field.getValue().type)) {
					System.out.println("no type named " + field.getValue().type);
					return;
				}
				// 判断.dict是否已经包含这个字段，如果已经包含，报错
				if (table.fieldMap.keySet().contains(field.getKey())) {
					System.out.println("field named " + field.getKey() + " has been existed.");
					return;
				}
				// 写入.dict文件
				pw.println(field.getKey() + " " + field.getValue().type + " " + "^");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static List<Map<String, String>> selfConFilter(List<Map<String, String>> srcData, List<JoinCondition> list) {
		List<Map<String, String>> dataList = new ArrayList<Map<String, String>>();
		boolean flag = true;
		for (Map<String, String> data : srcData) {
			flag = true;
			for (JoinCondition joinCondition : list) {
				switch (joinCondition.relationship) {
				case "=":
					if (!data.get(joinCondition.field1).equals(data.get(joinCondition.field2))) {
						flag = false;
						continue;
					}
					break;
				case ">":
					if (!(data.get(joinCondition.field1).compareTo(data.get(joinCondition.field2)) > 0)) {
						flag = false;
						continue;
					}
					break;
				case "<":
					if (!(data.get(joinCondition.field1).compareTo(data.get(joinCondition.field2)) < 0)) {
						flag = false;
						continue;
					}
					break;
				default:
					System.out.println("no relatonship named " + joinCondition.relationship);
					return null;
				}
			}
			if (flag == true) {
				dataList.add(data);
			}
		}
//		System.out.println(dataList.size());
		return dataList;

	}

	public static Table doubleConFilter(Table table1, Table table2, JoinCondition joinCondition) {
		System.out.println("TableUtils.doubleConFilter......");
		if (table1 == null || table2 == null) {
			System.out.println("TableUtils.doubleConFilter:null table");
			return null;
		}
		Table table = new Table();
		table.fieldMap = new LinkedHashMap<String, Field>();
		table.data = new ArrayList<Map<String, String>>();
		Map<String, Field> temFieldMap = new LinkedHashMap<String, Field>();
//		System.out.println(table1.name);

		if (table1.dictFile != null) {
//			System.out.println(table1.name);
			for (Map.Entry<String, Field> map : table1.fieldMap.entrySet()) {
				temFieldMap.put(table1.name + "." + map.getKey(), map.getValue());
			}
		} else {
			for (Map.Entry<String, Field> map : table1.fieldMap.entrySet()) {
				temFieldMap.put(map.getKey(), map.getValue());
			}
		}
		if (table2.dictFile != null) {
			for (Map.Entry<String, Field> map : table2.fieldMap.entrySet()) {
				temFieldMap.put(table2.name + "." + map.getKey(), map.getValue());
			}
		} else {
			for (Map.Entry<String, Field> map : table2.fieldMap.entrySet()) {
				temFieldMap.put(map.getKey(), map.getValue());
			}
		}
		table.fieldMap.putAll(temFieldMap);
//		System.out.println(table.fieldMap.size());
//		for (String s : table.fieldMap.keySet()) {
//			System.out.println(s);
//		}

		if (table1.dictFile == null) {
			joinCondition.field1 = joinCondition.table1 + "." + joinCondition.field1;
		}
		if (table2.dictFile == null) {
			joinCondition.field2 = joinCondition.table2 + "." + joinCondition.field2;
		}
		for (Map<String, String> data1 : table1.data) {
			for (Map<String, String> data2 : table2.data) {
				// 用于存放临时数据，之后将其复制进data
				Map<String, String> temData = new LinkedHashMap<String, String>();
				switch (joinCondition.relationship) {
				case "=":
//					System.out.println(table1.data.size());
//					for (Map<String, String> m : table1.data) {
//						for (String s : m.keySet()) {
//							System.out.println(s + " ");
//						}
//					}
//					System.out.println(data1.get(joinCondition.field1));
//					System.out.println(data2.get(joinCondition.field2));

					if (data1.get(joinCondition.field1).equals(data2.get(joinCondition.field2))) {
						if (table1.dictFile != null) {
							for (Map.Entry<String, String> map : data1.entrySet()) {
								temData.put(table1.name + "." + map.getKey(), map.getValue());
							}
						} else {
							for (Map.Entry<String, String> map : data1.entrySet()) {
								temData.put(map.getKey(), map.getValue());
							}
						}
						if (table2.dictFile != null) {
							for (Map.Entry<String, String> map : data2.entrySet()) {
								temData.put(table2.name + "." + map.getKey(), map.getValue());
							}
						} else {
							for (Map.Entry<String, String> map : data2.entrySet()) {
								temData.put(map.getKey(), map.getValue());
							}
						}
						table.data.add(temData);
//						for(String string:temData.values()) {
//							System.out.print(string);
//						}
//						System.out.println();
					}
					break;
				case ">":
					if (data1.get(joinCondition.field1).compareTo(data2.get(joinCondition.field2)) > 0) {
						if (table1.dictFile != null) {
							for (Map.Entry<String, String> map : data1.entrySet()) {
								temData.put(table1.name + "." + map.getKey(), map.getValue());
							}
						} else {
							for (Map.Entry<String, String> map : data1.entrySet()) {
								temData.put(map.getKey(), map.getValue());
							}
						}
						if (table2.dictFile != null) {
							for (Map.Entry<String, String> map : data2.entrySet()) {
								temData.put(table2.name + "." + map.getKey(), map.getValue());
							}
						} else {
							for (Map.Entry<String, String> map : data2.entrySet()) {
								temData.put(map.getKey(), map.getValue());
							}
						}
					}
					break;
				case "<":
					if (data1.get(joinCondition.field1).compareTo(data2.get(joinCondition.field2)) < 0) {
						if (table1.dictFile != null) {
							for (Map.Entry<String, String> map : data1.entrySet()) {
								temData.put(table1.name + "." + map.getKey(), map.getValue());
							}
						} else {
							for (Map.Entry<String, String> map : data1.entrySet()) {
								temData.put(map.getKey(), map.getValue());
							}
						}
						if (table2.dictFile != null) {
							for (Map.Entry<String, String> map : data2.entrySet()) {
								temData.put(table2.name + "." + map.getKey(), map.getValue());
							}
						} else {
							for (Map.Entry<String, String> map : data2.entrySet()) {
								temData.put(map.getKey(), map.getValue());
							}
						}
						table.data.add(temData);
					}
					break;
				default:
					System.out.println("no relationship named " + joinCondition.relationship);
					return null;
				}
			}
		}
//		System.out.println(joinCondition.field1 + joinCondition.relationship + joinCondition.field2);
//		for (Map<String, String> m : table.data) {
//			for (String s : m.values()) {
//				System.out.print(s + " ");
//			}
//			System.out.println();
//		}
		return table;
	}

	public static Table crossJoin(Table table1, Table table2) {
		System.out.println("TableUtils.crossJoin......");
		if (table1 == null || table2 == null) {
			System.out.println("TableUtils.crossJoin:null table");
			return null;
		}
		Table table = new Table();
		table.fieldMap = new LinkedHashMap<String, Field>();
		table.data = new ArrayList<Map<String, String>>();
		Map<String, Field> temFieldMap = new LinkedHashMap<String, Field>();
		for (Map.Entry<String, Field> map : table1.fieldMap.entrySet()) {
			temFieldMap.put(map.getKey(), map.getValue());
		}
		for (Map.Entry<String, Field> map : table2.fieldMap.entrySet()) {
			temFieldMap.put(map.getKey(), map.getValue());
		}
		if (table1.dictFile != null) {
			System.out.println(table1.name);
			for (Map.Entry<String, Field> map : table1.fieldMap.entrySet()) {
				temFieldMap.put(table1.name + "." + map.getKey(), map.getValue());
			}
		} else {
			for (Map.Entry<String, Field> map : table1.fieldMap.entrySet()) {
				temFieldMap.put(map.getKey(), map.getValue());
			}
		}
		if (table2.dictFile != null) {
			for (Map.Entry<String, Field> map : table2.fieldMap.entrySet()) {
				temFieldMap.put(table2.name + "." + map.getKey(), map.getValue());
			}
		} else {
			for (Map.Entry<String, Field> map : table2.fieldMap.entrySet()) {
				temFieldMap.put(map.getKey(), map.getValue());
			}
		}
		table.fieldMap.putAll(temFieldMap);
//		System.out.println(table.fieldMap.size());
//		for (String s : table.fieldMap.keySet()) {
//			System.out.println(s);
//		}
		for (Map<String, String> data1 : table1.data) {
			for (Map<String, String> data2 : table2.data) {
				// 用于存放临时数据，之后将其复制进data
				Map<String, String> temData = new LinkedHashMap<String, String>();
				if (table1.dictFile != null) {
					for (Map.Entry<String, String> map : data1.entrySet()) {
						temData.put(table1.name + "." + map.getKey(), map.getValue());
					}
				} else {
					for (Map.Entry<String, String> map : data1.entrySet()) {
						temData.put(map.getKey(), map.getValue());
					}
				}
				if (table2.dictFile != null) {
					for (Map.Entry<String, String> map : data2.entrySet()) {
						temData.put(table2.name + "." + map.getKey(), map.getValue());
					}
				} else {
					for (Map.Entry<String, String> map : data2.entrySet()) {
						temData.put(map.getKey(), map.getValue());
					}
				}
				table.data.add(temData);
			}
		}
//		for (Map<String, String> m : table.data) {
//			for (String s : m.values()) {
//				System.out.print(s + " ");
//			}
//			System.out.println();
//		}
		return table;
	}
}
