/**
 * Represents a shader object
 */

package graphics.shaders;

import android.opengl.GLES20;
import android.util.Log;

public class Shader {
	/************************
	 * PROPERTIES
	 **********************/
	
	// handles
	private int _program, _vertexShader, _pixelShader;
	
	// other handles - position/texture/mvpmatrix
	private int muMVPMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;
	
	// The shaders
	private String _vertexS, _fragmentS;
	
	
	
    /************************
     * CONSTRUCTOR(S)
     *************************/
	public Shader() {
		
	}
	
	public Shader(String vertexS, String fragmentS) {
		this._vertexS = vertexS;
		this._fragmentS = fragmentS;
		int create = createProgram();
	}
	
	
	/**************************
	 * OTHER METHODS
	 *************************/
	
	
	/**
	 * Creates a shader program.
	 * @param vertexSource
	 * @param fragmentSource
	 * @return returns 1 if creation successful, 0 if not
	 */
	private int createProgram() {
		// Vertex shader
        _vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, _vertexS);
        if (_vertexShader == 0) {
            return 0;
        }

        // pixel shader
        _pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, _fragmentS);
        if (_pixelShader == 0) {
            return 0;
        }

        // Create the program
        _program = GLES20.glCreateProgram();
        if (_program != 0) {
            GLES20.glAttachShader(_program, _vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(_program, _pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(_program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(_program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e("Shader", "Could not link _program: ");
                Log.e("Shader", GLES20.glGetProgramInfoLog(_program));
                GLES20.glDeleteProgram(_program);
                _program = 0;
                return 0;
            }
        }
        
        // The handles
        // position
        maPositionHandle = GLES20.glGetAttribLocation(_program, "position");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        
        // texture
        maTextureHandle = GLES20.glGetAttribLocation(_program, "textureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        // modelview/projection matrix
        muMVPMatrixHandle = GLES20.glGetUniformLocation(_program, "mvpMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }
        
        return 1;
    }
	
	/**
	 * Sets up the handles for the inputs
	 */
	public void setupHandles() {
		
	}
	
	/**
	 * Loads a shader (either vertex or pixel) given the source
	 * @param shaderType VERTEX or PIXEL
	 * @param source The string data representing the shader code
	 * @return handle for shader
	 */
	private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("Shader", "Could not compile shader " + shaderType + ":");
                Log.e("Shader", GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }
	
	/**
	 * Error for OpenGL
	 * @param op
	 */
	private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("Shader", op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

	/***************************
	 * GET/SET
	 *************************/
	public int get_program() {
		return _program;
	}

	public void set_program(int _program) {
		this._program = _program;
	}

	public int get_vertexShader() {
		return _vertexShader;
	}

	public void set_vertexShader(int shader) {
		_vertexShader = shader;
	}

	public int get_pixelShader() {
		return _pixelShader;
	}

	public void set_pixelShader(int shader) {
		_pixelShader = shader;
	}

	public int getMuMVPMatrixHandle() {
		return muMVPMatrixHandle;
	}

	public void setMuMVPMatrixHandle(int muMVPMatrixHandle) {
		this.muMVPMatrixHandle = muMVPMatrixHandle;
	}

	public int getMaPositionHandle() {
		return maPositionHandle;
	}

	public void setMaPositionHandle(int maPositionHandle) {
		this.maPositionHandle = maPositionHandle;
	}

	public int getMaTextureHandle() {
		return maTextureHandle;
	}

	public void setMaTextureHandle(int maTextureHandle) {
		this.maTextureHandle = maTextureHandle;
	}

	public String get_vertexS() {
		return _vertexS;
	}

	public void set_vertexS(String _vertexs) {
		_vertexS = _vertexs;
	}

	public String get_fragmentS() {
		return _fragmentS;
	}

	public void set_fragmentS(String _fragments) {
		_fragmentS = _fragments;
	}
}
