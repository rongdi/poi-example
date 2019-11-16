package com.example.service;

import com.example.commonevent.BigDataParseExcelUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * @author: rongdi
 * @date:
 */
@Service
public class ExcelService {

    public void import1(InputStream inputStream) throws Exception {

        BigDataParseExcelUtil xlx = new BigDataParseExcelUtil() {
            @Override
            public void optRows(int sheetIndex, int curRow, List<String> rowlist, List excelList)
                throws SQLException {
                System.out.println(rowlist);
            }
        };
        xlx.process(inputStream);
    }

    public void import2(File file) throws Exception {
        BigDataParseExcelUtil xlx = new BigDataParseExcelUtil() {
            @Override
            public void optRows(int sheetIndex, int curRow, List<String> rowlist, List excelList)
                throws SQLException {
                System.out.println(rowlist);
            }
        };
        xlx.process(file);
    }

    public void import3(File file) throws Exception {
        long start = System.currentTimeMillis();
        com.example.advanceevent.BigDataParseExcelUtil  xlx = new com.example.advanceevent.BigDataParseExcelUtil () {
            @Override
            public void optRows(int sheetIndex, int curRow, List<String> rowlist, List excelList)
                throws SQLException {
                System.out.println(rowlist);
            }
        };
        xlx.process(file);
        long end = System.currentTimeMillis();
        System.out.println((end - start) + "ms");
    }
}
