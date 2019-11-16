package com.example.advanceevent;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 百度上直接copy过来的
 * XSSF and SAX (Event API)
 */
public abstract class BigDataParseExcelUtil extends DefaultHandler {
    private ReadOnlySharedStringsTable sst;
    private String lastContents;
    private boolean nextIsString;
    private int sheetIndex = -1;
    private List<String> rowlist = new ArrayList<String>();
    private int curRow = 0; // 当前行
    private int curCol = 0; // 当前列索引
    private int preCol = 0; // 上一列列索引
    private int titleRow = 0; // 标题行，一般情况下为0
    private int rowsize = 0; // 列数
    private List excelList = new ArrayList();  //excel全部转换为list

    // excel记录行操作方法，以sheet索引，行索引和行元素列表为参数，对sheet的一行元素进行操作，元素为String类型

    public abstract void optRows(int sheetIndex, int curRow,
                                 List<String> rowlist, List excelList) throws SQLException, Exception;

    // 只遍历一个sheet，其中sheetId为要遍历的sheet索引，从1开始，1-3

    /**
     * @param filename
     * @param sheetId  sheetId为要遍历的sheet索引，从1开始，1-3
     * @throws Exception
     */
    public void processOneSheet(String filename, int sheetId) throws Exception {
        OPCPackage pkg = OPCPackage.open(filename);
        XSSFReader r = new XSSFReader(pkg);
        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
        XMLReader parser = fetchSheetParser(strings);
        // rId2 found by processing the Workbook
        // 根据 rId# 或 rSheet# 查找sheet
        InputStream sheet2 = r.getSheet("rId" + sheetId);
        sheetIndex++;
        InputSource sheetSource = new InputSource(sheet2);
        parser.parse(sheetSource);
        sheet2.close();
    }

    public void processOneSheet(File file, int sheetId) throws Exception {
        OPCPackage pkg = OPCPackage.open(file);
        XSSFReader r = new XSSFReader(pkg);
        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
        XMLReader parser = fetchSheetParser(strings);
        // rId2 found by processing the Workbook
        // 根据 rId# 或 rSheet# 查找sheet
        InputStream sheet2 = r.getSheet("rId" + sheetId);
        sheetIndex++;
        InputSource sheetSource = new InputSource(sheet2);
        parser.parse(sheetSource);
        sheet2.close();
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {
        // 得到单元格内容的值
        lastContents += new String(ch, start, length);
    }

    public void process(InputStream inputStream) throws Exception {
        OPCPackage pkg = OPCPackage.open(inputStream);
        XSSFReader r = new XSSFReader(pkg);
        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
        XMLReader parser = fetchSheetParser(strings);
        Iterator<InputStream> sheets = r.getSheetsData();
        while (sheets.hasNext()) {
            curRow = 0;
            sheetIndex++;
            InputStream sheet = sheets.next();
            InputSource sheetSource = new InputSource(sheet);
            parser.parse(sheetSource);
            sheet.close();
        }
    }

    /**
     * 遍历 excel 文件
     */
    public void process(File file) throws Exception {
        OPCPackage pkg = OPCPackage.open(file);
        XSSFReader r = new XSSFReader(pkg);
        ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
        XMLReader parser = fetchSheetParser(strings);
        Iterator<InputStream> sheets = r.getSheetsData();
        while (sheets.hasNext()) {
            curRow = 0;
            sheetIndex++;
            InputStream sheet = sheets.next();
            InputSource sheetSource = new InputSource(sheet);
            parser.parse(sheetSource);
            sheet.close();
        }
    }

    public XMLReader fetchSheetParser(ReadOnlySharedStringsTable sst)
        throws SAXException {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        // .createXMLReader("org.apache.xerces.parsers.SAXParser");
        this.sst = sst;
        parser.setContentHandler(this);
        return parser;
    }

    @Override
    public void startElement(String uri, String localName, String name,
                             Attributes attributes) throws SAXException {
        // c => 单元格
        if (name.equals("c")) {
            // 如果下一个元素是 SST 的索引，则将nextIsString标记为true
            String cellType = attributes.getValue("t");
            String rowStr = attributes.getValue("r");
            curCol = this.getRowIndex(rowStr);
            if (cellType != null && cellType.equals("s")) {
                nextIsString = true;
            } else {
                nextIsString = false;
            }
        }
        // 置空
        lastContents = "";
    }

    @Override
    public void endElement(String uri, String localName, String name)
        throws SAXException {
        // 根据SST的索引值的到单元格的真正要存储的字符串
        // 这时characters()方法可能会被调用多次
        if (nextIsString) {
            try {
                int idx = Integer.parseInt(lastContents);
                lastContents = new XSSFRichTextString(sst.getEntryAt(idx))
                    .toString();
            } catch (Exception e) {
            }
        }
        // v => 单元格的值，如果单元格是字符串则v标签的值为该字符串在SST中的索引
        // 将单元格内容加入rowlist中，在这之前先去掉字符串前后的空白符
        if (name.equals("v")) {
            String value = lastContents.trim();
            value = value.equals("") ? " " : value;
            int cols = curCol - preCol;
            if (cols > 1) {
                for (int i = 0; i < cols - 1; i++) {
                    rowlist.add(preCol, "");
                }
            }
            preCol = curCol;
            rowlist.add(curCol - 1, value);
        } else {
            // 如果标签名称为 row ，这说明已到行尾，调用 optRows() 方法
            if (name.equals("row")) {
                int tmpCols = rowlist.size();
                if (curRow > this.titleRow && tmpCols < this.rowsize) {
                    for (int i = 0; i < this.rowsize - tmpCols; i++) {
                        rowlist.add(rowlist.size(), "");
                    }
                }
                try {
                    optRows(sheetIndex, curRow, rowlist, excelList);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (curRow == this.titleRow) {
                    this.rowsize = rowlist.size();
                }
                rowlist.clear();
                curRow++;
                curCol = 0;
                preCol = 0;
            }
        }
    }

    // 得到列索引，每一列c元素的r属性构成为字母加数字的形式，字母组合为列索引，数字组合为行索引，
    // 如AB45,表示为第（A-A+1）*26+（B-A+1）*26列，45行
    public int getRowIndex(String rowStr) {
        rowStr = rowStr.replaceAll("[^A-Z]", "");
        byte[] rowAbc = rowStr.getBytes();
        int len = rowAbc.length;
        float num = 0;
        for (int i = 0; i < len; i++) {
            num += (rowAbc[i] - 'A' + 1) * Math.pow(26, len - i - 1);
        }
        return (int) num;
    }


}