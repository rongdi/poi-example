package com.example.controller;

import com.example.service.ExcelService;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * @author: rongdi
 * @date:
 */
@Controller
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @RequestMapping("/excel/import1")
    @ResponseBody
    public String import1(@RequestParam("file") MultipartFile multipartFile) throws Exception {
        excelService.import1(multipartFile.getInputStream());
        return "ok";
    }

    @RequestMapping("/excel/import2")
    @ResponseBody
    public String import2(@RequestParam("file") MultipartFile multipartFile) throws Exception {
        // 延迟解析比率
        ZipSecureFile.setMinInflateRatio(-1.0d);
        File tmp = Files.createTempFile("tmp-", ".xlsx").toFile();
        Files.copy(multipartFile.getInputStream(), Paths.get(tmp.getPath()), StandardCopyOption.REPLACE_EXISTING);
        excelService.import2(tmp);
        return "ok";
    }

    @RequestMapping("/excel/import3")
    @ResponseBody
    public String import3(@RequestParam("file") MultipartFile multipartFile) throws Exception {
        // 延迟解析比率
        ZipSecureFile.setMinInflateRatio(-1.0d);
        File tmp = Files.createTempFile("tmp-", ".xlsx").toFile();
        Files.copy(multipartFile.getInputStream(), Paths.get(tmp.getPath()), StandardCopyOption.REPLACE_EXISTING);
        excelService.import3(tmp);
        return "ok";
    }

}
