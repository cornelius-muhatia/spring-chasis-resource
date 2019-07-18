/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cm.projects.spring.resource.chasis.utils.export;

import com.cm.projects.spring.resource.chasis.annotations.ExportField;
import com.cm.projects.spring.resource.chasis.utils.SharedMethods;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cornelius M
 * @param <T>
 */
public class ExcelFlexView<T> extends AbstractXlsView {

    private final Class<T> clazz;
    private List<T> entities;
    private String fileName;
    private String dateFormat = "dd-MM-yyyy HH:mm:ss";

    public ExcelFlexView(Class<T> clazz) {
        this.clazz = clazz;
    }

    public ExcelFlexView(Class<T> clazz, List<T> entities) {
        this.clazz = clazz;
        this.entities = entities;
    }

    public ExcelFlexView(Class<T> clazz, List<T> entities, String fileName) {
        this.clazz = clazz;
        this.entities = entities;
        this.fileName = fileName;
    }

    public void setEntities(List<T> entities) {
        this.entities = entities;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    protected void buildExcelDocument(Map<String, Object> model, org.apache.poi.ss.usermodel.Workbook workbook,
                                      HttpServletRequest request, HttpServletResponse response) throws Exception {
        // change the file name
        String fileName = (this.fileName == null ? SharedMethods.splitCamelString(this.clazz.getSimpleName()).toLowerCase()
                : this.fileName);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".xls\"");

        // create excel xls sheet
        Sheet sheet = workbook.createSheet(fileName);
        sheet.setDefaultColumnWidth(30);

        // create style for header cells
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Arial");
        style.setFillForegroundColor(HSSFColor.WHITE.index);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        font.setBold(true);
        font.setColor(HSSFColor.BLACK.index);
        style.setFont(font);

        // create header row
        Row header = sheet.createRow(0);
        int i = 0;

        BeanWrapper classWrapper = new BeanWrapperImpl(clazz);
        for (Field field : clazz.getDeclaredFields()) {//Get column titles from field name or annotation
            if (field.isAnnotationPresent(ExportField.class)
                    && classWrapper.isReadableProperty(field.getName())) {//check if the field has export annotation
                ExportField exportField = field.getAnnotation(ExportField.class);
                if (exportField.name().isEmpty()) {//check if field name has been given, if not retrieve from the field
                    header.createCell(i).setCellValue(SharedMethods.splitCamelString(field.getName()));
                } else {
                    header.createCell(i).setCellValue(exportField.name());
                }
                header.getCell(i++).setCellStyle(style);
            }
        }
        int rowCount = 1;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.dateFormat);
        for (T entity : this.entities) {//Generate rows from entities list
            Row userRow = sheet.createRow(rowCount++);
            i = 0;
            BeanWrapper wrapper = new BeanWrapperImpl(entity);
            for (Field field : clazz.getDeclaredFields()) {//retrieve values from the base field
                if (field.isAnnotationPresent(ExportField.class) && wrapper.isReadableProperty(field.getName())) {
                    ExportField exportField = field.getAnnotation(ExportField.class);
                    if (exportField.entityField().fieldName().isEmpty()) {//check if the current field is an entity
                        if (field.getType() == Date.class) {
                            String value = wrapper.getPropertyValue(field.getName()) == null ? "" : simpleDateFormat.format(wrapper.getPropertyValue(field.getName()));
                            userRow.createCell(i++).setCellValue(value);
                        } else {
                            String value = wrapper.getPropertyValue(field.getName()) == null ? "" : "" + wrapper.getPropertyValue(field.getName());
                            userRow.createCell(i++).setCellValue(value);
                        }
//                        userRow.createCell(i++).setCellValue((String) wrapper.getPropertyValue(field.getName()));
                    } else {//process the entity field
                        Object entity2 = wrapper.getPropertyValue(field.getName());
                        if (entity2 != null) {
                            BeanWrapper wrapper2 = new BeanWrapperImpl(entity2);
                            if (field.getType() == Date.class) {
                                String value = wrapper2.getPropertyValue(exportField.entityField().fieldName()) == null ? "" : simpleDateFormat.format(wrapper2.getPropertyValue(exportField.entityField().fieldName()));
                                userRow.createCell(i++).setCellValue(value);
                            } else {
                                String value = wrapper2.getPropertyValue(exportField.entityField().fieldName()) == null ? "" : "" + wrapper2.getPropertyValue(exportField.entityField().fieldName());
                                userRow.createCell(i++).setCellValue(value);
                            }
//                            userRow.createCell(i++).setCellValue((String) wrapper2.getPropertyValue(exportField.entityField().fieldName()));
                        }
                    }
                }
            }
        }
    }
}
