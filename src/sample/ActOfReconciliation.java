package sample;

import javafx.util.Pair;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class ActOfReconciliation {
    private Workbook workbook;
    private String name;
    private Sheet sheet;
    private static String tabOfDebit = "Дебет";
    private static String tabOfCredit = "Кредит";
    private List<Pair<Integer, Integer>> posDebitList;
    private List<Pair<Integer, Integer>> posCreditList;
    private Deque<Cell> deque;
    private int endRow;
    private int startRow;
    private int targetColumn;
    private static int countNextCells = 10;
    private enum EXTENSION_ENUM {XSSF, HSSF}
    private EXTENSION_ENUM EXTENSION;
    private CellStyle found, notFound;


    public ActOfReconciliation(Workbook workbook, String name){
        this.workbook = workbook;
        this.name = name;
        System.out.println(workbook.getNumberOfSheets());
        this.sheet = workbook.getSheetAt(0);
        posCreditList = new LinkedList<>();
        posDebitList = new LinkedList<>();
        deque = new LinkedList<>();
        /*
        Workbook wb = WorkbookFactory.create(new File("existing.xls"));
CellStyle origStyle = wb.getCellStyleAt(1); // Or from a cell

Workbook newWB = new XSSFWorkbook();
Sheet sheet = newWB.createSheet();
Row r1 = sheet.createRow(0);
Cell c1 = r1.createCell(0);

CellStyle newStyle = newWB.createCellStyle();
newStyle.cloneStyleFrom(origStyle);
c1.setCellStyle(newStyle);
         */
        found = workbook.createCellStyle();
        notFound = workbook.createCellStyle();
        found.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        found.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        notFound.setFillForegroundColor(IndexedColors.RED.getIndex());
        notFound.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    public void findTabs() throws Exception {
        Iterator<Row> rows = sheet.rowIterator();
        boolean flag = false;
        while (rows.hasNext()) {
            Row row = rows.next();
            Iterator<Cell> cells = row.cellIterator();
            while(cells.hasNext()){
                Cell cell = cells.next();
                if(cell != null && cell.getCellTypeEnum() == CellType.STRING){
                    if(cell.getStringCellValue().equals(tabOfDebit)){
                        if(!checkOnEmptyColumn(cell.getRowIndex(), cell.getColumnIndex())) {
                            posDebitList.add(new Pair<Integer, Integer>(cell.getRowIndex() + 1, cell.getColumnIndex()));
                            flag = true;
                        }
                    } else if(cell.getStringCellValue().equals(tabOfCredit)){
                        if(!checkOnEmptyColumn(cell.getRowIndex(), cell.getColumnIndex())) {
                            posCreditList.add(new Pair<Integer, Integer>(cell.getRowIndex() + 1, cell.getColumnIndex()));
                            flag = true;
                        }
                    }
                }
            }
            System.out.println(row.getRowNum());
            if(flag)
                break;
        }
        if(posDebitList.size() == 0 || posCreditList.size() == 0)
            throw new Exception("Не найдены колонки!");
    }

    public void findEndOfTable(){
        int rowIndex = posDebitList.get(0).getKey();
        int columnIndex = posDebitList.get(0).getValue();
        while(checkBorder(sheet.getRow(rowIndex++).getCell(columnIndex)));
        endRow = --rowIndex;
    }

    private boolean checkBorder(Cell cell) {
        if(cell == null)
            return false;
        CellStyle style = cell.getCellStyle();
        return style.getBorderTopEnum() == style.getBorderLeftEnum()
                && style.getBorderBottomEnum() == style.getBorderTopEnum();
    }

    private boolean checkOnEmptyColumn(int rowIndex, int columnIndex) {
        for(int rowInd = rowIndex; rowInd < rowIndex + 10; rowInd++) {
            Row row = sheet.getRow(rowInd);
            Cell cell = row.getCell(columnIndex);
            if(cell.getCellTypeEnum() == CellType.NUMERIC || (cell.getCellTypeEnum() == CellType.STRING && checkStrToNum(cell.getStringCellValue())))
                return false;
        }
        return true;
    }

    private boolean checkStrToNum(String stringCellValue) {
        try {
            Double.parseDouble(stringCellValue);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }

    public int compare(ActOfReconciliation other){
        int compareValue = 0;
        startRow = posDebitList.get(0).getKey();
        targetColumn = posDebitList.get(0).getValue();
        other.startRow = other.posCreditList.get(0).getKey();
        other.targetColumn = other.posCreditList.get(0).getValue();
        List<Cell> modifyCellList = new ArrayList<>();
        List<Cell> removeList = new ArrayList<>();
        boolean readFlag = readCells();
        boolean readOtherFlag = other.readCells();
        while(readFlag || readOtherFlag){
            Iterator<Cell> iCell = deque.iterator();
            while(iCell.hasNext()){
                Cell cell = iCell.next();
                Cell otherCell = other.getCellWithValue(getNumber(cell));
                if(otherCell != null){
                    //cell.setCellStyle(found);
                    cell.setCellStyle(found);
                    otherCell.setCellStyle(other.found);
                    removeList.add(cell);
                    other.deque.removeLastOccurrence(otherCell);
                }
            }
            if(removeList.size() == 0){
                compareValue++;
                Cell notFoundCell = deque.pollLast();
                if(notFoundCell != null) {
                    notFoundCell.setCellStyle(notFound);
                    System.out.println("not found, pollLast = " + getNumber(notFoundCell));
                }
            } else {
                for (Cell buf : removeList)
                    deque.removeLastOccurrence(buf);
                /*iCell = modifyCellList.iterator();
                while(iCell.hasNext()){
                    Cell cell = iCell.next();
                    cell.setCellStyle(getCellStyle(cell, found));
                }*/
            }
            readFlag = readCells();
            readOtherFlag = other.readCells();
        }
        compareValue += removeLast(notFound);
        compareValue += other.removeLast(other.notFound);
        return compareValue;
    }

    private CellStyle getCellStyle(Cell cell, CellStyle style){
        CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
        cellStyle.cloneStyleFrom(style);
        return cellStyle;
    }

    private double getNumber(Cell cell){
        if(cell.getCellTypeEnum() == CellType.STRING){
            return Double.parseDouble(cell.getStringCellValue().replace(",", ".").replace(" ", ""));
        } else if(cell.getCellTypeEnum() == CellType.NUMERIC)
            return cell.getNumericCellValue();
        else
            throw new NumberFormatException("Найден текст (" + cell.getRowIndex() + ", " + cell.getColumnIndex() + ")");
    }

    private int removeLast(CellStyle cellStyle){
        int count = 0;
        Cell lastCell;
        while((lastCell = deque.pollLast()) != null){
            System.out.println(getNumber(lastCell));
            lastCell.setCellStyle(cellStyle);
            count++;
        }
        return count;
    }

    /*public int compare(ActOfReconciliation other){
        int compareValue = 0;
        int countNextCells = 10;
        int startRowOur = posDebitList.get(0).getKey();
        int constColumnOurDebit = posDebitList.get(0).getValue();
        int startRowTheir = other.posCreditList.get(0).getKey();
        int constColumnTheirCredit = other.posCreditList.get(0).getValue();
        Cell cellOur;
        while((cellOur = getNextCell(startRowOur, constColumnOurDebit)) != null) {
            do {
                Cell cellTheir = other.getNextCell(startRowTheir, constColumnTheirCredit);
                Cell cellLastOur;
                Cell cellLastTheir;
                if (cellTheir != null) {
                    double our = Double.parseDouble(cellOur.getStringCellValue());

                    Cell selected = getCellFromTheirWithValue(our);
                    if (selected == null) {
                        if (dequeTheir.size() < countNextCells)
                            dequeTheir.push(cellTheir);
                        else{
                            cellLastTheir = dequeTheir.pollLast();

                        }
                    }
                }
            } while (dequeTheir.size() < countNextCells);
        }
        return compareValue;
    }*/

    private boolean readCells(){
        Cell cell;
        boolean addedFlag = false;
        do {
            cell = getNextCell();
            if(cell != null) {
                deque.add(cell);
                addedFlag = true;
            }
        } while(cell != null && deque.size() < countNextCells);
        return addedFlag;
    }

    private Cell getCellWithValue(double value){
        if(deque.isEmpty())
            return null;
        Cell cell = null;
        for(Cell buf : deque){
            double d = getNumber(buf);
            if(d == value){
                cell = buf;
            }
        }
        return cell;
    }

    private Cell getNextCell() {
        Cell cell = null;
        if(startRow == endRow)
            return cell;
        do {
            cell = sheet.getRow(startRow++).getCell(targetColumn);
            if(cell.getCellTypeEnum() == CellType.STRING){
                System.out.println("warning: Найден текст О_О (row=" + cell.getRowIndex() + ", column=" + cell.getColumnIndex()
                        + ", text = " + cell.getStringCellValue() + ")");
                try {
                    double d = getNumber(cell);
                    if(d == 0)
                        cell = null;
                } catch (NumberFormatException e){
                    cell = null;
                }
            } else if(cell.getCellTypeEnum() == CellType.NUMERIC && cell.getNumericCellValue() == 0){
                cell = null;
            } else if(cell.getCellTypeEnum() == CellType.NUMERIC){
                System.out.println("cell.NumericCellValue = " + cell.getNumericCellValue());
            } else
                cell = null;
        }while (cell == null &&startRow < endRow);
        return cell;
    }

    public double readNumber(int rowIndex, int columnIndex){
        Cell cell = sheet.getRow(rowIndex).getCell(columnIndex);
        if(cell != null){
            return Double.parseDouble(cell.getStringCellValue());
        } else
            throw new NumberFormatException("number format exception");
    }

    public List<Pair<Integer, Integer>> getPosDebitList() {
        return posDebitList;
    }

    public List<Pair<Integer, Integer>> getPosCreditList() {
        return posCreditList;
    }

    public void save(FileOutputStream file){
        try {
            workbook.write(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            workbook.close();
        } catch (IOException e){

        }
    }

    @Override
    public String toString() {
        return "ActOfReconciliation{" +
                "name=" + name +
                ", posDebitList=" + posDebitList +
                ", posCreditList=" + posCreditList +
                ", endRow=" + endRow +
                '}';
    }
}
