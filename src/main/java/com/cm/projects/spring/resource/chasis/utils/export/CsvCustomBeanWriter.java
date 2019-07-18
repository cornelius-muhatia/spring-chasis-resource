/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cm.projects.spring.resource.chasis.utils.export;

import org.supercsv.io.CsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 *
 * @author Cornelius M
 */
public class CsvCustomBeanWriter extends CsvBeanWriter {

    public CsvCustomBeanWriter(Writer writer, CsvPreference preference) {
        super(writer, preference);
    }
    /**
     * Used to write from List preferably a string List
     * @param objects
     * @param columns
     * @throws IOException 
     */
    public void writeFromList(final List<?> objects, String... columns) throws IOException {
        // update the current row/line numbers
        super.incrementRowAndLineNo();
        
        // write the list
        super.writeRow(objects);
    }

}
