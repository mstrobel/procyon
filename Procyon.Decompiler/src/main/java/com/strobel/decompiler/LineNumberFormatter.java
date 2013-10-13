package com.strobel.decompiler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.strobel.decompiler.languages.LineNumberPosition;

/**
 * A <code>LineNumberFormatter</code> is used to rewrite an existing .java file, introducing
 * line number information.  It can handle either, or both, of the following jobs:
 * 
 * <ul>
 *   <li>Introduce line numbers as leading comments.
 *   <li>Stretch the file so that the line number comments match the physical lines.
 * </ul>
 */
public class LineNumberFormatter {
    private final List<LineNumberPosition> _positions;
    private final File _file;
    private final EnumSet<LineNumberOption> _options;
    
    public enum LineNumberOption
    {
        LEADING_COMMENTS,
        STRETCHED,
    }
    
    /**
     * Constructs an instance.
     * 
     * @param file the file whose line numbers should be fixed
     * @param lineNumberPositions a recipe for how to fix the line numbers in 'file'.
     * @param options controls how 'this' represents line numbers in the resulting file
     */
    public LineNumberFormatter(File file,
            List<LineNumberPosition> lineNumberPositions,
            EnumSet<LineNumberOption> options) {
        _file = file;
        _positions = lineNumberPositions;
        _options = (options == null ? EnumSet.noneOf( LineNumberOption.class) : options);
    }

    /**
     * Rewrites the file passed to 'this' constructor so that the actual line numbers match
     * the recipe passed to 'this' constructor.
     */
    public void reformatFile() throws IOException {
        List<LineNumberPosition> lineBrokenPositions = new ArrayList<LineNumberPosition>();
        File lineBrokenFile = breakLines( lineBrokenPositions);
        emitFormatted( lineBrokenFile, lineBrokenPositions);
    }
    
    /**
     * Processes {@link #_file}, breaking apart any lines on which multiple line-number markers
     * appear in different columns.
     * 
     * @return the temporary file created by this method
     */
    private File breakLines( List<LineNumberPosition> o_LineBrokenPositions) throws IOException {
        int numLinesRead = 0;
        int lineOffset = 0;
        File lineBrokenFile = new File( _file.getAbsolutePath() + ".linebroken");

        try( PrintWriter w = new PrintWriter( new BufferedWriter( new FileWriter( lineBrokenFile)));
                BufferedReader r = new BufferedReader( new FileReader( _file))) {
            for ( int posIndex=0; posIndex<_positions.size(); posIndex++) {
                LineNumberPosition pos = _positions.get( posIndex);
                o_LineBrokenPositions.add( new LineNumberPosition(
                        pos.getOriginalLine(), pos.getEmittedLine()+lineOffset, pos.getEmittedColumn()));
                
                // Copy the input file up to but not including the emitted line # in "pos".
                while ( numLinesRead < pos.getEmittedLine()-1) {
                    w.println( r.readLine());
                    numLinesRead++;
                }
                
                // Read the line that contains the next line number annotations, but don't write it yet.
                String line = r.readLine();
                numLinesRead++;
                
                // See if there are two original line annotations on the same emitted line.
                LineNumberPosition nextPos;
                int prevPartLen = 0;
                do {
                    nextPos = (posIndex < _positions.size()-1) ? _positions.get( posIndex+1) : null;
                    if ( nextPos != null
                        && nextPos.getEmittedLine() == pos.getEmittedLine()
                        && nextPos.getOriginalLine() > pos.getOriginalLine()) {
                        // Two different source line numbers on the same emitted line!
                        posIndex++;
                        lineOffset++;
                        String firstPart = line.substring( 0, nextPos.getEmittedColumn() - prevPartLen - 1);
                        prevPartLen += firstPart.length();
                        w.println( firstPart);
                        char[] indent = new char[prevPartLen];
                        Arrays.fill( indent, ' ');
                        w.print( indent);
                        line = line.substring( firstPart.length(), line.length());
                        
                        // Alter the position while adding it.
                        o_LineBrokenPositions.add( new LineNumberPosition(
                                nextPos.getOriginalLine(), nextPos.getEmittedLine()+lineOffset, nextPos.getEmittedColumn()));
                    } else {
                        nextPos = null;
                    }
                } while ( nextPos != null);
                
                // Nothing special here-- just emit the line.
                w.println( line);
            }
            
            // Copy out the remainder of the file.
            String line;
            while ( (line = r.readLine()) != null) {
                w.println( line);
            }
        }
        return lineBrokenFile;
    }
    
    private void emitFormatted( File lineBrokenFile, List<LineNumberPosition> lineBrokenPositions) throws IOException {
        File tempFile = new File( _file.getAbsolutePath() + ".fixed");
        int globalOffset = 0;
        int numLinesRead = 0;
        
        int maxLineNo = LineNumberPosition.computeMaxLineNumber( lineBrokenPositions);
        try( LineNumberPrintWriter w = new LineNumberPrintWriter( maxLineNo,
                                                                  new BufferedWriter( new FileWriter( tempFile)));
             BufferedReader r = new BufferedReader( new FileReader( lineBrokenFile))) {
            
            // Suppress all line numbers if we weren't asked to show them.
            if ( ! _options.contains( LineNumberOption.LEADING_COMMENTS)) {
                w.suppressLineNumbers();
            }
            
            // Suppress stretching if we weren't asked to do it.
            boolean doStretching = (_options.contains( LineNumberOption.STRETCHED));
            
            for ( LineNumberPosition pos : lineBrokenPositions) {
                int nextTarget = pos.getOriginalLine();
                int nextActual = pos.getEmittedLine();
                int requiredAdjustment = (nextTarget - nextActual - globalOffset);
                
                while( numLinesRead < nextActual) {
                    String line = r.readLine();
                    numLinesRead++;
                    boolean isLast = (numLinesRead >= nextActual);
                    int lineNoToPrint = isLast ? nextTarget : LineNumberPrintWriter.NO_LINE_NUMBER;
                    
                    if ( requiredAdjustment == 0 || !doStretching) {
                        // No tweaks needed-- we are on the ball.
                        w.println( lineNoToPrint, line);
                    } else if ( requiredAdjustment > 0) {
                        // We currently need to inject newlines to space things out.
                        do {
                            w.println( "");
                            requiredAdjustment--;
                            globalOffset++;
                        } while ( isLast && requiredAdjustment > 0);
                        w.println( lineNoToPrint, line);
                    } else {
                        // We currently need to remove newlines to squeeze things together.
                        w.print( lineNoToPrint, line);
                        w.print( "  ");
                        requiredAdjustment++;
                        globalOffset--;
                    }
                }                
            }
            
            // Finish out the file.
            String line;
            while ( (line = r.readLine()) != null) {
                w.println( line);
            }
        }
        
        // Delete the line-broken file and rename the formatted temp file over the original.
        lineBrokenFile.delete();
        tempFile.renameTo( _file);
    }

}
