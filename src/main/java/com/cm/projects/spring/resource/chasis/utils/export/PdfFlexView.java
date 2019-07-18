/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cm.projects.spring.resource.chasis.utils.export;

import com.cm.projects.spring.resource.chasis.annotations.ExportField;
import com.cm.projects.spring.resource.chasis.utils.SharedMethods;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cornelius M
 * @param <T>
 */
public class PdfFlexView<T> extends AbstractPdfView {

    private final Class<T> clazz;
    private List<T> entities;
    private String fileName;
    private String title = "";
    private String dateFormat = "dd-MM-yyyy HH:mm:ss";

    public PdfFlexView(Class<T> clazz) {
        this.clazz = clazz;
    }

    public PdfFlexView(Class<T> clazz, List<T> entities) {
        this.clazz = clazz;
        this.entities = entities;
    }

    public PdfFlexView(Class<T> clazz, List<T> entities, String fileName) {
        this.clazz = clazz;
        this.entities = entities;
        this.fileName = fileName;
    }

    public PdfFlexView(Class<T> clazz, List<T> entities, String fileName, String title) {
        this.clazz = clazz;
        this.entities = entities;
        this.fileName = fileName;
        this.title = title;
    }

    public void setEntities(List<T> entities) {
        this.entities = entities;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    protected void buildPdfDocument(Map<String, Object> model, Document document, PdfWriter writer, HttpServletRequest request, HttpServletResponse response) throws Exception {

        // change the file name
        String fName = (this.fileName == null ? SharedMethods.splitCamelString(this.clazz.getSimpleName()).toLowerCase()
                : this.fileName);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fName + ".pdf\"");

        document.add(new Paragraph(title));

        // write table header
        BeanWrapper classWrapper = new BeanWrapperImpl(clazz);
        int columns = 0;
        List<String> columnTitles = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {//Get column titles from field name or annotation
            if (field.isAnnotationPresent(ExportField.class)
                    && classWrapper.isReadableProperty(field.getName())) {//check if the field has export annotation
                ExportField exportField = field.getAnnotation(ExportField.class);
                if (exportField.name().isEmpty()) {//check if field name has been given, if not retrieve from the field
                    columnTitles.add(SharedMethods.splitCamelString(field.getName()));
                } else {
                    columnTitles.add(exportField.name());
                }
                columns++;
            }
        }

        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100.0f);
        table.setSpacingBefore(10);

        // define font for table header row
        Font font = FontFactory.getFont(FontFactory.TIMES);
//        font.setColor(BaseColor.WHITE);

        // define table header cell
        PdfPCell cell = new PdfPCell();
//        cell.setBackgroundColor(BaseColor.DARK_GRAY);
        cell.setPadding(5);

        columnTitles.forEach(colTitle -> {
            cell.setPhrase(new Phrase(colTitle, font));
            table.addCell(cell);
        });

        //Generate rows from entities list
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.dateFormat);
        this.entities.stream().map((entity) -> {
            int i = 0;
            BeanWrapper wrapper = new BeanWrapperImpl(entity);
            return wrapper;
        }).forEachOrdered((wrapper) -> {
            List<String> row = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields()) {//retrieve values from the base field
                if (field.isAnnotationPresent(ExportField.class) && wrapper.isReadableProperty(field.getName())) {
                    ExportField exportField = field.getAnnotation(ExportField.class);
                    if (exportField.entityField().fieldName().isEmpty()) {//check if the current field is an entity
                        if (field.getType() == Date.class) {
                            String value = wrapper.getPropertyValue(field.getName()) == null ? "" : simpleDateFormat.format(wrapper.getPropertyValue(field.getName()));
                            table.addCell(value);
                        } else {
                            String value = wrapper.getPropertyValue(field.getName()) == null ? "" : "" + wrapper.getPropertyValue(field.getName());
                            table.addCell(value);
                        }
                    } else {//process the entity field
                        Object entity2 = wrapper.getPropertyValue(field.getName());
                        if (entity2 != null) {
                            BeanWrapper wrapper2 = new BeanWrapperImpl(entity2);
//                            wrapper.setPropertyValue(field.getName(), exportField.entityField().fieldName());
                            if (field.getType() == Date.class) {
                                String value = wrapper2.getPropertyValue(exportField.entityField().fieldName()) == null ? "" : simpleDateFormat.format(wrapper2.getPropertyValue(exportField.entityField().fieldName()));
                                table.addCell(value);
                            } else {
                                String value = wrapper2.getPropertyValue(exportField.entityField().fieldName()) == null ? "" : "" + wrapper2.getPropertyValue(exportField.entityField().fieldName());
                                table.addCell(value);
                            }
                        }
                    }
                }
            }
        });

        document.add(table);
    }
}
