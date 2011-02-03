/*
 * Copied from APIDemos
 */

package graphics.shaders;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

// Debugging
import android.util.Log;

/**
 * This sample shows how to check for OpenGL ES 2.0 support at runtime, and then
 * use either OpenGL ES 1.0 or OpenGL ES 2.0, as appropriate.
 */
public class ShaderActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a new GLSurfaceView - this holds the GL Renderer
        mGLSurfaceView = new GLSurfaceView(this);
        
        // detect if OpenGL ES 2.0 support exists - if it doesn't, exit.
        mGLSurfaceView.setEGLContextClientVersion(2);
        renderer = new Renderer(this);
        mGLSurfaceView.setRenderer(renderer);
        /*if (detectOpenGLES20()) {
            // Tell the surface view we want to create an OpenGL ES 2.0-compatible
            // context, and set an OpenGL ES 2.0-compatible renderer.
            mGLSurfaceView.setEGLContextClientVersion(2);
            renderer = new Renderer(this);
            mGLSurfaceView.setRenderer(renderer);
        } 
        else { // quit if no support
        	this.finish();
        }*/
        /*else {
            // Give the user an error saying that it is not supported
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("Your crappy phone does not support OpenGL ES 2.0. Click ok to close the application")
        	       .setCancelable(false)
        	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        	    	   // Just close the activity
        	           public void onClick(DialogInterface dialog, int id) {
        	                ShaderActivity.this.finish();
        	           }
        	       });
        	       /*.setNegativeButton("No", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	AlertDialog alert = builder.create();
        	alert.show();
        }*/
        setContentView(mGLSurfaceView);
    }

    /**
     * Detects if OpenGL ES 2.0 exists
     * @return true if it does
     */
    private boolean detectOpenGLES20() {
        ActivityManager am =
            (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        Log.d("OpenGL Ver:", info.getGlEsVersion());
        return (info.reqGlEsVersion >= 0x20000);
    }

    /*@Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        //super.onResume();
        //mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        //super.onPause();
        //mGLSurfaceView.onPause();
    }*/

    /************
     *  MENU FUNCTIONS
     **********/
    /*
     * Creates the menu and populates it via xml
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.game_menu, menu);
        //menu.add(0, 3, 0, "Game Settings...");
        return true;
    }
    
    /*
     * On selection of a menu item
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	        case R.id.gouraud: 			// Gouraud Shading
	            renderer.setShader(0);
	            return true;
	        case R.id.phong: 			// Phong Shading
	        	renderer.setShader(1);
	            return true;
	        case R.id.normal_map:		// Normal Mapping
	        	renderer.setShader(2);
	            return true;
	        case R.id.quit:
	            quit();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
    
    // Quit the app
    private void quit() {
    	//super.onDestroy();
    	this.finish();
    }
    
    /********************************
     * PROPERTIES
     *********************************/
     
    private GLSurfaceView mGLSurfaceView;
    
    // The Renderer
    Renderer renderer;
}
