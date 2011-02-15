package org.erlide.cover.views.model;

/**
 * Line coverage results
 * 
 * @author Aleksandra Lipiec <aleksandra.lipiec@erlang-solutions.com>
 * 
 */
public class LineResult implements Comparable<LineResult> {

    private final int lineNum;
    private final int lineCalls;

    public LineResult(final int lineNum, final int lineCalls) {
        this.lineNum = lineNum;
        this.lineCalls = lineCalls;
    }

    public int getLineNum() {
        return lineNum;
    }

    public int getLineCalls() {
        return lineCalls;
    }

    public boolean called() {
        return lineCalls != 0;
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();

        buf.append(lineNum).append(" ").append(lineCalls);

        return buf.toString();
    }

    public int compareTo(final LineResult lineRes2) {
        return lineNum - lineRes2.getLineNum();
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof LineResult &&
                ((LineResult)obj).getLineNum() == lineNum)
            return true;
        return false;
    }
    
    @Override
    public int hashCode() {
        return lineNum;
    }

}