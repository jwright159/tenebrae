package wrightway.gdx.tnb;

import android.content.pm.*;
import android.os.*;
import com.badlogic.gdx.backends.android.*;
import java.io.*;
import java.lang.Process;
import android.util.*;
import android.app.*;
import android.content.*;
import java.util.*;
import com.badlogic.gdx.*;

public class MainActivity extends AndroidApplication{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
		
		/*try{
		 Runtime.getRuntime().exec("logcat -c");
		 }catch(IOException e){}*/

		final Thread.UncaughtExceptionHandler exhand = Thread.getDefaultUncaughtExceptionHandler();
		//Log.d("Tenebrae","HANDLER: "+exhand);

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
				@Override
				public void uncaughtException(Thread thread, Throwable e){
					String message = "Error at "+Calendar.getInstance().getTime()+"!\n";
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					wrightway.gdx.Log.error(message += sw.toString());
					pw.close();

					Gdx.files.external("/WrightWay/Tenebrae/error.txt").writeString(message, false);

					Context context = getApplicationContext();
					Intent registerActivity = new Intent(context, CrashReport.class);
					registerActivity.putExtra("message", message);
					registerActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					registerActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
					registerActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(registerActivity);
					
					//Gdx.files.external("/WrightWay/Tenebrae/error.txt").writeString("did a do2", true);
					
					// make sure we die, otherwise the app will hang ...
					//android.os.Process.killProcess(android.os.Process.myPid());
					//exhand.uncaughtException(thread, e);
					System.exit(1);
				}
			});

        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();

        initialize(new MyGdxGame(), cfg);
    }
}
