import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class ENMF {
	static final String DATA_PATH="data"+File.separator;
	
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
	double precision=0.001;
	
	double X[];
	int K=20;
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
	boolean useSigmoid=true;
	boolean useSum=true;
	
	Logger logger=new Logger();
	
	public void normalize() {
		for (RatingData data:training) {
			data.rating=(data.rating-1)/4;
		}
		for (RatingData data:testing) {
			data.rating=(data.rating-1)/4;
		}
	}
	
	public void loadData(String filename) {
		List<List<RatingData>> result=RatingData.split(RatingData.readFile1M(filename),5);
		training=result.get(0);
		testing=result.get(1);
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
	}
	
	public void loadData(String traingFile,String testingFile) {
		training=RatingData.readFile(traingFile);
		testing=RatingData.readFile(testingFile);
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
		logger.log("lrate1="+lrate1+",lrate2="+lrate2+",lambda="+lambda+",lambda1="+lambda1+",lambda2="+lambda2+",lambda3="+lambda3);
		logger.log("use sigmoid: "+useSigmoid);
		logger.log("use bias: "+useBias);
		logger.log("use sum: "+useSum);
		logger.log("use user info: "+useUserInfo);
		logger.log("use movie info: "+useMovieInfo);
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
	    
	    logger.log(userNum+" uses, "+movieNum+" movies");
	     
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
	    
	    if (Double.isNaN(sum)) {
	    	System.out.println("Nan");
	    }
	    return sum;
	}
	
//	double predict0(int userid,int movieid)
//	{
//	    double sum=0;
//	    for(int i=0;i<K;i++)
//	        sum+=fuser[userid][i]*fmovie[movieid][i];
//	    return sum;
//	}
	
	double fx(double x) {
		if (useSigmoid)
			return sigmoid(x);
		else
			return x;
	}
	
	double fy(double y) {
		if (useSigmoid)
			return sigmoid(y);
		else
			return y;
	}
		
	double fxDiff(double x) {
		if (useSigmoid)
			return Math.exp(x)/Math.pow((1+Math.exp(x)),2);
		else
			return 1;
	}
	
	double fyDiff(double y) {
		if (useSigmoid)
			return sigmoidDiff(y);
		else
			return 1;
	}
	
	double sigmoidDiff(double y) {
		double r=Math.exp(y)/Math.pow((1+Math.exp(y)),2);
//		 if (Double.isNaN(r)) {
//		    	r=0;
//		    }
		return r;
	}
	
	double sigmoid(double x) {
		double r= 1d/(1+Math.exp(-x));
		if (Double.isNaN(r)) {
	    	System.out.println("Nan");
	    }
		return r;
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
	    double err=rate-predict(userid, movieid);
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
	        
	        double userDelta=err*fy-lambda*fx;
	        if (useSum) {
	        	userDelta-=lambda1*(sumx-1);
	        }
	        double movieDelta=err*fx-lambda*fy;
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
	    logger.log("Training Set RMSE:"+vrmse,print);
	    return vrmse;
	}
	
	void printMovie(int movieid) {
		for (int i=0;i<K;i++) {
			System.out.print(String.format("%1$,.2f", fy(fmovie[movieid][i]))+" ");
		}
		System.out.println();
	}
	
	static public void run(double lrate1,double lrate2, double lambda,double lambda1,double lambda2, double lambda3,int iteration, boolean print) {
		ENMF sigmoid=new ENMF();
		sigmoid.loadData(DATA_PATH+"ratings.dat");
//		sigmoid.loadData("u1.base", "u1.test");
		sigmoid.normalize();
		sigmoid.lrate1=lrate1;
		sigmoid.lrate2=lrate2;
		sigmoid.lambda=lambda;
		sigmoid.lambda1=lambda1;
		if (lambda2==0) {
			sigmoid.useBias=false;
		} else {
			sigmoid.useBias=true;
			sigmoid.lambda2=lambda2;
		}
		if (lambda3==0) {
		} else {
			sigmoid.lambda3=lambda3;
			sigmoid.loadMovieData(DATA_PATH+"movies.dat");
			sigmoid.loadUserData(DATA_PATH+"users.dat");
		}
	    sigmoid.initFeatureVector();
	    double tRMSE=1000;
	    double vRMSE=1000;
	    for(int i=0;i<iteration;i++)
	    {
	    	sigmoid.logger.log("iteration "+i,print);

	    	sigmoid.interation();
	        double newTRMSE=sigmoid.trainRMSE(print);
	        double newVRMSE=sigmoid.validationRMSE(print);
//	        if (tRMSE<newTRMSE)
//	        	break;
//	        if (vRMSE<newVRMSE)
//	        	break;
	        tRMSE=newTRMSE;
	        vRMSE=newVRMSE;
	    }
	    sigmoid.logger.log("finish");
	    sigmoid.trainRMSE(true);
	    sigmoid.validationRMSE(true);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Movie.loadGenres(DATA_PATH+"genres.txt");
		User.loadFeatures(DATA_PATH+"features.txt");
		double lrate1=1.5;
		double lrate2=1.5;
		double lambda=0.02;
		double lambda1=0.0001; //sum
		double lambda2=0; //bias
		double lambda3=0.00005; //movie and user
		int iteration=200;
		run(lrate1,lrate2,lambda,lambda1,lambda2,lambda3,iteration,true);

	}

}

