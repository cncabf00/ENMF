import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RatingData {
	
	int userid;
	int itemid;
	double rating;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		List<RatingData> datas=RatingData.readFile("ratings_data");
//		int item=0;
//		for (RatingData data:datas) {
//			if (data.itemid>item)
//				item=data.itemid;
//		}
//		int[][] movies=new int[item+1][6];
//		for (int i=0;i<item;i++) {
//			for (int j=0;j<6;j++) {
//				movies[i][j]=0;
//			}
//		}
//		for (RatingData d:datas) {
//			movies[d.itemid][d.rating]++;
//		}
//		for (int i=0;i<item;i++) {
//			for (int j=1;j<6;j++) {
//				System.out.print(movies[i][j]+" ");
//			}
//			System.out.println();
//		}
	}
	
	public static List<RatingData> readFile(String filename) {
		List<RatingData> datas=new ArrayList<>();
		try {
			BufferedReader br=new BufferedReader(new FileReader(new File(filename)));
			String line=br.readLine();
			while (line!=null) {
				String[] strs=line.split("\\s");
				RatingData data=new RatingData();
				data.userid=Integer.parseInt(strs[0]);
				data.itemid=Integer.parseInt(strs[1]);
				data.rating=Integer.parseInt(strs[2]);
				datas.add(data);
				line=br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return datas;
	}
	
	public static List<RatingData> readFile1M(String filename) {
		List<RatingData> datas=new ArrayList<>();
		try {
			BufferedReader br=new BufferedReader(new FileReader(new File(filename)));
			String line=br.readLine();
			while (line!=null) {
				String[] strs=line.split("::");
				RatingData data=new RatingData();
				data.userid=Integer.parseInt(strs[0]);
				data.itemid=Integer.parseInt(strs[1]);
				data.rating=Integer.parseInt(strs[2]);
				datas.add(data);
				line=br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return datas;
	}
	
	public static List<List<List<RatingData>>> split(List<RatingData> origin) {
		return split(origin,5,true);
	}
	
	public static List<List<List<RatingData>>> split(List<RatingData> origin,int fold,boolean normalize) {
		if (normalize) {
			for (RatingData data:origin) {
				data.rating=(data.rating-1)/4;
			}
		}
		Collections.shuffle(origin);
		double per=(1.0)/fold;
		int n=(int) (origin.size()*per);
		List<List<List<RatingData>>> result=new ArrayList<>();
		for (int i=0;i<fold;i++) {
			List<List<RatingData>> l=new ArrayList<>();
			List<RatingData> test=new ArrayList<>();
			test.addAll(origin.subList(i*n, (i+1)*n));
			List<RatingData> train=new ArrayList<>();
			train.addAll(origin.subList((i+1)*n, origin.size()-1));
			train.addAll(origin.subList(0, i*n));
			l.add(train);
			l.add(test);
			result.add(l);
		}
		return result;
	}
	

}
