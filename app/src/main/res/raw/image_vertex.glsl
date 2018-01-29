attribute vec4 vPosition;
attribute vec2 vCoordinate;
uniform mat4 trans;
uniform mat4 proj;

varying vec2 aCoordinate;

void main() {
    gl_Position = proj * trans * vPosition;
    aCoordinate = vCoordinate;
}