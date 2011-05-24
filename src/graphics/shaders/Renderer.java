/** 
 * The OpenGL renderer
 */

package graphics.shaders;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

class Renderer implements GLSurfaceView.Renderer {
	/******************************
	 * PROPERTIES
	 ******************************/
	// rotation 
	public float mAngleX;
	public float mAngleY;

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 8 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_NOR_OFFSET = 3;
	private static final int TRIANGLE_VERTICES_DATA_TEX_OFFSET = 6;

	// shader constants
	private final int GOURAUD_SHADER = 0;
	private final int PHONG_SHADER = 1;
	private final int NORMALMAP_SHADER = 2;
	private final int DEPTHMAP_SHADER = 3; // generates the depth map

	// array of shaders
	Shader _shaders[] = new Shader[4];
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
	// The plane (to display the shadow)
	Object3D _plane;
	
	// the full-screen quad buffers
	final float x = 10.0f;
	final float y = 15.0f;
	final float z = 2.0f;
	
	// vertex information - clockwise
						   // x, y, z, nx, ny, nz, u, v
	final float _quadv[] = { -x, -y, z, 0, 0, -1, 0, 0,
							 -x,  y, z, 0, 0, -1, 0, 1,
							  x,  y, z, 0, 0, -1, 1, 1,
							  x, -y, z, 0, 0, -1, 1, 0
						   };

	private FloatBuffer _qvb;
	
	// index
	final short _quadi[] = { 0, 1, 2,
			              2, 3, 0  
						};
	private ShortBuffer _qib;
	
	// current object
	private int _currentObject;

	// Modelview/Projection matrices for camera
	private float[] mMVPMatrix = new float[16];
	private float[] mProjMatrix = new float[16];
	private float[] mScaleMatrix = new float[16];   // scaling
	private float[] mRotXMatrix = new float[16];	// rotation x
	private float[] mRotYMatrix = new float[16];	// rotation x
	private float[] mMMatrix = new float[16];		// rotation
	private float[] mVMatrix = new float[16]; 		// modelview
	private float[] normalMatrix = new float[16]; 	// modelview normal

	// Matrices for the light
	private float[] lMVPMatrix = new float[16];
	private float[] lProjMatrix = new float[16];
	private float[] lMMatrix = new float[16];		// rotation
	private float[] lMVMatrix = new float[16]; 		// modelview
	
	
	// list of textures
	private int[] _texIDs;

	// light parameters
	//private float[] lightPos; // not really used anymore
	private float[] lightPos = { 17.0f, 20.0f, 10.0f, 1,   // position
								  0.0f, 0.0f,  0.0f,       // center (where the light is looking at)
								  0.0f, 1.0f,  0.0f,       // up vector
	};
	private float[] lightColor;
	
	// angle rotation for light
	float angle = 0.0f;
	boolean lightRotate = true; 


	// material properties
	private float[] matAmbient;
	private float[] matAmbient2;
	private float[] matDiffuse;
	private float[] matSpecular;
	private float matShininess;

	// eye pos
	private float[] eyePos = {-5.0f, 0.0f, 0.0f};
	float eyeView[] = { 0.0f, 0.0f, -5.0f,  // eyePosition
 						0.0f, 0.0f, 0.0f,   // center (where it's looking at
 						0.0f, 1.0f, 0.0f    // up vector
	};
	
	// scaling
	float scaleX = 1.0f;
	float scaleY = 1.0f;
	float scaleZ = 1.0f;

	// RENDER TO TEXTURE VARIABLES
	int[] fb, depthRb, renderTex;
	final int texW = 512;//480;
	final int texH = 512;//800;
	IntBuffer texBuffer;
	
	
	// viewport variables
	float ratio = 1.0f;
	int w, h;
	
	// GAME LOOP variables
	final int TICKS_PER_SECOND = 25; // Update "game" info at 60fps - will ensure light doesn't rotate too fast
    final int SKIP_TICKS = 1000 / TICKS_PER_SECOND;
    final int MAX_FRAMESKIP = 10;
    float lastTime = 0.0f;
    float next_game_tick;// = System.currentTimeMillis();//GetTickCount();
    int loops;

    // Variables which can be toggled
    private boolean viewDepthTex = false; 	// Render the depth texture or not?
	private boolean enableTexture = true;	// textures for objects enabled?
	private boolean viewShadows = true;		// Should shadows be visible?
    
	private Context mContext;
	private static String TAG = "Renderer";

	/***************************
	 * CONSTRUCTOR(S)
	 **************************/
	public Renderer(Context context) {

		mContext = context;

		// setup all the shaders
		vShaders = new int[4];
		fShaders = new int[4];

		// basic - just gouraud shading
		vShaders[GOURAUD_SHADER] = R.raw.gouraud_vs;
		fShaders[GOURAUD_SHADER] = R.raw.gouraud_ps;

		// phong shading
		vShaders[PHONG_SHADER] = R.raw.phong_vs;
		fShaders[PHONG_SHADER] = R.raw.phong_ps;

		// normal mapping
		vShaders[NORMALMAP_SHADER] = R.raw.normalmap_vs;
		fShaders[NORMALMAP_SHADER] = R.raw.normalmap_ps;

		// Depth map
		vShaders[DEPTHMAP_SHADER] = R.raw.depth_vs;
		fShaders[DEPTHMAP_SHADER] = R.raw.depth_ps;
		
		// Create some objects - pass in the textures, the meshes
		try {
			int[] normalMapTextures = {R.raw.diffuse_old, R.raw.diffusenormalmap_deepbig};
			_objects[0] = new Object3D(R.raw.octahedron, false, context);
			_objects[1] = new Object3D(R.raw.dragon, false, context);
			_objects[2] = new Object3D(normalMapTextures, R.raw.texturedcube, true, context);
			
			// create the plane
			_plane = new Object3D(R.raw.plane, false, context);
		} catch (Exception e) {
			//showAlert("" + e.getMessage());
		}

		// set current object and shader
		_currentObject = this.OCTAHEDRON;
		_currentShader = this.PHONG_SHADER;
	}

	
	/*****************************
	 * GL FUNCTIONS
	 ****************************/
	/*
	 * Draw function - called for every frame
	 */
	public void onDrawFrame(GL10 glUnused) {
		
		// Check system time at the beginning
		//long startTime = System.currentTimeMillis();
		
		// Rotate the light?
    	//if (lightRotate) {
    	//	rotateLight();
		//}
		
        // Render shadows or not?
        if (viewShadows) {
	        // This will require multiple passes
	        
	        // First pass:
	        // Render the depth texture from the viewpoint of the light
	        renderDepthToTexture();
        }
        else {
        	// regular render
        	regularRender();
        }
        
        // check system time at the end (for framerate limit)
        /*long endTime = System.currentTimeMillis();
        long deltaT = endTime - startTime;
        if (deltaT < 33) // sleep to keep framerate at 30
        	SystemClock.sleep(33-deltaT);*/
        
        
	} // END DRAW FUNCTION

	/**
	 * Draws one object
	 * @param Object3D the object to draw - uses index buffer
	 * @param _program the shader program
	 * @param _sendNormals should normals be sent in?
	 * @param _sendTextures should textures be sent in?
	 */
	void drawObject(Object3D ob, int _program, boolean _sendNormals, boolean _sendTextures) {
		/*** DRAWING OBJECT **/
		// Get buffers from mesh
		Mesh mesh = ob.getMesh();
		FloatBuffer _vb = mesh.get_vb();
		ShortBuffer _ib = mesh.get_ib();

		short[] _indices = mesh.get_indices();

		// Vertex buffer

		// the vertex coordinates
		_vb.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(_program, "aPosition"), 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _vb);
		GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(_program, "aPosition"));

		// the normal info
		if (_sendNormals) {
			_vb.position(TRIANGLE_VERTICES_DATA_NOR_OFFSET);
			GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(_program, "aNormal"), 3, GLES20.GL_FLOAT, false,
					TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _vb);
			GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(_program, "aNormal"));
		}

		// Texture info

		// bind textures
		if (ob.hasTexture() && _sendTextures) {
			// number of textures
			int[] texIDs = ob.get_texID(); 
			
			for(int i = 0; i < _texIDs.length; i++) {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i + 1);
				////Log.d("TEXTURE BIND: ", i + " " + texIDs[i]);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIDs[i]);
				GLES20.glUniform1i(GLES20.glGetUniformLocation(_program, "texture" + (i+1)), i+1);
			}
		}

		// enable texturing? [fix - sending float is waste]
		if (_sendTextures) {
			GLES20.glUniform1f(GLES20.glGetUniformLocation(_program, "hasTexture"), ob.hasTexture() && enableTexture ? 2.0f : 0.0f);

			// texture coordinates
			_vb.position(TRIANGLE_VERTICES_DATA_TEX_OFFSET);
			GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(_program, "textureCoord"), 2, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _vb);
			GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(_program, "textureCoord"));
		}
		
		// Draw with indices
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, _indices.length, GLES20.GL_UNSIGNED_SHORT, _ib);
		checkGlError("glDrawElements");
		
		
	}
	
	/**
	 * Draws all the objects to draw
	 * @param _program the shader program
	 * @param _sendNormals should normals be sent in?
	 */
	void drawAllObjects(int _program, boolean _sendNormals, boolean _sendTextures) {
		// Draw the plane first
		//if (_sendNormals) // only draw when depth test not needed
		if (_sendNormals) // drawing phong
			GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matAmbient"), 1, matAmbient2, 0);
		
		drawObject(_plane, _program, _sendNormals, _sendTextures);
		
		// Draw the other object
		if (_sendNormals) // drawing phong
			GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matAmbient"), 1, matAmbient, 0);
		drawObject(this._objects[this._currentObject], _program, _sendNormals, _sendTextures);
	}
	
	/**
	 * Renders to a texture
	 */
	private boolean renderDepthToTexture() {
		// Cull front faces for shadow generation
		GLES20.glCullFace(GLES20.GL_FRONT); 
		
		// much bigger viewport?
		//Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 0.5f, 10);
		GLES20.glViewport(0, 0, this.texW, this.texH);
		
		// bind the generated framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
		
		// specify texture as color attachment
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTex[0], 0);
		
		// attach render buffer as depth buffer
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRb[0]);
		
		// check status
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE)
			return false;

		/*** DRAW ***/
		// Clear color and buffers
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		// depth map shaders
		Shader shader = _shaders[this.DEPTHMAP_SHADER];
		int _program = shader.get_program();
		
		// Start using the shader
		GLES20.glUseProgram(_program);
		checkGlError("glUseProgram");

		
		
		// Setup ModelViewProjectionMatrix
		
		// View from the light's perspective (TODO: Does this turn it into a directional light? Generate cube map for point light)
		Matrix.setLookAtM(lMVMatrix, 0, lightPos[0], lightPos[1], lightPos[2], 
									    lightPos[4], lightPos[5], lightPos[6],
									    lightPos[7], lightPos[8], lightPos[9]);
		float ratio2 = (float)texW /texH;
		//Matrix.frustumM(lProjMatrix, 0, -ratio2, ratio2, -1, 1, 1f, 5000f);
		Matrix.frustumM(lProjMatrix, 0, -ratio, ratio, -1, 1, 1f, 40000);
		
		// modelviewprojection matrix
		Matrix.multiplyMM(lMVPMatrix,0, lProjMatrix, 0, lMVMatrix, 0);
		
		// send to the shader
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "uMVPMatrix"), 1, false, lMVPMatrix, 0);

		/// DRAW ALL THE OBJECTS 
		drawAllObjects(_program, false, false);
		
		// render the depth buffer?
		if (viewDepthTex) {
			renderToQuad();
			return true;
		}
		
		/**** Else, render with shadow now --
		 *  Steps:
		 *    -Render the scene as usual
		 *    -Pass in depth map
		 *    -Project the depth map onto the scene
		 *    -Compare depth values to see if pixel is visible (in shadow or not?)
		 */
		renderWithShadow(lMVPMatrix);
		
		/** END DRAWING OBJECT ***/
		return true;
	}
	
	/**
	 * Renders the scene with the shadow 
	 * 2nd pass
	 * @param lightMVPMatrix the light modelviewprojection matrix
	 */
	private void renderWithShadow(float[] lightMVPMatrix) {
		//Log.d("SHADOWRENDER", "Beginning");
		
		// bind default framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		
		// backface culling
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		//GLES20.glCullFace(GLES20.GL_BACK);
		
		// Clear the depth buffer
		GLES20.glClearColor(.1f, .1f, .1f, 1.0f);
		GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		// the current shader
		Shader shader = _shaders[this._currentShader]; 
		int _program = shader.get_program();
		
		// Start using the shader
		GLES20.glUseProgram(_program);
		checkGlError("glUseProgram");

		// revert to regular viewport
		GLES20.glViewport(0, 0, w, h);
		ratio = (float) w / h;
		
		//Log.d("SHADOWRENDER", "Middle1");
		
		// View from the eye's perspective
		Matrix.setLookAtM(mVMatrix, 0, eyeView[0], eyeView[1], eyeView[2], 
									   eyeView[3], eyeView[4], eyeView[5],
									   eyeView[6], eyeView[7], eyeView[8]);
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 0.5f, 5000);
		
		// scaling
		Matrix.setIdentityM(mScaleMatrix, 0);
		Matrix.scaleM(mScaleMatrix, 0, scaleX, scaleY, scaleZ);

		// Rotation along x
		Matrix.setRotateM(mRotXMatrix, 0, this.mAngleY, -1.0f, 0.0f, 0.0f);
		Matrix.setRotateM(mRotYMatrix, 0, this.mAngleX, 0.0f, 1.0f, 0.0f);

		// Set the ModelViewProjectionMatrix
		float tempMatrix[] = new float[16]; 
		Matrix.multiplyMM(tempMatrix, 0, mRotYMatrix, 0, mRotXMatrix, 0);
		Matrix.multiplyMM(mMMatrix, 0, mScaleMatrix, 0, tempMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

		//Log.d("SHADOWRENDER", "Middle2");
		
		// send to the shader
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "uMVPMatrix"), 1, false, mMVPMatrix, 0);

		// Create the normal modelview matrix
		// Invert + transpose of mvpmatrix
		Matrix.invertM(normalMatrix, 0, mMVPMatrix, 0);
		Matrix.transposeM(normalMatrix, 0, normalMatrix, 0);

		// send to the shader
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "normalMatrix"), 1, false, normalMatrix, 0);//mMVPMatrix, 0);

		// lighting variables
		
		// invert cameraview matrix
		float shadowProjMatrix[] = new float[16];

		Matrix.invertM(shadowProjMatrix, 0, mVMatrix, 0); // just the view matrix or modelviewprojection matrix?
		Matrix.multiplyMM(shadowProjMatrix, 0, lightMVPMatrix, 0, shadowProjMatrix, 0);
		
		// send to shaders
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "lightPos"), 1, lightPos, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "lightColor"), 1, lightColor, 0);

		// send the shadow projection matrix
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "shadowProjMatrix"), 1, false, lightMVPMatrix, 0); 
		//GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "shadowProjMatrix"), 1, false, shadowProjMatrix, 0);
		
		// material 
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matAmbient"), 1, matAmbient, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matDiffuse"), 1, matDiffuse, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matSpecular"), 1, matSpecular, 0);
		GLES20.glUniform1f(GLES20.glGetUniformLocation(_program, "matShininess"), matShininess);

		// send the depth texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[0]);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(_program, "shadowTexture"), 0);
		
		
		//Log.d("SHADOWRENDER", "Middle3");
		
		// eye position
		GLES20.glUniform3fv(GLES20.glGetUniformLocation(_program, "eyePos"), 1, eyePos, 0); // send in eyePos variable instead?
		
		//Log.d("SHADOWRENDER", "Middle4");
		
		/// DRAW ALL THE OBJECTS 
		drawAllObjects(_program, true, true);
		
		
		//Log.d("SHADOWRENDER", "End");
	}
	
	/**
	 * Renders the texture to a quad
	 */
	private void renderToQuad() {
		// bind default framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		
		// backface culling
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK); 
		
		GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
		GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		// RENDER A FULL-SCREEN QUAD
		
		// use gouraud shader to render it?
		Shader shader = _shaders[this.GOURAUD_SHADER];
		int _program = shader.get_program();
		
		// Start using the shader
		GLES20.glUseProgram(_program);
		checkGlError("glUseProgram");

		// set the viewport
		GLES20.glViewport(0, 0, w, h);
		
		// View from the eye's perspective
		Matrix.setLookAtM(mVMatrix, 0, 0.0f, 0.0f, -5.0f,//eyeView[0], eyeView[1], eyeView[2], 
									   eyeView[3], eyeView[4], eyeView[5],
									   eyeView[6], eyeView[7], eyeView[8]);
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 0.5f, 10);
		
		// scaling
        Matrix.setIdentityM(mScaleMatrix, 0);
        //Matrix.scaleM(mScaleMatrix, 0, scaleX, scaleY, scaleZ);

        // Rotation along x
        Matrix.setRotateM(mRotXMatrix, 0, 0, -1.0f, 0.0f, 0.0f);
        Matrix.setRotateM(mRotYMatrix, 0, 0, 0.0f, 1.0f, 0.0f);

        // Set the ModelViewProjectionMatrix
        float[] tempMatrix = new float[16]; 
        Matrix.multiplyMM(tempMatrix, 0, mRotYMatrix, 0, mRotXMatrix, 0);
        Matrix.multiplyMM(mMMatrix, 0, mScaleMatrix, 0, tempMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
		
		// send to the shader
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "uMVPMatrix"), 1, false, mMVPMatrix, 0);

		// Create the normal modelview matrix
		// Invert + transpose of mvpmatrix
		Matrix.invertM(normalMatrix, 0, mMVPMatrix, 0);
		Matrix.transposeM(normalMatrix, 0, normalMatrix, 0);

		// send to the shader
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "normalMatrix"), 1, false, mMVPMatrix, 0);

		// lighting variables
		// send to shaders
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "lightPos"), 1, lightPos, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "lightColor"), 1, lightColor, 0);

		// material 
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matAmbient"), 1, matAmbient, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matDiffuse"), 1, matDiffuse, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matSpecular"), 1, matSpecular, 0);
		GLES20.glUniform1f(GLES20.glGetUniformLocation(_program, "matShininess"), matShininess);

		// eye position
		GLES20.glUniform3fv(GLES20.glGetUniformLocation(_program, "eyePos"), 1, eyePos, 0);

		// Vertex buffer

		// the vertex coordinates
		_qvb.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(_program, "aPosition"), 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _qvb);
		GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(_program, "aPosition"));

		// the normal info
		_qvb.position(TRIANGLE_VERTICES_DATA_NOR_OFFSET);
		GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(_program, "aNormal"), 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _qvb);
		GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(_program, "aNormal"));

		// bind the framebuffer texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[0]);
		GLES20.glUniform1i(GLES20.glGetUniformLocation(_program, "texture1"), 0);

		// enable texturing? [TODO: fix - sending float is waste]
		GLES20.glUniform1f(GLES20.glGetUniformLocation(_program, "hasTexture"), 1.0f);

		// texture coordinates
		_qvb.position(TRIANGLE_VERTICES_DATA_TEX_OFFSET);
		GLES20.glVertexAttribPointer(GLES20.glGetAttribLocation(_program, "textureCoord"), 2, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, _qvb);
		GLES20.glEnableVertexAttribArray(GLES20.glGetAttribLocation(_program, "textureCoord"));//GLES20.glEnableVertexAttribArray(shader.maTextureHandle);

		// Draw with indices
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, _quadi.length, GLES20.GL_UNSIGNED_SHORT, _qib); // NOTE: On some devices GL_UNSIGNED_SHORT works 
		checkGlError("glDrawElements");

		////Log.d("End Render Texture", "Rendered texture");
		
		
	}
	
	
	/**
	 * Render the objects
	 */
	private void regularRender() {
		// bind default framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		
		GLES20.glClearColor(.0f, .0f, .0f, 1.0f);
		GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		
		// the current shader
		Shader shader = _shaders[this._currentShader]; // PROBLEM!
		int _program = shader.get_program();
		
		// Start using the shader
		GLES20.glUseProgram(_program);
		checkGlError("glUseProgram");

		// scaling
		Matrix.setIdentityM(mScaleMatrix, 0);
		Matrix.scaleM(mScaleMatrix, 0, scaleX, scaleY, scaleZ);

		// Rotation along x
		Matrix.setRotateM(mRotXMatrix, 0, this.mAngleY, -1.0f, 0.0f, 0.0f);
		Matrix.setRotateM(mRotYMatrix, 0, this.mAngleX, 0.0f, 1.0f, 0.0f);

		// Set the ModelViewProjectionMatrix
		float tempMatrix[] = new float[16]; 
		Matrix.multiplyMM(tempMatrix, 0, mRotYMatrix, 0, mRotXMatrix, 0);
		Matrix.multiplyMM(mMMatrix, 0, mScaleMatrix, 0, tempMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

		// send to the shader
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "uMVPMatrix"), 1, false, mMVPMatrix, 0);

		// Create the normal modelview matrix
		// Invert + transpose of mvpmatrix
		Matrix.invertM(normalMatrix, 0, mMVPMatrix, 0);
		Matrix.transposeM(normalMatrix, 0, normalMatrix, 0);

		// send to the shader
		GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(_program, "normalMatrix"), 1, false, normalMatrix, 0);//mMVPMatrix, 0);

		// lighting variables
		// send to shaders
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "lightPos"), 1, lightPos, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "lightColor"), 1, lightColor, 0);

		// material 
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matAmbient"), 1, matAmbient, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matDiffuse"), 1, matDiffuse, 0);
		GLES20.glUniform4fv(GLES20.glGetUniformLocation(_program, "matSpecular"), 1, matSpecular, 0);
		GLES20.glUniform1f(GLES20.glGetUniformLocation(_program, "matShininess"), matShininess);

		// eye position
		GLES20.glUniform3fv(GLES20.glGetUniformLocation(_program, "eyePos"), 1, eyePos, 0);

		/*** DRAWING OBJECT **/
		drawAllObjects(_program, true, true);
		
	}
	
	/*
	 * Called when viewport is changed
	 * @see android.opengl.GLSurfaceView$Renderer#onSurfaceChanged(javax.microedition.khronos.opengles.GL10, int, int)
	 */
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		w = width;
		h = height;
		ratio = (float) width / height;
		Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 0.5f, 60);
		//Matrix.orthoM(mProjMatrix, 0, -ratio, ratio, -1, 1, 0.5f, 10);
	}

	/**
	 * Initialization function
	 */
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		// initialize shaders
		try {
			_shaders[GOURAUD_SHADER] = new Shader(vShaders[GOURAUD_SHADER], fShaders[GOURAUD_SHADER], mContext, false, 0); // gouraud
			_shaders[PHONG_SHADER] = new Shader(vShaders[PHONG_SHADER], fShaders[PHONG_SHADER], mContext, false, 0); // phong
			_shaders[NORMALMAP_SHADER] = new Shader(vShaders[NORMALMAP_SHADER], fShaders[NORMALMAP_SHADER], mContext, false, 0); // normal map
			_shaders[DEPTHMAP_SHADER] = new Shader(vShaders[DEPTHMAP_SHADER], fShaders[DEPTHMAP_SHADER], mContext, false, 0); // normal map
		} catch (Exception e) {
			//Log.d("Shader Setup", e.getLocalizedMessage());
		}

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glClearDepthf(1.0f);
		GLES20.glDepthFunc( GLES20.GL_LEQUAL );  // Passes if the incoming depth value is less than or equal to the stored depth value.
		GLES20.glDepthMask( true ); // enable writing into the depth buffer

		// cull backface
		GLES20.glFrontFace(GLES20.GL_CCW);
		GLES20.glEnable( GLES20.GL_CULL_FACE );
		GLES20.glCullFace(GLES20.GL_BACK); 

		// disable dithering for better shadow mapping
		GLES20.glDisable(GLES20.GL_DITHER);
		
		// light variables
		float[] lightC = {0.5f, 0.5f, 0.5f};
		this.lightColor = lightC;

		// material properties - TODO: ideally should be in Object3D itself 
		float[] mA = {1.0f, 0.5f, 0.5f, 1.0f};
		matAmbient = mA;

		// material properties for plane
		float[] ma2 = {1.0f, 215f/255f, 0.0f};
		matAmbient2 = ma2;
		
		float[] mD = {0.5f, 0.5f, 0.5f, 1.0f};
		matDiffuse = mD;

		float[] mS =  {1.0f, 1.0f, 1.0f, 1.0f};
		matSpecular = mS;

		matShininess = 5.0f;

		// Setup Render to texture
		setupRenderToTexture();
		
		// setup textures for all objects
		for(int i = 0; i < _objects.length; i++)
			setupTextures(_objects[i]);

		// set the view matrix
		Matrix.setLookAtM(mVMatrix, 0, eyeView[0], eyeView[1], eyeView[2], 
									   eyeView[3], eyeView[4], eyeView[5],
									   eyeView[6], eyeView[7], eyeView[8]);
		
		
		
		// Setup quad 
		// Generate your vertex, normal and index buffers
		// vertex buffer
		_qvb = ByteBuffer.allocateDirect(_quadv.length
				* FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
		_qvb.put(_quadv);
		_qvb.position(0);

		// index buffer
		_qib = ByteBuffer.allocateDirect(_quadi.length
				* 4).order(ByteOrder.nativeOrder()).asShortBuffer();
		_qib.put(_quadi);
		_qib.position(0);
		
		// Set the game loop tick
		 next_game_tick = SystemClock.elapsedRealtime();
	}

	/**************************
	 * OTHER METHODS
	 *************************/

	// rotates the light around the y-axis
	public void rotateLight() {
		if (!lightRotate)
			return;
		
		angle += 0.000005f;
		if (angle >= 6.2)
			angle = 0.0f;

		// rotate light about y-axis
		float newPosX = (float)(Math.cos(angle) * lightPos[0] - Math.sin(angle) * lightPos[2]);
		float newPosZ = (float)(Math.sin(angle) * lightPos[0] + Math.cos(angle) * lightPos[2]);
		lightPos[0] = newPosX; lightPos[2] = newPosZ;
	}
	
	/**
	 * Sets up the framebuffer and renderbuffer to render to texture
	 */
	private void setupRenderToTexture() {
		fb = new int[1];
		depthRb = new int[1];
		renderTex = new int[1];
		
		// generate
		GLES20.glGenFramebuffers(1, fb, 0);
		GLES20.glGenRenderbuffers(1, depthRb, 0);
		GLES20.glGenTextures(1, renderTex, 0);
		
		// generate color texture
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[0]);

		// parameters
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_NEAREST);

		// create it 
		// create an empty intbuffer first?
		int[] buf = new int[texW * texH];
		texBuffer = ByteBuffer.allocateDirect(buf.length
				* FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asIntBuffer();
		GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texW, texH, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texBuffer);//GLES20.GL_UNSIGNED_SHORT_5_6_5, texBuffer);
		
		// create render buffer and bind 16-bit depth buffer
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, texW, texH);
	}
	
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
	}

	/**
	 * Show texture or not?
	 */
	public void toggleTexturing() {
		enableTexture = !enableTexture;
		Object3D ob = _objects[this._currentObject];

		if (enableTexture && !ob.hasTexture()) {
			// Create a toast notification signifying that there is no texture associated with this object
			CharSequence text = "Object does not have associated texture";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(mContext, text, duration);
			toast.show();
		}
		//this.toggleTexturing();
	}

	/**
	 * Toggle viewing the depth texture
	 */
	public void toggleDepthTex() {
		this.viewDepthTex = !viewDepthTex;
		CharSequence text;
		// show short toast to notify the user
		if (viewDepthTex) {
			text = "Viewing depth texture";
			int duration = Toast.LENGTH_SHORT;
	
			Toast toast = Toast.makeText(mContext, text, duration);
			toast.show();
		}
	}
	
	/**
	 * Rotate light or not?
	 */
	public void toggleLight() {
		this.lightRotate = !lightRotate;
		CharSequence text;
		if (lightRotate)
			text = "Light rotation resumed";
		else
			text = "Light rotation paused";
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(mContext, text, duration);
		toast.show();
	}
	
	/**
	 * View shadows or not?
	 */
	public void toggleShadows() {
		this.viewShadows = !viewShadows;
	}

	/**
	 * Sets up texturing for the object
	 */
	private void setupTextures(Object3D ob) {
		// create new texture ids if object has them
		if (ob.hasTexture()) {
			// number of textures
			int[] texIDs = ob.get_texID();
			int[] textures = new int[texIDs.length];
			_texIDs = new int[texIDs.length];
			// texture file ids
			int[] texFiles = ob.getTexFile();

			//Log.d("TEXFILES LENGTH: ", texFiles.length + "");
			GLES20.glGenTextures(texIDs.length, textures, 0);

			for(int i = 0; i < texIDs.length; i++) {
				texIDs[i] = textures[i];

				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIDs[i]);

				// parameters
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
				.openRawResource(texFiles[i]);
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

				// create it 
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
				bitmap.recycle();

				//Log.d("ATTACHING TEXTURES: ", "Attached " + i);
			}
		}
	}

	/**
	 * Scaling
	 */
	public void changeScale(float scale) {
		if (scaleX * scale > 1.4f)
			return;
		scaleX *= scale;scaleY *= scale;scaleZ *= scale;

		//Log.d("SCALE: ", scaleX + "");
	}

	// debugging opengl
	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}

} 

// END CLASS