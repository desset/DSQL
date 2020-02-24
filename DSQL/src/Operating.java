import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Operating {
	private static final Pattern PATTERN_TEST = Pattern.compile("\\w+");
//    private static final Pattern PATTERN_INSERT = Pattern.compile("insert\\s+into\\s+(\\w+)(\\(((\\w+,?)+)\\))?\\s+\\w+\\((([^\\)]+,?)+)\\);?");

	private static final Pattern PATTERN_INSERT = Pattern
			.compile("insert\\s+into\\s+(\\w+)\\s+(\\(((\\w+,?)+)\\))?\\s+\\w+\\s\\((([^\\)]+,?)+)\\);?");
	// ((?:pattern,))中?:的作用是不把(?:pattern,)作为一个mather.group()，而是将((?:pattern,))整体作为一个group
	private static final Pattern PATTERN_CREATE_TABLE = Pattern
			.compile("create\\stable\\s(\\w+)\\s?\\(((?:\\s?\\w+\\s\\w+,?)+)\\)\\s?;");
	private static final Pattern PATTERN_ALTER_TABLE_ADD = Pattern
			.compile("alter\\stable\\s(\\w+)\\sadd\\s(\\w+\\s\\w+)\\s?;");
	// 括号()被转义
	// delete from ${tablename} (where(${fieldname} = ${value}(and ${fieldname} =
	// ${value})*))?
	private static final Pattern PATTERN_DELETE = Pattern.compile(
			"delete\\sfrom\\s(\\w+)(?:\\swhere\\s(\\w+\\s?[<=>]\\s?[^\\s\\;]+(?:\\sand\\s(?:\\w+)\\s?(?:[<=>])\\s?(?:[^\\s\\;]+))*))?\\s?;");
	private static final Pattern PATTERN_UPDATE = Pattern.compile(
			"update\\s(\\w+)\\sset\\s(\\w+\\s?=\\s?[^,\\s]+(?:\\s?,\\s?\\w+\\s?=\\s?[^,\\s]+)*)(?:\\swhere\\s(\\w+\\s?[<=>]\\s?[^\\s\\;]+(?:\\sand\\s(?:\\w+)\\s?(?:[<=>])\\s?(?:[^\\s\\;]+))*))?\\s?;");
	private static final Pattern PATTERN_DROP_TABLE = Pattern.compile("drop\\stable\\s(\\w+);");
	private static final Pattern PATTERN_SELECT = Pattern.compile(
			"select\\s(\\*|(?:(?:\\w+(?:\\.\\w+)?)+(?:\\s?,\\s?\\w+(?:\\.\\w+)?)*))\\sfrom\\s(\\w+(?:\\s?,\\s?\\w+)*)(?:\\swhere\\s([^\\;]+\\s?))?;");
	private static final Pattern PATTERN_DELETE_INDEX = Pattern.compile("delete\\sindex\\s(\\w+)\\s?;");
	// grant admin to user1;
	private static final Pattern PATTERN_GRANT_ADMIN = Pattern.compile("grant\\sadmin\\sto\\s([^;\\s]+)\\s?;");
	private static final Pattern PATTERN_REVOKE_ADMIN = Pattern.compile("revoke\\sadmin\\sfrom\\s([^;\\s]+)\\s?;");

	public void operate() {
		Scanner sc = new Scanner(System.in);
		String cmd;
		while (!"exit".equals(cmd = sc.nextLine())) {
			Matcher matcherInsert = PATTERN_INSERT.matcher(cmd);
			Matcher matcherCreateTable = PATTERN_CREATE_TABLE.matcher(cmd);
			Matcher matcherAlterTableAdd = PATTERN_ALTER_TABLE_ADD.matcher(cmd);
			Matcher matcherDelete = PATTERN_DELETE.matcher(cmd);
			Matcher matcherUpdate = PATTERN_UPDATE.matcher(cmd);
			Matcher matcherDropTable = PATTERN_DROP_TABLE.matcher(cmd);
			Matcher matcherSelect = PATTERN_SELECT.matcher(cmd);
			Matcher matcherDeleteIndex = PATTERN_DELETE_INDEX.matcher(cmd);
			Matcher matcherGrantAdmin = PATTERN_GRANT_ADMIN.matcher(cmd);
			Matcher matcherRevokeAdmin = PATTERN_REVOKE_ADMIN.matcher(cmd);
			if (matcherInsert.find()) {
				insert(matcherInsert);
			}
			if (matcherCreateTable.find()) {
				createTable(matcherCreateTable);
			}
			if (matcherAlterTableAdd.find()) {
				alterTableAdd(matcherAlterTableAdd);
			}
			if (matcherDelete.find()) {
				delete(matcherDelete);
			}
			if (matcherUpdate.find()) {
				update(matcherUpdate);
			}
//			if (matcherDropTable.find()) {
//
//			}
			if (matcherSelect.find()) {
				select(matcherSelect);
			}
//			if (matcherDeleteIndex.find()) {
//
//			}
//			if (matcherGrantAdmin.find()) {
//
//			}
//			if (matcherRevokeAdmin.find()) {
//
//			}

		}
	}

	private void select(Matcher matcherSelect) {
		String selectStr = matcherSelect.group(1);
		String fromStr = matcherSelect.group(2);
		String whereStr = matcherSelect.group(3);
		// 解析group(2)中表示表名的语句
		List<String> nameList = new ArrayList<String>();
		Map<String, Table> tableMap = new LinkedHashMap<String, Table>();
		String froms[] = fromStr.trim().split(",");
		for (String from : froms) {
			nameList.add(from.trim());
			if(TableUtils.getTable(from)!=null) {
				tableMap.put(from, TableUtils.getTable(from));
			}
			else {
				System.out.println("Operating.select:no table named "+from);
				return;
			}
		}
		// 解析group(1)中的select表示要选择的字段的语句
		Map<String, List<String>> fieldMap = new LinkedHashMap<String, List<String>>();

		String selects[] = selectStr.trim().split(",");
		// 将所有的select中的field格式化为table.field的形式并存入fieldMap
		for (String select : selects) {
			// 如果tablename.fieldname的形式直接存入Map
			if (select.contains(".")) {
//				System.out.println(select);
				// .居然需要转义？？？
				String[] sections = select.trim().split("\\.");
//				System.out.println("sections length: "+sections.length);
				String tableName = sections[0];
				String fieldName = sections[1];
				// 若还没有向fieldMap中加入key为tableName的项，则创建之
				if (fieldMap.get(tableName) == null) {
					List<String> fieldList = new ArrayList<String>();
					fieldList.add(select);
					fieldMap.put(tableName, fieldList);
				} else {
					fieldMap.get(tableName).add(select);
				}
			} else {
				for (Table table : tableMap.values()) {
					if (table.fieldMap.keySet().contains(select)) {
						select = table.name + "." + select;
//						System.out.println(select);
						// 若还没有向fieldMap中加入key为tableName的项，则创建之
						if (fieldMap.get(table.name) == null) {
							List<String> fieldList = new ArrayList<String>();
							fieldList.add(select);
							fieldMap.put(table.name, fieldList);
						} else {
							fieldMap.get(table.name).add(select);
						}
					}
				}
			}

		}
//		for(String key:fieldMap.keySet()) {
//			List<String> stringList=fieldMap.get(key);
//			System.out.println(key+":");
//			for(String s:stringList) {
//				System.out.println(s);
//			}
//		}
		// 解析group(3)中的where语句中的过滤条件，field的值格式化为table.field
		// 分为3类：
		// 1.选择查询(field=value)
		// 2.自连接(同一个表里的两个字段相等,field1=field2)
		// 3.多表关联查询(属于不同表中的字段相等)
		List<Map<String, String>> whereList = StringUtils.parseWhere(whereStr);
		// 存选择查询
		Map<String, List<Filter>> filterMap = new LinkedHashMap<String, List<Filter>>();
		// 存自连接
		Map<String, List<JoinCondition>> selfConnection = new LinkedHashMap<String, List<JoinCondition>>();
		// 存两个表关联查询
		List<JoinCondition> doubleConnection = new ArrayList<JoinCondition>();
		// 存关联过的表名，用于完成所有关联查询后从tableMap中去掉这些表以防重复做笛卡尔积
		Set<String> joinedTableName = new LinkedHashSet<String>();
		// 存各个表中的数据，通过过滤后再将所有的表连接(笛卡尔积)成一张表，然后再根据fieldMap筛选所需的列
		Map<String, List<Map<String, String>>> dataMap = new LinkedHashMap<String, List<Map<String, String>>>();
		// 存最终结果
		Table resulTable = new Table();
		for (Map<String, String> where : whereList) {
//			for(Map.Entry<String, String> whereEntry:where.entrySet()) {
//				System.out.println(whereEntry.getKey()+" "+whereEntry.getValue());
//				
//			}
//			System.out.println(where.get("condition"));
			// 如果右值不包含.,则说明是选择查询
			if (!where.get("condition").contains(".")) {
//				System.out.println("no .");
				// 如果左值包含.则直接获得field所属于的表
				if (where.get("field").contains(".")) {
					String sections[] = where.get("field").split("\\.");
					String tableName = sections[0];
					String fieldName = sections[1];
//					System.out.println(sections[0]+" "+sections[1]);
					Table table = tableMap.get(tableName);
					Filter filter = new Filter();
					// 若tablename不在group中的表名集合中，则报错并返回
					if (table == null) {
						System.out.println("field named " + where + " no suit table set");
						return;
					}
					// 若该表没有这个field，报错并返回
					if (table.fieldMap.get(fieldName) == null) {
						System.out.println("Invalid input:no field named " + where.get("field"));
						return;
					}
					filter.field = new Field(fieldName, table.fieldMap.get(fieldName).type);
					filter.relationship = Relationship.parseRel(where.get("relationship"));
					filter.condition = where.get("condition");
					// 若还没有加入key为tableName的项，则创建之
					if (filterMap.get(tableName) == null) {
						List<Filter> newFilterList = new ArrayList<Filter>();
						newFilterList.add(filter);
						filterMap.put(tableName, newFilterList);
					}
					// filterMap中已经存在key为tableName的项，则直接加入
					else {
						filterMap.get(tableName).add(filter);
					}
				}
				// 右值不包含.(也就是选择查询)，左值不包含.(需要用代码判断field所属的表)
				else {
					for (Map.Entry<String, Table> tablEntry : tableMap.entrySet()) {
						Table table = tablEntry.getValue();
						if (table.fieldMap.keySet().contains(where.get("field"))) {
							Filter filter = new Filter();
							filter.field = new Field(where.get("field"), table.fieldMap.get(where.get("field")).type);
							filter.relationship = Relationship.parseRel(where.get("relationship"));
							filter.condition = where.get("condition");
							// 若还没有加入key为tableName的项，则创建之
							if (filterMap.get(table.name) == null) {
								List<Filter> newFilterList = new ArrayList<Filter>();
								newFilterList.add(filter);
								filterMap.put(table.name, newFilterList);
							}
							// filterMap中已经存在key为tableName的项，则直接加入
							else {
								filterMap.get(table.name).add(filter);
							}
						}
					}
				}

			}

			// 若右值包含.，则说明是自连接或者多表关联查询，需要进一步判断
			else {
//				System.out.println("contains .");
				// 若右值是table.field的格式则左值也要求是table.field的格式
				if (!where.get("field").contains(".")) {
					System.out.println("Invalid format:left value should contains table name.");
					return;
				}
				String[] leftValue = where.get("field").split("\\.");
				String[] rightValue = where.get("condition").split("\\.");
				String table1 = leftValue[0];
				String field1 = leftValue[1];
				String table2 = rightValue[0];
				String field2 = rightValue[1];
				// 判断查询语句中表名以及字段的合法性
				JoinCondition joinCondition = new JoinCondition(table1, table2, field1, field2,
						where.get("relationship"));
//				System.out.println("输出左右值的table值："+joinCondition.table1+" "+joinCondition.table2);
				// 若左右值的表名相同，说明是自连接查询(两个字段属于同一个表)
				// 比较字符串相等只能用.equals()，不能有=，切记！！！
				if (joinCondition.table1.equals(joinCondition.table2)) {
					// 如果selfConnection没有加入key为table1的项，则创建之
					if (selfConnection.get(joinCondition.table1) == null) {
						List<JoinCondition> joinConditions = new ArrayList<JoinCondition>();
						joinConditions.add(joinCondition);
						selfConnection.put(joinCondition.table1, joinConditions);
					}
					// 如果selfConnection已经存在key为table1的项，则直接加入到List
					else {
						selfConnection.get(joinCondition.table1).add(joinCondition);
					}

				}
				// 若左右值的表名不相同，说明是两个表的关联查询(两个field属于不同的表)
				else {
//					joinCondition.field1=table1+field1;
//					joinCondition.field2=table2+field2;
					doubleConnection.add(joinCondition);
				}
			}
		}
		// 使用filterMap过滤，执行选择查询
		// 得到tableMap中每个表的数据
		// filterMap中包含的表返回文件中读出后在经过List<Filter>过滤后的数据，否则直接返回从文件读出的数据
		// 使用for(s:S)结构时底层是通过迭代器实现的，迭代器遍历的过程中迭代器的结构不允许被破坏,
		// 所以使用for(Table t:tableMap.values())时；不能使用tableMap.remove(t.name);，
		// 否则会报错java.util.ConcurrentModificationException
		// 如果非要使用remove，可使用迭代器遍历如下
//		for(Table t:tableMap.values()) {

		// 由于关联查询有两个表名都需要删除，但是迭代器只能删除当前的一个表，
		// 所以先将删除不了的等号另一边的表存起来，迭代完毕后再删除
		// Set<String> temTableNameList = new LinkedHashSet<String>();
		Iterator<Entry<String, Table>> iteratorFilter = tableMap.entrySet().iterator();
		while (iteratorFilter.hasNext()) {
			Table t = iteratorFilter.next().getValue();
			// 若有属于这个表的过滤器，则过滤
			if (filterMap.keySet().contains(t.name)) {
				List<Map<String, String>> data = TableUtils.dataFilter(t, filterMap.get(t.name));
				dataMap.put(t.name, data);
			} else {
				dataMap.put(t.name, t.data);
			}
			// 若此表为空或经过滤后为空，则从tableMap中删去此表并省去后序查询
//			if (dataMap.get(t.name) == null || dataMap.get(t.name).size() == 0) {
//				// 使用迭代器的remove不会报错
//				iteratorFilter.remove();
//				// tableMap.remove(t.name);
//				// 省去此表有关的自连接查询
//				if (selfConnection.get(t.name) != null) {
//					selfConnection.remove(t.name);
//				}
//				// 省去此表有关的多表关联查询
//				// Map或List的遍历过程中要进行remove时只能使用迭代器自己的remove()
//				// 由于关联查询有两个表名都需要删除，但是迭代器只能删除当前的一个表，
//				// 所以先将删除不了的等号另一边的表存起来，迭代完毕后再删除
////				for(JoinCondition j:doubleConnection) {
//				Iterator<JoinCondition> iterDoubleCon = doubleConnection.iterator();
//				while (iterDoubleCon.hasNext()) {
//
//					JoinCondition j = iterDoubleCon.next();
//					if (j.table1.equals(t.name) || j.table2.equals(t.name)) {
//						// 判断选取此时删除不了的表名存储起来
//						if (j.table1.equals(t.name)) {
//							temTableNameList.add(j.table2);
//						} else {
//							temTableNameList.add(j.table1);
//						}
//						System.out.println(t.name + "为空导致删除......");
//						iterDoubleCon.remove();
//					}
//				}
//			}
			// 若若此表为空或经过滤后为空，则报错并返回
			if (dataMap.get(t.name) == null || dataMap.get(t.name).size() == 0) {
				System.out.println("Operating.select:Data from " + t.name + " suited Filters rules is null");
				return;
			} else {
				tableMap.get(t.name).data = dataMap.get(t.name);
			}
		}

		// 执行自连接过滤
//		for(Table t:tableMap.values()) {
		Iterator<Entry<String, Table>> iteratorSelfCon = tableMap.entrySet().iterator();
		while (iteratorSelfCon.hasNext()) {
			Table t = iteratorSelfCon.next().getValue();
			// 若有属于这个表的自连接
			if (selfConnection.keySet().contains(t.name)) {
				dataMap.put(t.name, TableUtils.selfConFilter(dataMap.get(t.name), selfConnection.get(t.name)));
			}
			// 若字表为空或经过滤后为空，则从tableMap中删去此表并省去后序查询
//			if (dataMap.get(t.name) == null || dataMap.get(t.name).size() == 0) {
//				iteratorSelfCon.remove();
//				// 省去此表有关的自连接查询
//				selfConnection.remove(t.name);
////				tableMap.remove(t.name);
//				// 省去此表有关的后续多表关联从查询
//				// Map或List的遍历过程中要进行remove时只能使用迭代器自己的remove()
//				// 由于关联查询有两个表名都需要删除，但是迭代器只能删除当前的一个表，
//				// 所以先将删除不了的等号另一边的表存起来，迭代完毕后再删除
////				for(JoinCondition j:doubleConnection) {
//				Iterator<JoinCondition> iterDoubleCon = doubleConnection.iterator();
//				while (iterDoubleCon.hasNext()) {
//
//					JoinCondition j = iterDoubleCon.next();
//					if (j.table1.equals(t.name) || j.table2.equals(t.name)) {
//						// 判断选取此时删除不了的表名存储起来
//						if (j.table1.equals(t.name)) {
//							temTableNameList.add(j.table2);
//						} else {
//							temTableNameList.add(j.table1);
//						}
//						System.out.println(t.name + "为空导致删除......");
//						iterDoubleCon.remove();
//					}
//				}
//			} 
			if (dataMap.get(t.name) == null || dataMap.get(t.name).size() == 0) {
				System.out.println("Operating.select:Data from " + t.name + " suited joinCondition rules is null");
				return;
			} else {
				tableMap.get(t.name).data = dataMap.get(t.name);
			}
		}
		// 删除前面迭代器中无法删除的，由于等号另一边的表为空而不需要关联查询的表名
		Iterator<Entry<String, Table>> iteratorDelTem = tableMap.entrySet().iterator();
//		while (iteratorDelTem.hasNext()) {	
//			Table t = iteratorDelTem.next().getValue();
////			System.out.println("iteratorDelTem:"+t.name);
//			if (temTableNameList.contains(t.name)) {
////				System.out.println("DeleteTable:"+t.name);
//				iteratorDelTem.remove();
//			}
//		}
//		Iterator<Entry<String, Table>> finalTable = tableMap.entrySet().iterator();
//		while (finalTable.hasNext()) {	
//			Table t = finalTable.next().getValue();
//			System.out.println("finalTable:"+t.name);
//		}
//		System.out.println("输出关联查询前的数据");
//		for(Map.Entry<String, Table> t:tableMap.entrySet()) {
//			Table table=t.getValue();
//			for(Map<String, String> map:table.data) {
//				for(Map.Entry<String, String> mEntry:map.entrySet()) {
//					System.out.println(mEntry.getKey()+" "+mEntry.getValue());
//				}
//			}
//		}
		// 将所有的表连接在一起

		// 1.首先执行两个表关联查询并连接

		for (JoinCondition joinCondition : doubleConnection) {
			Table table1 = tableMap.get(joinCondition.table1);
			Table table2 = tableMap.get(joinCondition.table2);
//			System.out.println(joinCondition.table1 + " " + joinCondition.table2);
			if (resulTable.fieldMap == null || table1.dictFile == null || table2.dictFile == null) {
//				System.out.println("invoke TableUtils.doubleConFilter() with " + table1 + " " + table2);
				if (table1 != null && table2 != null) {
					resulTable = TableUtils.doubleConFilter(table1, table2, joinCondition);
					if (resulTable == null || resulTable.data.size() == 0) {
						System.out.println("Operating.select:Data from " + table1 + " and " + table2
								+ " suited joinCondition is null");
						return;
					}
				} else {
					continue;
				}
			} else {
				Table temTable = new Table();
				temTable = TableUtils.doubleConFilter(table1, table2, joinCondition);
				resulTable = TableUtils.crossJoin(resulTable, temTable);
			}
			joinedTableName.add(joinCondition.table1);
			joinedTableName.add(joinCondition.table2);
			for (String string : joinedTableName) {
				tableMap.put(string, resulTable);
			}
		}
		// 对剩下的不用关联查询的表进行笛卡尔积连接
		// 1.从tableMap中删除已经关联查询过的表
		Iterator<Entry<String, Table>> tableIterator = tableMap.entrySet().iterator();
		while (tableIterator.hasNext()) {
			Map.Entry<String, Table> table = tableIterator.next();
//			System.out.println(table.getKey());
			if (joinedTableName.contains(table.getKey())) {
				tableIterator.remove();
			}
		}
		// 2.将剩下的没有关联查询的表进行笛卡尔积连接
//		System.out.println("输出resultTable:");
//		for (Map<String, String> m :resulTable.data) {
//			for (String s : m.values()) {
//				System.out.print(s + " ");
//			}
//			System.out.println();
//		}
		Iterator<Entry<String, Table>> crossJoinIterator = tableMap.entrySet().iterator();
		while (crossJoinIterator.hasNext()) {
			Map.Entry<String, Table> table = crossJoinIterator.next();

			if (resulTable.data != null && tableMap != null) {
				resulTable = TableUtils.crossJoin(resulTable, table.getValue());
			}
			// 若没有执行过关联查询连接(resulTable.data==null)且存在待笛卡尔积连接的表(tableMap!=null)
			// 则将第一个表作为resulTable便于后续迭代
			// 此时没有经过连接操作，所以需要将这个表的field改为table.field的形式
			else if (resulTable.data == null && tableMap != null) {
				Map<String, Field> temFieldMap = new LinkedHashMap<String, Field>();
				for (Map.Entry<String, Field> map : table.getValue().fieldMap.entrySet()) {
					temFieldMap.put(table.getKey() + "." + map.getKey(), map.getValue());
				}
				resulTable.fieldMap = new LinkedHashMap<String, Field>();
				resulTable.fieldMap = temFieldMap;
				resulTable.data = new ArrayList<Map<String, String>>();
				for (Map<String, String> data : table.getValue().data) {
					Map<String, String> temData = new LinkedHashMap<String, String>();
					for (Map.Entry<String, String> mEntry : data.entrySet()) {
						temData.put(table.getKey() + "." + mEntry.getKey(), mEntry.getValue());
					}
					resulTable.data.add(temData);
				}

			}

		}

		// 输出filterMap中的所有过滤器的信息
//		System.out.println("filterMap......");
//		for (Map.Entry<String, List<Filter>> e : filterMap.entrySet()) {
//			System.out.println(e.getKey() + ":");
//			for (Filter f : e.getValue()) {
//				System.out.println(f.field.name + " " + f.relationship + " " + f.condition);
//			}
//		}
		// 输出selfConnection中的所有信息
//		System.out.println("selfConnection......");
//		for (Map.Entry<String, List<JoinCondition>> e : selfConnection.entrySet()) {
//			System.out.println(e.getKey() + ":");
//			for (JoinCondition j : e.getValue()) {
//				System.out.println(j.table1 + " " + j.field1 + " " + j.relationship + " " + j.table2 + " " + j.field2);
//			}
//		}
		// 输出doubleConnection中的所有信息
//		System.out.println("doubleConnection......");
//		for (JoinCondition j : doubleConnection) {
//			System.out.println(j.table1 + " " + j.field1 + " " + j.relationship + " " + j.table2 + " " + j.field2);
//		}
		// 输出dataMap中所有数据的值
//		for (Map.Entry<String, List<Map<String, String>>> d : dataMap.entrySet()) {
//			System.out.println("tablename: " + d.getKey());
//			for (Map<String, String> m : d.getValue()) {
//				for (String s : m.values()) {
//					System.out.print(s + " ");
//				}
//				System.out.println();
//			}
//		}
		// 没有筛选字段的查询结果
//		System.out.println("最终查询结果：");
//		for (Map<String, String> m : resulTable.data) {
//			for (String s : m.values()) {
//				System.out.print(s + " ");
//			}
//			System.out.println();
//		}
		// 筛选完字段的查询结果
		System.out.println("筛选完字段的查询结果：");
		// 统计需要筛选的字段总数
		int fieldCount = 0;
		// 控制字段总数的值只统计一条数据的field个数，避免重复累加
		boolean flag = true;
		List<Map<String, String>> resultData = new ArrayList<Map<String, String>>();
		for (Map<String, String> data : resulTable.data) {
			Map<String, String> temDataMap = new LinkedHashMap<String, String>();
			for (Map.Entry<String, List<String>> mEntry : fieldMap.entrySet()) {
				if (flag == true) {
					fieldCount += mEntry.getValue().size();
				}
//				System.out.println(mEntry.getKey());
				for (String s : mEntry.getValue()) {
//					System.out.print(s+" ");
					temDataMap.put(s, data.get(s));
				}
//				System.out.println();
			}
			flag = false;
			resultData.add(temDataMap);
		}
//		for (Map<String, String> m :resultData) {
//			for (String s : m.values()) {
//				System.out.print(s + " ");
//			}
//			System.out.println();
//		}

		// 计算名字长度，用来对齐数据
		int[] len = new int[fieldCount];
		int m = 0;
		for (Map.Entry<String, List<String>> mEntry : fieldMap.entrySet()) {
			for (String dataName : mEntry.getValue()) {
				len[m] = dataName.length();
				m++;
				System.out.printf("|%s", dataName);
			}
		}

		System.out.println("|");
		System.out.print("|");

		for (int ls : len) {
			for (int l = 1; l <= ls; l++) {
				System.out.printf("-");
			}
		}
		for (int k = 0; k < len.length - 1; k++) {
			System.out.printf("-");
		}
		System.out.println("|");

		for (Map<String, String> line : resultData) {
			Iterator<String> valueIter = line.values().iterator();
			for (int i = 0; i < len.length; i++) {
				String value = valueIter.next();
				System.out.printf("|%s", value);
				for (int j = 0; j < len[i] - value.length(); j++) {
					System.out.printf(" ");
				}
			}
			System.out.println("|");
		}

	}

	private void alterTableAdd(Matcher matcherAlterTableAdd) {
		System.out.println("alterTableAdd......");
		String tableName = matcherAlterTableAdd.group(1);
		String fieldStr = matcherAlterTableAdd.group(2);
		Table table = TableUtils.getTable(tableName);
		if (table == null) {
			System.out.println("Table named " + tableName + " not exists!");
			return;
		}
		Map<String, Field> fieldMap = new LinkedHashMap<String, Field>();
		String[] properties = fieldStr.split(" ");
		fieldMap.put(properties[0], new Field(properties[0], properties[1]));
//		System.out.println(properties[0]+" "+properties[1]);
		TableUtils.alterTableAdd(table, fieldMap);
	}

	private void update(Matcher matcherUpdate) {
		String tableName = matcherUpdate.group(1);
		String setStr = matcherUpdate.group(2);
		String whereStr = matcherUpdate.group(3);
		Table table = TableUtils.getTable(tableName);
		if (table == null || table.data.size() == 0) {
			System.out.println("Table named " + tableName + " not exists!");
			return;
		}
		// 解析where语句
		List<Filter> filterList = new ArrayList<Filter>();
		List<Map<String, String>> filterMapList = StringUtils.parseWhere(whereStr);
		for (Map<String, String> filterMap : filterMapList) {
//			System.out.println(filterMap.get("field"));
//			System.out.println(filterMap.get("relationship"));
//			System.out.println(filterMap.get("condition"));
			Filter filter = new Filter();
			if (table.fieldMap.get(filterMap.get("field")) == null) {
				System.out.println("Invalid input:no field named " + filterMap.get("field"));
				return;
			}
			filter.field = new Field(filterMap.get("field"), table.fieldMap.get(filterMap.get("field")).type);
			filter.relationship = Relationship.parseRel(filterMap.get("relationship"));
			filter.condition = filterMap.get("condition");
			filterList.add(filter);
//			System.out.println(filter.field);
//			System.out.println(filter.relationship);
//			System.out.println(filter.condition);

		}
		// 解析set语句
		Map<String, String> setData = new LinkedHashMap<String, String>();
		String field;
		String value;
		String[] setStrs = setStr.trim().split(",");
		if (setStrs.length > table.fieldMap.size()) {
			System.out.println("the number of set part if more than tableFields");
			return;
		}
		for (String set : setStrs) {
			String[] properties = set.trim().split("=");
			field = properties[0];
			value = properties[1];
			setData.put(field, value);
		}
		TableUtils.update(table, filterList, setData);
	}

	public void delete(Matcher matcherDelete) {
		System.out.println("Operating.delete......");
		String tableName = matcherDelete.group(1);
		String whereStr = matcherDelete.group(2);
		Table table = TableUtils.getTable(tableName);
		if (table == null || table.data.size() == 0) {
			System.out.println("Table named " + tableName + " not exists!");
		}
//		System.out.println(tableName+" "+filterStr);
		// 生成过滤器
		List<Filter> filterList = new ArrayList<Filter>();
		List<Map<String, String>> filterMapList = StringUtils.parseWhere(whereStr);
		for (Map<String, String> filterMap : filterMapList) {
//			System.out.println(filterMap.get("field"));
//			System.out.println(filterMap.get("relationship"));
//			System.out.println(filterMap.get("condition"));
			Filter filter = new Filter();
			if (table.fieldMap.get(filterMap.get("field")) == null) {
				System.out.println("Invalid input:no field named " + filterMap.get("field"));
				return;
			}
			filter.field = new Field(filterMap.get("field"), table.fieldMap.get(filterMap.get("field")).type);
			filter.relationship = Relationship.parseRel(filterMap.get("relationship"));
			filter.condition = filterMap.get("condition");
			filterList.add(filter);
//			System.out.println(filter.field);
//			System.out.println(filter.relationship);
//			System.out.println(filter.condition);

		}
		TableUtils.delete(table, filterList);
	}

	public void createTable(Matcher matcherCreateTable) {
		System.out.println("Operating.createTable......");
		String tablename = matcherCreateTable.group(1);
		String properties = matcherCreateTable.group(2);
		Table table = TableUtils.getTable(tablename);
		if (table != null) {
			System.out.println("table named " + tablename + " has been exist.");
			return;
		}
		// 这里不定义成Field[]而是定义成Map<string,Field>是为了方便通过字段名查找字段信息
		// 相当于加了Field.name为索引的Field[]
		// 这个技巧在很多地方可以使用
		Map<String, Field> fieldMap = StringUtils.parseFieldInfo(properties);
//		 for(Map.Entry<String,Field> field:fieldMap.entrySet()) {
//			 System.out.println(field.getValue());
//		 }

		TableUtils.createTable(tablename, fieldMap);
	}

	public void insert(Matcher matcherInsert) {
		System.out.println("Operating.insert......");
		String tableName = matcherInsert.group(1);
		String fieldStr = matcherInsert.group(3);
		String valueStr = matcherInsert.group(5);
//		System.out.println(tableName+" "+" "+fieldStr+" "+valueStr);
		Table table = TableUtils.getTable(tableName);
		if (table == null) {
			System.out.println("Table named " + tableName + " not exists!");
			return;
		}
		Map<String, String> insertData = new LinkedHashMap<String, String>();
		String[] fieldStrs = fieldStr.trim().split(",");
		String[] valueStrs = valueStr.trim().split(",");
		if (fieldStrs.length != valueStrs.length) {
			System.out.println("Invalid format:length of fields not suit length of values");
			return;
		}
		for (int i = 0; i < fieldStrs.length; i++) {
			insertData.put(fieldStrs[i], valueStrs[i]);
//			System.out.println(fieldStrs[i]+" "+valueStrs[i]);
		}
		TableUtils.insert(table, insertData);

	}

}
