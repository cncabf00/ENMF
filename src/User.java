import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class User {
	static Map<String,Integer> featureMap;
	
	
	
	Integer[] features;
	
	static public void loadFeatures(String filename) {
		File file=new File(filename);
		featureMap=new HashMap<>();
		try {
			BufferedReader reader=new BufferedReader(new FileReader(file));
			int count=0;
			String line=reader.readLine();
			while (line!=null) {
				featureMap.put(line.trim(), count++);
				line=reader.readLine();
			}
			reader.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static public Map<Integer,User> readUsers(String filename) {
		Map<Integer,User> users=new HashMap<>();
		File file=new File(filename);
		try {
			BufferedReader reader=new BufferedReader(new FileReader(file));
			String line=reader.readLine();
			while (line!=null) {
				User user=new User();
				String[] strs=line.split("::");
				Integer index=Integer.parseInt(strs[0]);
//				String[] s=strs[2].split("\\|");
				user.features=new Integer[3];
				user.features[0]=featureMap.get(strs[1]);
				user.features[1]=featureMap.get("a"+strs[2]);
				user.features[2]=featureMap.get(strs[3]);
//				for (int i=0;i<s.length;i++) {
//					movie.features[i]=featureMap.get(s[i]);
//				}
				
				users.put(index,user);
				line=reader.readLine();
			}
			reader.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return users;
	}
	
	
}
