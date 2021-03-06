package ru.icc.cells.ssdc.writers;

import org.apache.poi.ss.usermodel.*;
import ru.icc.cells.ssdc.model.CEntry;
import ru.icc.cells.ssdc.model.CLabel;
import ru.icc.cells.ssdc.model.CTable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Created by Alexey Shigarov on 28.06.2016.
 */
public class EvaluationExcelWriter extends BasicExcelWriter
{
    protected Workbook writeToWorkbook(CTable table)
    {
        Workbook workbook = super.writeToWorkbook(table);
        Sheet sheet = null;
        Row excelRow = null;
        Cell excelCell = null;
        int i = 0;

        final Path sourceWorkbookPath = table.getSrcWorkbookFile().toPath();
        final String sourceSheetName = table.getSrcSheetName();
        final Path basePath = Paths.get(outputFile.getParent());
        final Path relativePath = basePath.relativize(sourceWorkbookPath);
        final String template = "HYPERLINK(\"[%s]%s!%s\",\"%s\")";

        // Writing the evaluation sheet of entries

        sheet = workbook.createSheet("ENTRIES");
        excelRow = sheet.createRow(0);
        excelRow.createCell(0).setCellValue("ENTRY");
        excelRow.createCell(1).setCellValue("PROVENANCE");
        excelRow.createCell(2).setCellValue("LABELS");

        Iterator<CEntry> entries = table.getEntries();
        i = 1;
        while(entries.hasNext())
        {
            CEntry entry = entries.next();
            excelRow = sheet.createRow(i++);

            excelRow.createCell(0).setCellValue(entry.getValue());

            excelCell = excelRow.createCell(1);
            String cellRef = entry.getCell().getProvenance();
            String formula = String.format(template, relativePath, sourceSheetName, cellRef, cellRef);
            excelCell.setCellFormula(formula);

            final StringBuilder sb = new StringBuilder();
            Iterator<CLabel> labels = entry.getLabels();
            while ( labels.hasNext() )
            {
                CLabel label = labels.next();
                String value = label.getValue();
                String provenance = label.getCell().getProvenance();
                value = String.format("\"%s [%s]\"", value, provenance);
                sb.append(value);
                if ( labels.hasNext() )
                    sb.append(", ");
            }
            excelRow.createCell(2).setCellValue(sb.toString());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);

        // Writing the evaluation sheet of labels

        sheet = workbook.createSheet("LABELS");
        excelRow = sheet.createRow(0);
        excelRow.createCell(0).setCellValue("LABEL");
        excelRow.createCell(1).setCellValue("PROVENANCE");
        excelRow.createCell(2).setCellValue("PARENT");
        excelRow.createCell(3).setCellValue("CATEGORY");

        Iterator<CLabel> labels = table.getLabels();
        i = 1;
        while(labels.hasNext()) {
            CLabel label = labels.next();
            excelRow = sheet.createRow(i++);
            excelRow.createCell(0).setCellValue(label.getValue());

            excelCell = excelRow.createCell(1);
            //excelCell.setCellValue(entry.getCell().getProvenance());
            String cellRef = label.getCell().getProvenance();
            String formula = String.format(template, relativePath, sourceSheetName, cellRef, cellRef);
            excelCell.setCellFormula(formula);

            if (label.hasParent())
            {
                CLabel parent = label.getParent();
                String value = parent.getValue();
                String provenance = parent.getCell().getProvenance();
                value = String.format("%s [%s]", value, provenance);
                excelRow.createCell(2).setCellValue(value);
            }
            excelRow.createCell(3).setCellValue(label.getCategory().getName());
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);

        // Writing the provenance information
        sheet = workbook.createSheet("INFO");
        excelRow = sheet.createRow(0);
        excelCell = excelRow.createCell(0);
        excelCell.setCellValue("LINK TO THE SOURCE TABLE");
        sheet.autoSizeColumn(0);
        excelCell = excelRow.createCell(1);

        String cellRef = table.getSrcStartCellRef();
        String s = String.format("[%s]%s!%s", relativePath, sourceSheetName, cellRef);

        String formula = String.format(template, relativePath, sourceSheetName, cellRef, s);
        excelCell.setCellFormula(formula);

        // Copying the area of the source table from the source workbook to result one
        //Sheet sheetFrom = table.getSourceSheet();
        //CellReference start = new CellReference(table.getSrcStartCellRef());
        //CellReference end = new CellReference(table.getSrcEndCellRef());
        //Sheet sheetTo = workbook.createSheet("SOURCE TABLE");
        //copyArea(sheetFrom, start, end, sheetTo);




        return workbook;
    }

    public EvaluationExcelWriter(File outputFile) {
        super(outputFile);
    }

    /*
    private void copyArea(Sheet sheetFrom, CellReference start, CellReference end, Sheet sheetTo)
    {
        Row sourceRow = null;
        Row copyRow = null;
        Cell sourceCell = null;
        Cell copyCell = null;

        final int shift = start.getRow();

        for (int i = start.getRow(); i <= end.getRow(); i++) {
            sourceRow = sheetFrom.getRow(i);
            if (null == sourceRow) continue;

            copyRow = sheetTo.createRow(i - shift);

            // Copying cells in the row
            for (int j = start.getCol(); j <= end.getCol(); j++) {
                sourceCell = sourceRow.getCell(j);
                if (null == sourceCell) continue;

                copyCell = copyRow.createCell(j);

                // Copying style
                CellStyle copyCellStyle = sheetTo.getWorkbook().createCellStyle();
                copyCellStyle.cloneStyleFrom(sourceCell.getCellStyle());
                copyCell.setCellStyle(copyCellStyle);

                // Copying comment
                if (null != sourceCell.getCellComment()) {
                    copyCell.setCellComment(sourceCell.getCellComment());
                }

                // Copying hyperlink
                if (null != sourceCell.getHyperlink()) {
                    copyCell.setHyperlink(sourceCell.getHyperlink());
                }

                // Copying data type
                copyCell.setType(sourceCell.getType());

                // Copying value
                switch (sourceCell.getType()) {
                    case Cell.CELL_TYPE_BLANK:
                        copyCell.setCellValue(sourceCell.getStringCellValue());
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        copyCell.setCellValue(sourceCell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_ERROR:
                        copyCell.setCellErrorValue(sourceCell.getErrorCellValue());
                        break;
                    case Cell.CELL_TYPE_FORMULA:
                        copyCell.setCellFormula(sourceCell.getCellFormula());
                        break;
                    case Cell.CELL_TYPE_NUMERIC:
                        copyCell.setCellValue(sourceCell.getNumericCellValue());
                        break;
                    case Cell.CELL_TYPE_STRING:
                        copyCell.setCellValue(sourceCell.getRichStringCellValue());
                        break;
                }
            }
        }
    }
    */
}
