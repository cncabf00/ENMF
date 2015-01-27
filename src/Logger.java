import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


public class Logger {
	
	static String PATH="log";
	BufferedWriter writer;
	
	public Logger() {
		File folder=new File(PATH);
		if (!folder.exists())
			folder.mkdirs();
		
		Date now = new Date();
		DateFormat d1 = DateFormat.getDateTimeInstance();
		String str = d1.format(now);
		String filename=PATH+File.separator+str.replaceAll(":", "_")+".txt";
		File file=new File(filename);
		try {
			file.createNewFile();
			writer=new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void log(String content,boolean console) {
		try {
			writer.write(content+"\n");
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (console) {
			System.out.println(content);
		}
	}
	
	public void log(String content) {
		log(content,true);
	}
}
