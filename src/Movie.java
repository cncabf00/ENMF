import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Movie {
	static Map<String,Integer> genreMap;
	
	
	
	Integer[] features;
	
	static public void loadGenres(String filename) {
		File file=new File(filename);
		genreMap=new HashMap<>();
		try {
			BufferedReader reader=new BufferedReader(new FileReader(file));
			int count=0;
			String line=reader.readLine();
			while (line!=null) {
				genreMap.put(line.trim(), count++);
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
	
	static public Map<Integer,Movie> readMovies(String filename) {
		Map<Integer,Movie> movies=new HashMap<>();
		File file=new File(filename);
		try {
			BufferedReader reader=new BufferedReader(new FileReader(file));
			String line=reader.readLine();
			while (line!=null) {
				Movie movie=new Movie();
				String[] strs=line.split("::");
				Integer index=Integer.parseInt(strs[0]);
				String[] s=strs[2].split("\\|");
				movie.features=new Integer[s.length];
				for (int i=0;i<s.length;i++) {
					movie.features[i]=genreMap.get(s[i]);
				}
				movies.put(index,movie);
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
		return movies;
	}
	
	
}
