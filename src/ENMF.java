import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class ENMF {
	static final String DATA_PATH="data"+File.separator;
	
	int currentFold=0;
	int fold=5;
	List<List<List<RatingData>>> allData;
	List<RatingData> training;
	List<RatingData> testing;
	int userNum,movieNum;
	Movie[] movies;
	User[] users;
	double[][] fuser;
	double[][] fmovie;
	Random rand=new Random();
	double lambda=0.02;
	double lambda1=0.02;
	double lambda2=0.02;
	double lambda3=0.0003;
	double lrate1=0.1;
	double lrate2=0.3;
	double lrate3=0.2;

	
	double X[];
	int K=35;
	double[] bu;
	double[] bi;
	double avg=0;
	
	double[] countsForMovieFeatures;
	double[][] sumForMovieFeatures;
	
	double[] countsForUserFeatures;
	double[][] sumForUserFeatures;
	
	boolean useMovieInfo=false;
	boolean useUserInfo=false;
	boolean useBias=false;
	boolean useSum=true;
	
	Function useFunction=Function.Sigmoid;
	Function movieFunction=Function.Square;
	
	double vRMSE=0;
	double tRMSE=0;
	
	
	Logger logger=new Logger();
	
	public void normalize() {
		for (RatingData data:training) {
			data.rating=(data.rating-1)/4;
		}
		for (RatingData data:testing) {
			data.rating=(data.rating-1)/4;
		}
	}
	
	public void loadData(String filename,int fold) {
		allData=RatingData.split(RatingData.readFile1M(filename),fold,true);
//		currentFold=0;
//		training=allData.get(currentFold).get(0);
//		testing=allData.get(currentFold).get(1);
		
	}
	
	public void loadData(String traingFile,String testingFile) {
		training=RatingData.readFile(traingFile);
		testing=RatingData.readFile(testingFile);
//		for (RatingData data:training) {
//			if (data.userid>userNum)
//				userNum=data.userid;
//			if (data.itemid>movieNum)
//				movieNum=data.itemid;
//		}
//		for (RatingData data:testing) {
//			if (data.userid>userNum)
//				userNum=data.userid;
//			if (data.itemid>movieNum)
//				movieNum=data.itemid;
//		}
	}
	
	public void loadMovieData(String filename) {
		useMovieInfo=true;
		Map<Integer,Movie> movieMap=Movie.readMovies(filename);
		movies=new Movie[movieNum+1];
		for (int i=0;i<movies.length;i++) {
			movies[i]=movieMap.getOrDefault((Integer)i, null);
		}
	}
	
	public void loadUserData(String filename) {
		useUserInfo=true;
		Map<Integer,User> userMap=User.readUsers(filename);
		users=new User[userNum+1];
		for (int i=0;i<users.length;i++) {
			users[i]=userMap.getOrDefault((Integer)i, null);
		}
	}

	void initFeatureVector()
	{
		if (currentFold==0) {
			logger.log(userNum+" uses, "+movieNum+" movies");
			logger.log("K="+K);
			logger.log("lrate1="+lrate1+",lrate2="+lrate2+",lambda="+lambda+",lambda1="+lambda1+",lambda2="+lambda2+",lambda3="+lambda3);
			logger.log("Function used in user matrix: "+useFunction);
			logger.log("Function used in movie matrix: "+movieFunction);
			logger.log("use bias: "+useBias);
			logger.log("use sum: "+useSum);
			logger.log("use user info: "+useUserInfo);
			logger.log("use movie info: "+useMovieInfo);
		}
		logger.log("fold "+currentFold);
		
		for (RatingData data:training) {
			if (data.userid>userNum)
				userNum=data.userid;
			if (data.itemid>movieNum)
				movieNum=data.itemid;
		}
		for (RatingData data:testing) {
			if (data.userid>userNum)
				userNum=data.userid;
			if (data.itemid>movieNum)
				movieNum=data.itemid;
		}
	    fuser=new double[userNum+1][K];
	    fmovie=new double[movieNum+1][K];
	    
	    for(int i=1;i<=userNum;i++)
	    {
	        for(int j=0;j<K;j++)
	            fuser[i][j]=rand.nextDouble();
	    }
	     
	    for(int i=1;i<=movieNum;i++)
	    {
	        for(int j=0;j<K;j++)
	            fmovie[i][j]=rand.nextDouble();
	    }
	    
	    if (useBias) {
		    bu=new double[userNum+1];
		    bi=new double[movieNum+1];
		    for (RatingData data:training) {
				avg+=data.rating;
			}
		    avg/=training.size();
		    logger.log("avg="+avg);
	    }
	    
	    if (useMovieInfo) {
	    	countsForMovieFeatures=new double[Movie.genreMap.size()];
	    	sumForMovieFeatures=new double[Movie.genreMap.size()][K];
	    	
	    	for (int i=0;i<movies.length;i++) {
	    		Movie movie=movies[i];
	    		if (movie==null)
	    			continue;
	    		Integer[] genres=movie.features;
	    		for (int j=0;j<genres.length;j++) {
	    			countsForMovieFeatures[genres[j]]++;
	    			for (int k=0;k<K;k++) {
	    				sumForMovieFeatures[genres[j]][k]+=fy(fmovie[i][k]);
	    			}
	    		}
	    	}
	    }
	    
	    if (useUserInfo) {
	    	countsForUserFeatures=new double[User.featureMap.size()];
	    	sumForUserFeatures=new double[User.featureMap.size()][K];
	    	
	    	for (int i=0;i<users.length;i++) {
	    		User user=users[i];
	    		if (user==null)
	    			continue;
	    		Integer[] features=user.features;
	    		for (int j=0;j<features.length;j++) {
	    			countsForUserFeatures[features[j]]++;
	    			for (int k=0;k<K;k++) {
	    				sumForUserFeatures[features[j]][k]+=fx(fuser[i][k]);
	    			}
	    		}
	    	}
	    }
	    
	}
	
	void loadCurrentFold() {
		training=allData.get(currentFold).get(0);
		testing=allData.get(currentFold).get(1);
		initFeatureVector();
		currentFold++;
	}
	
	 
	double predict(int userid,int movieid)
	{
	    double sum=0;
	    for(int i=0;i<K;i++)
	        sum+=fx(fuser[userid][i])*fy(fmovie[movieid][i]);
	    if (useBias)
	    	sum+=bu[userid]+bi[movieid]+avg;
//	    sum=Math.max(0d,sum);
//	    sum=Math.min(1d,sum);
//	    sum=sigmoid(sum);
	    sum=Function.SigmoidP.f(sum);
	    if (Double.isNaN(sum)) {
	    	System.out.println("Nan");
	    }
	    return sum;
	}
	
	double predict0(int userid,int movieid)
	{
	    double sum=0;
	    for(int i=0;i<K;i++)
	        sum+=fx(fuser[userid][i])*fy(fmovie[movieid][i]);
	    if (useBias)
	    	sum+=bu[userid]+bi[movieid]+avg;
//	    sum=Math.max(0d,sum);
//	    sum=Math.min(1d,sum);
//	    sum=sigmoid(sum);
//	    sum=Function.Sigmoid.f(sum);
	    if (Double.isNaN(sum)) {
	    	System.out.println("Nan");
	    }
	    return sum;
	}
	
	double fx(double x) {
		return useFunction.f(x);
	}
	
	double fy(double y) {
		return movieFunction.f(y);
	}
		
	double fxDiff(double x) {
		return useFunction.diff(x);
	}
	
	double fyDiff(double y) {
		return movieFunction.diff(y);
	}
	
	void updateMovies(int movieid,double[] oldValues) {
		Integer[] genres=movies[movieid].features;
		for (int i=0;i<genres.length;i++) {
			for (int j=0;j<K;j++) {
				sumForMovieFeatures[i][j]=sumForMovieFeatures[i][j]-fy(oldValues[j])+fy(fmovie[movieid][j]);
			}
		}
	}
	
	void updateUsers(int userid,double[] oldValues) {
		Integer[] features=users[userid].features;
		for (int i=0;i<features.length;i++) {
			for (int j=0;j<K;j++) {
				sumForUserFeatures[i][j]=sumForUserFeatures[i][j]-fx(oldValues[j])+fx(fuser[userid][j]);
			}
		}
	}
	 
	void train(int userid,int movieid,double rate)
	{
		double[] oldMovieValues=fmovie[movieid].clone();
		double[] oldUserValues=fuser[userid].clone();
		double r=predict0(userid, movieid);
	    double err=rate-Function.SigmoidP.f(r);
	    double c=Function.SigmoidP.diff(r);
	    if (Double.isNaN(err)) {
	    	System.out.println("Nan");
	    }
	    double sumx=0;
	    if (useSum) {
		    for(int i=0;i<K;i++)
		    {
		    	double fx=fx(fuser[userid][i]);
		    	sumx+=fx;
		    }
	    }
	    if (useBias) {
		    bu[userid]+=lrate3*(err-lambda2*bu[userid]);
		    bi[movieid]+=lrate3*(err-lambda2*bi[movieid]);
	    }
	    for(int i=0;i<K;i++)
	    {
	    	double fx=fx(fuser[userid][i]);
	    	double fxDiff=fxDiff(fuser[userid][i]);
	    	double fy=fy(fmovie[movieid][i]);
	    	double fyDiff=fyDiff(fmovie[movieid][i]);
	        
	        double userDelta=err*fy*c-lambda*fx;
	        if (useSum) {
	        	userDelta-=lambda1*(sumx-1);
	        }
	        double movieDelta=err*fx*c-lambda*fy;
	        if (useMovieInfo) {
	        	double t=0;
	        	Integer[] genres=movies[movieid].features;
	        	for (int j=0;j<genres.length;j++) {
	        		t+=fy(fmovie[movieid][i])-sumForMovieFeatures[genres[j]][i]/countsForMovieFeatures[genres[j]];
	        	}
	        	t/=genres.length;
	        	movieDelta+=lambda3*t;
	        }
	        if (useUserInfo) {
	        	double t=0;
	        	Integer[] features=users[userid].features;
	        	for (int j=0;j<features.length;j++) {
	        		t+=fx(fuser[userid][i])-sumForUserFeatures[features[j]][i]/countsForUserFeatures[features[j]];
	        	}
	        	t/=features.length;
	        	userDelta+=lambda3*t;
	        }
	        fuser[userid][i]+=lrate1*userDelta*fxDiff;
	        fmovie[movieid][i]+=lrate2*movieDelta*fyDiff;
	        
	        
	        if (Double.isNaN(fuser[userid][i])) {
		    	System.out.println("Nan");
		    }
	        if (Double.isNaN(fmovie[movieid][i])) {
		    	System.out.println("Nan");
		    }
	    }
	    
	    if (useMovieInfo)
	    	updateMovies(movieid,oldMovieValues);
	    if (useUserInfo)
	    	updateUsers(userid, oldUserValues);
	}
	 
	void interation()
	{
		Collections.shuffle(training);
	    for (RatingData data:training)
	    {
	        train(data.userid,data.itemid,data.rating);
	    }
		
	}
	 
	double trainRMSE(boolean print)
	{
	    double vrmse=0,err;
	    for (RatingData data:training)
	    {
	        err=(predict(data.userid,data.itemid)-data.rating);
	        vrmse+=err*err;
	    }
	    vrmse=Math.sqrt(vrmse/training.size());
	    if (Double.isNaN(vrmse)) {
	    	System.out.println("Nan");
	    }
	    logger.log("Training Set RMSE:"+vrmse,print);
	    return vrmse;
	}
	 
	double validationRMSE(boolean print)
	{
	    double vrmse=0,err;
	    for(RatingData data:testing)
	    {
	 
	        err=(predict(data.userid,data.itemid)-data.rating);
	        vrmse+=err*err;
	    }
	    vrmse=Math.sqrt(vrmse/testing.size());
	    logger.log("Validation Set RMSE:"+vrmse,print);
	    return vrmse;
	}
	
	void printMovie(int movieid) {
		for (int i=0;i<K;i++) {
			System.out.print(String.format("%1$,.2f", fy(fmovie[movieid][i]))+" ");
		}
		System.out.println();
	}
	
	static public void run(double lrate1,double lrate2, double lambda,double lambda1,double lambda2, double lambda3,Function userFunction, Function movieFunction, int iteration, int fold, boolean print) {
		ENMF enmf=new ENMF();
		enmf.loadData(DATA_PATH+"ratings.dat",fold);
//		sigmoid.loadData("u1.base", "u1.test");
//		enmf.normalize();
		enmf.lrate1=lrate1;
		enmf.lrate2=lrate2;
		enmf.lambda=lambda;
		enmf.lambda1=lambda1;
		if (lambda1==0) {
			enmf.useSum=false;
		}
		enmf.lambda2=lambda2;
		if (lambda2==0) {
			enmf.useBias=false;
		} else {
			enmf.useBias=true;
		}
		if (lambda3==0) {
		} else {
			enmf.loadMovieData(DATA_PATH+"movies.dat");
			enmf.loadUserData(DATA_PATH+"users.dat");
		}
		enmf.lambda3=lambda3;
		enmf.useFunction=userFunction;
		enmf.movieFunction=movieFunction;
		enmf.tRMSE=0;
		enmf.vRMSE=0;
//		enmf.function=false;
	    for (int k=0;k<fold;k++) {
	    	enmf.loadCurrentFold();
		    double tRMSE=1000;
		    double vRMSE=1000;
		    for(int i=0;i<iteration;i++)
		    {
		    	enmf.logger.log("iteration "+i,print);
	
		    	enmf.interation();
		        double newTRMSE=enmf.trainRMSE(print);
		        double newVRMSE=enmf.validationRMSE(print);
	//	        if (tRMSE<newTRMSE)
	//	        	break;
	//	        if (vRMSE<newVRMSE)
	//	        	break;
		        tRMSE=newTRMSE;
		        vRMSE=newVRMSE;
		    }
		    enmf.tRMSE+=tRMSE;
		    enmf.vRMSE+=vRMSE;
	    }
	    enmf.logger.log("finish "+fold +"-fold cross validation");
	    enmf.logger.log("Training Set RMSE:"+enmf.tRMSE/enmf.fold);
	    enmf.logger.log("Validation Set RMSE:"+enmf.vRMSE/enmf.fold);
	}
	
	public static void run(double lrate,double lambda,Function userFunction, Function movieFunction, int iteration, int fold, boolean print) {
		run(lrate,lrate,lambda,0,0,0,userFunction,movieFunction,iteration,fold,print);
	}
	
	public static void run(double lrate1,double lrate2,double lambda,Function userFunction, Function movieFunction, int iteration, int fold, boolean print) {
		run(lrate1,lrate2,lambda,0,0,0,userFunction,movieFunction,iteration,fold,print);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Movie.loadGenres(DATA_PATH+"genres.txt");
		User.loadFeatures(DATA_PATH+"features.txt");
//		double lrate1=1.5;
//		double lrate2=1.5;
//		double lambda=0.02;
//		double lambda1=0;//0.0001; //sum
//		double lambda2=0; //bias
//		double lambda3=0;//0.00005; //movie and user
		double lrate1=1;
		double lrate2=1;
		double lambda=0.02;
		int iteration=250;
		int fold=5;
//		run(lrate1,lrate2,lambda,lambda1,lambda2,lambda3,iteration,fold,true);
		Function userFunction=Function.Sigmoid;//Function.Sigmoid;
		Function movieFunction=Function.Sigmoid;
		run(lrate1,lrate2,lambda,userFunction,movieFunction,iteration,fold,true);
	}

}

enum Function {
	Sigmoid,Square,None,SigmoidP;
	
	public double f(double x) {
		switch (this) {
		case Sigmoid:
			return 1d/(1+Math.exp(-x));
		case Square:
			return x*x;
		case SigmoidP:
			return 1d/(1+Math.exp(-6*(x-.5)));
		default:
			return x;
		}
	}
	
	public double diff(double x) {
		switch (this) {
		case Sigmoid:
			return Math.exp(x)/Math.pow((1+Math.exp(x)),2);
		case Square:
			return 2*x;
		case SigmoidP:
			return 6*Math.exp(x)/Math.pow((1+Math.exp(x)),2);
		default:
			return 1;
		}
	}
	
	public String toString() {
		switch (this) {
		case Sigmoid:
			return "sigmoid";
		case Square:
			return "squrae";
		case None:
			return "None";
		default:
			return "";
		}
	}
}
