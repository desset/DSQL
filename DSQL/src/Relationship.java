public enum Relationship {
	LESS_THAN,
	MORE_THAN,
	EQUAL_TO;
	public static Relationship parseRel(String relationship) {
		switch (relationship) {
		case "<":
			return LESS_THAN;	
		case ">":
			return MORE_THAN;
		case "=":
			return EQUAL_TO;
		default:
			try {
				throw new Exception("条件错误!");
			}catch(Exception e){
				e.printStackTrace();
				return null;
			}
		}
	}
}
