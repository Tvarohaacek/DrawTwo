package model;


public class Point {
    public int x;
    public int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Metoda pro převod na AWT Point
    public java.awt.Point toAWTPoint() {
        return new java.awt.Point(this.x, this.y);
    }

    public static Point fromAWTPoint(java.awt.Point awtPoint) {
        return new Point(awtPoint.x, awtPoint.y);
    }
}

