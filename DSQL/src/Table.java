import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Table {
	public static final int Max_LineNum = 20;
	public String name;
	public File parentPath;
	public LinkedHashSet<File> fileSet;
	public File dictFile;
	public File tableFolder;
	public Map<String,Field> fieldMap;
	public List<Map<String,String>> data;
	public void Table() {
	}


}
