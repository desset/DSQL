public class JoinCondition {
	String table1;
	String table2;
	String field1;
	String field2;
	String relationship;
	public JoinCondition(String table1, String table2, String field1, String field2, String relationship) {
		super();
		this.table1 = table1;
		this.table2 = table2;
		this.field1 = field1;
		this.field2 = field2;
		this.relationship = relationship;
	}
}
