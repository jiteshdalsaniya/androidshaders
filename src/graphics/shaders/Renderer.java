package graphics.shaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.StringTokenizer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

class Renderer implements GLSurfaceView.Renderer {
	/******************************
	 * PROPERTIES
	 ******************************/
	// rotation 
	public float mAngleX;
    public float mAngleY;

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 6 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_NOR_OFFSET = 3;

	// shader constants
	private final int GOURAUD_SHADER = 0;
	private final int PHONG_SHADER = 1;
	private final int NORMALMAP_SHADER = 2;
	//private static final int GOURAUD_SHADER = 0;

	// array of shaders
	Shader _shaders[] = new Shader[3];
	private int _currentShader;

	/** Shader code **/
	private int[] vShaders;
	private int[] fShaders;

	// object constants
	private final int OCTAHEDRON = 0;
	private final int TETRAHEDRON = 1;
	private final int CUBE = 2;

	// The objects
	Object3D[] _objects = new Object3D[3];

	// current object
	private int _currentObject;

	// Modelview/Projection matrices
	private float[] mMVPMatrix = new float[16];
	private float[] mProjMatrix = new float[16];
	private float[] mScaleMatrix = new float[16];   // scaling
	private float[] mRotXMatrix = new float[16];	// rotation x
	private float[] mRotYMatrix = new float[16];	// rotation x
	private float[] mMMatrix = new float[16];		// rotation
	private float[] mVMatrix = new float[16]; 		// modelview
	private float[] normalMatrix = new float[16]; 	// modelview normal
	
	// light parameters
	private float[] lightPos;
	private float[] lightColor;
	private float[] lightAmbient;
	private float[] lightDiffuse;
	
	// material properties
	private float[] matAmbient;
	private float[] matDiffuse;
	private float[] matSpecular;
	private float matShininess;
	
	// eye pos
	private float[] eyePos = {-5.0f, 0.0f, 0.0f};
	
	// scaling
	float scaleX = 1.0f;
	float scaleY = 1.0f;
	float scaleZ = 1.0f;
	
	private Context mContext;
	private static String TAG = "GLES20TriangleRenderer";

	/***************************
	 * CONSTRUCTOR(S)
	 **************************/
	public Renderer(Context context) {

		mContext = context;

		// setup all the shaders
		vShaders = new int[3];
		fShaders = new int[3];
		// basic - just gouraud shading
		vShaders[0] = R.raw.vshader_basic;
		fShaders[0] = R.raw.pshader_basic;


		// Create some objects...
		// Octahedron - WORKS!
		try {
			_objects[0] = new Object3D(R.raw.octahedron, false, context);
			_objects[1] = new Object3D(R.raw.tetrahedron, false, context);
			_objects[2] = new Object3D(R.raw.cube, false, context);
		} catch (Exception e) {
			showAlert("" + e.getMessage());
		}
		
		// set current object and shader
		_currentObject = this.OCTAHEDRON;
		_currentShader = this.GOURAUD_SHADER;
	}

	/*****************************
	 * GL FUNCTIONS
	 ****************************/
	/*
	 * Draw function - called for every frame
	 */
	public void onDrawFrame(GL10 glUnused) {
		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.
		GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
		GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		// the current shader
		Shader shader = _shaders[this._currentShader]; // PROBLEM!

		// Start using the shader
		GLES20.glUseProgram(shader.get_program());
		checkGlError("glUseProgram");

		// GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
	
		// MODELVIEW MATRIX
		long time = SystemClock.uptimeMillis() % 4000L;
		float angle = 0.090f * ((int) time);
		
		// scaling
		Matrix.setIdentityM(mScaleMatrix, 0);
		Matrix.scaleM(mScaleMatrix, 0, scaleX, scaleY, scaleZ);
		
		// Rotation along x
		Matrix.setRotateM(mRotXMatrix, 0, this.mAngleY, -1.0f, 0.0f, 0.0f);
		Matrix.setRotateM(mRotYMatrix, 0, this.mAngleX, 0.0f, 1.0f, 0.0f);
		
		float tempMatrix[] = new float[16]; 
		Matrix.multiplyMM(tempMatrix, 0, mRotYMatrix, 0, mRotXMatrix, 0);
		Matrix.multiplyMM(mMMatrix, 0, mScaleMatrix, 0, tempMatrix, 0);
		//Matrix.setRotateM(mMMatrix, 0, angle, 1.0f, 0.0f, 0.0f);
		//Matrix.scaleM(mMMatrix, 0, 100.0f, 100.0f, 100.0f);
		Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
		
		// send to the shader
		GLES20.glUniformMatrix4fv(shader.muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Create the normal modelview matrix
		// Invert + transpose of mvpmatrix
		Matrix.invertM(normalMatrix, 0, mMVPMatrix, 0);
		Matrix.transposeM(normalMatrix, 0, normalMatrix, 0);
		
		// send to the shader
		GLES20.glUniformMatrix4fv(shader.normalMatrixHandle, 1, false, mMVPMatrix, 0);
		
		// lighting variables
		// send to shaders
		GLES20.glUniform4fv(_shaders[this._currentShader].lightPosHandle, 1, lightPos, 0);
		GLES20.glUniform4fv(_shaders[this._currentShader].lightColorHandle, 1, lightColor, 0);
	
		// material 
		GLES20.glUniform4fv(_shaders[this._currentShader].matAmbientHandle, 1, matAmbient, 0);
		GLES20.glUniform4fv(_shaders[this._currentShader].matDiffuseHandle, 1, matDiffuse, 0);
		GLES20.glUniform4fv(_shaders[this._currentShader].matSpecularHandle, 1, matSpecular, 0);
		GLES20.glUniform1f(_shaders[this._currentShader].matShininessHandle, matShininess);
		
		// eyepos
		GLES20.glUniform3fv(_shaders[this._currentShader].eyeHandle, 1, eyePos, 0);
		
		/*** DRAWING OBJECT **/
		// Get buffers from mesh
		Object3D ob = this._objects[this._currentObject];
		Mesh mesh = ob.getMesh();
		FloatBuffer _vb = mesh.get_vb();
		ShortBuffer _ib = mesh.get_ib();
		
		short[] _indices = mesh.get_indices();
		
		// Textures
		// has texture?
		GLES20.glUniform1f(shader.hasTextureHandle, ob.hasTexture() ? 1 : 0);
	
		// the vertex coordinates
		_vb.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(shader.maPositionHandle, 3, GLES20.GL_FLOAT, false,
				 TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _vb);
		GLES20.glEnableVertexAttribArray(shader.maPositionHandle);
		 
		// the normal info
		_vb.position(TRIANGLE_VERTICES_DATA_NOR_OFFSET);
		GLES20.glVertexAttribPointer(shader.maNormalHandle, 3, GLES20.GL_FLOAT, false,
				 TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _vb);
		GLES20.glEnableVertexAttribArray(shader.maNormalHandle);
		
		// Draw with indices
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, _indices.length, GLES20.GL_UNSIGNED_SHORT, _ib);
		checkGlError("glDrawElements");
        
		/** END DRAWING OBJECT ***/
	}

	/*
	 * Called when viewport is changed
	 * @see android.opengl.GLSurfaceView$Renderer#onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)
	 */
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Ignore the passed-in GL10 interface, and use the GLES20
		// class's static methods instead.
		GLES20.glViewport(0, 0, width, height);
		float ratio = (float) width / height;
		//Matrix.frustumM(mProjMatrix, 0, -5, 5, -1, 1, 0.5f, 6.0f);
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 0.5f, 11);
	}

	/**
	 * Initialization function
	 */
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// Generate all the shader programs
		// initialize shaders - PROBLEM!
		try {
			_shaders[0] = new Shader(vShaders[0], fShaders[0], mContext, false, 0);
			_shaders[1] = new Shader(vShaders[this._currentShader], fShaders[this._currentShader], mContext, false, 0);
		} catch (Exception e) {
			Log.d("SHADER 0 SETUP", e.getLocalizedMessage());
		}
		
		//GLES20.glEnable   ( GLES20.GL_DEPTH_TEST );
		GLES20.glDepthFunc( GLES20.GL_LEQUAL     );
		GLES20.glDepthMask( true );
		
		// cull backface
		GLES20.glEnable( GLES20.GL_CULL_FACE );
		GLES20.glCullFace(GLES20.GL_BACK); // Should be culling GL_BACK though - fix meshes?
		
		// light variables
		float[] lightP = {3.0f, 3.0f, -3.0f, 1};
		this.lightPos = lightP;
		
		float[] lightC = {1.0f, 0.5f, 0.5f};
		this.lightColor = lightC;
		
		//float[] lA = {
		//private float[] lightAmbient;
		//private float[] lightDiffuse;
		
		// material properties
		float[] mA = {1.0f, 0.5f, 0.5f, 1.0f};
		matAmbient = mA;
		
		float[] mD = {1.0f, 0.5f, 0.5f, 1.0f};
		matDiffuse = mD;
		
		float[] mS =  {1.0f, 1.0f, 1.0f, 1.0f};
		matSpecular = mS;
		
		matShininess = 5.0f;
		
		// send to shaders
		//GLES20.glUniform3fv(_shaders[this._currentShader].lightPosHandle, 1, lightPos, 0);
		//GLES20.glUniform3fv(_shaders[this._currentShader].lightColorHandle, 1, lightColor, 0);
		GLES20.glClearDepthf(1.0f);
		//GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthFunc(GLES20.GL_EQUAL);//.enable(gl.DEPTH_TEST);
		
		// set the view matrix
		Matrix.setLookAtM(mVMatrix, 0, 0, 0, -5.0f, 0.0f, 0f, 0f, 0f, 1.0f, 0.0f);
	}

	/**************************
	 * OTHER METHODS
	 *************************/

	/**
	 * Changes the shader based on menu selection
	 * @param represents the other shader 
	 */
	public void setShader(int shader) {
		_currentShader = shader;
	}

	/**
	 * Changes the object based on menu selection
	 * @param represents the other object 
	 */
	public void setObject(int object) {
		_currentObject = object;
	
		// setup texture?
		//_objects[_currentObject].setupTexture(mContext);
	}

	/**
	 * Scaling
	 */
	public void increaseScale() {
		scaleX += 0.01f;
		scaleY += 0.01f;
		scaleZ += 0.01f;
	}
	
	public void decreaseScale() {
		scaleX -= 0.01f;
		scaleY -= 0.01f;
		scaleZ -= 0.01f;
		
	}
	
	public void defaultScale() {
		scaleX = 1f;
		scaleY = 1f;
		scaleZ = 1f;
	}
	
	// debugging opengl
	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}


	// debugging
	private void showAlert(String alert2) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
		builder.setMessage(alert2)
		.setCancelable(false)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			// Just close the activity
			public void onClick(DialogInterface dialog, int id) {
				//Renderer.this.mContext.finish();
			}
		});
		/*.setNegativeButton("No", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	                dialog.cancel();
    	           }
    	       });*/
		AlertDialog alert = builder.create();
		alert.show();
	}

	
	/******************* UNNEEDED CODE *************/
	
	
	/*
	 * Create our texture(s). This has to be done each time the
	 * surface is created.
	 */
	//if ( _objects[this._currentObject].hasTexture())
	//_objects[this._currentObject].setupTexture(mContext);
	//for (int i = 0; i < _objects.length; i++)
	//_objects[i].setupTexture(mContext);

	/*int[] textures = new int[1];
    GLES20.glGenTextures(1, textures, 0);

    mTextureID = textures[0];
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);

    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST);
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR);

    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_REPEAT);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_REPEAT);

    InputStream is = mContext.getResources()
        .openRawResource(R.raw.robot);
    Bitmap bitmap;
    try {
        bitmap = BitmapFactory.decodeStream(is);
    } finally {
        try {
            is.close();
        } catch(IOException e) {
            // Ignore.
        }
    }

    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    bitmap.recycle();*/
	
	// TEST DATA
	/* 
	 * private final float[] mTriangleVerticesData = {
			// X, Y, Z, U, V
			-1.0f, -0.5f, 0, -0.5f, 0.0f,
			1.0f, -0.5f, 0, 1.5f, -0.0f,
			0.0f,  1.11803399f, 0, 0.5f,  1.61803399f };

		private FloatBuffer mTriangleVertices;
	 */
}
