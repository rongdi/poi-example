package com.example.commonevent;

import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.util.Removal;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFRelation;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.poi.xssf.usermodel.XSSFRelation.NS_SPREADSHEETML;

public class ReadOnlySharedStringsTable extends DefaultHandler implements SharedStrings {

    protected final boolean includePhoneticRuns;

    /**
     * An integer representing the total count of strings in the workbook. This count does not
     * include any numbers, it counts only the total of text strings in the workbook.
     */
    protected int count;

    /**
     * An integer representing the total count of unique strings in the Shared String Table.
     * A string is unique even if it is a copy of another string, but has different formatting applied
     * at the character level.
     */
    protected int uniqueCount;

    /**
     * The shared strings table.
     */
    private List<String> strings;

    private File tmp = null;

    FileOutputStream fos = null;

    private int counts;

    private Map<Integer,String> map = new LinkedHashMap<Integer,String>();

    public ReadOnlySharedStringsTable(OPCPackage pkg)
            throws IOException, SAXException {
        this(pkg, true);
    }

    public ReadOnlySharedStringsTable(OPCPackage pkg, boolean includePhoneticRuns)
            throws IOException, SAXException {
        this.includePhoneticRuns = includePhoneticRuns;
        ArrayList<PackagePart> parts =
                pkg.getPartsByContentType(XSSFRelation.SHARED_STRINGS.getContentType());

        // Some workbooks have no shared strings table.
        if (parts.size() > 0) {
            PackagePart sstPart = parts.get(0);
            readFrom(sstPart.getInputStream());
        }
    }

    /**
     * Like POIXMLDocumentPart constructor
     *
     * Calls {@link #ReadOnlySharedStringsTable(PackagePart, boolean)}, with a
     * value of <code>true</code> to include phonetic runs.
     *
     * @since POI 3.14-Beta1
     */
    public ReadOnlySharedStringsTable(PackagePart part) throws IOException, SAXException {
        this(part, true);
    }

    /**
     * Like POIXMLDocumentPart constructor
     *
     * @since POI 3.14-Beta3
     */
    public ReadOnlySharedStringsTable(PackagePart part, boolean includePhoneticRuns)
        throws IOException, SAXException {
        this.includePhoneticRuns = includePhoneticRuns;
        readFrom(part.getInputStream());
    }
    
    /**
     * Read this shared strings table from an XML file.
     *
     * @param is The input stream containing the XML document.
     * @throws IOException if an error occurs while reading.
     * @throws SAXException if parsing the XML data fails.
     */
    public void readFrom(InputStream is) throws IOException, SAXException {
        // test if the file is empty, otherwise parse it
        PushbackInputStream pis = new PushbackInputStream(is, 1);
        int emptyTest = pis.read();
        if (emptyTest > -1) {
            pis.unread(emptyTest);
            InputSource sheetSource = new InputSource(pis);
            try {
                XMLReader sheetParser = SAXHelper.newXMLReader();
                sheetParser.setContentHandler(this);
                sheetParser.parse(sheetSource);
            } catch(ParserConfigurationException e) {
                throw new RuntimeException("SAX parser appears to be broken - " + e.getMessage());
            }
        }
    }

    /**
     * Return an integer representing the total count of strings in the workbook. This count does not
     * include any numbers, it counts only the total of text strings in the workbook.
     *
     * @return the total count of strings in the workbook
     */
    @Override
    public int getCount() {
        return this.count;
    }

    /**
     * Returns an integer representing the total count of unique strings in the Shared String Table.
     * A string is unique even if it is a copy of another string, but has different formatting applied
     * at the character level.
     *
     * @return the total count of unique strings in the workbook
     */
    @Override
    public int getUniqueCount() {
        return this.uniqueCount;
    }

    /**
     * Return the string at a given index.
     * Formatting is ignored.
     *
     * @param idx index of item to return.
     * @return the item at the specified position in this Shared String table.
     * @deprecated use <code>getItemAt</code> instead
     */
    @Removal(version = "4.2")
    @Deprecated
    public String getEntryAt(int idx) {
        /**
         * 这里就是修改部分了，直接从按行存储的临时文件读取需要的字符串
         */
        String value = map.get(idx + 1);
        if(value == null) {

            return readString(idx,1000,this.uniqueCount);
        } else {
            return value;
        }

    }

    /**
     * 从指定位置读取size个字符串，这里是使用局部性原理，每次读取size个字符串，
     * 以免每次需要读取文件，性能极低
     * @return
     */
    private String readString(int idx,int size,int numbers) {
        map.clear();
        int currNumber = idx + 1;
        if (currNumber < 0 || currNumber > numbers) {
            return null;
        }
        try {
            FileReader in = new FileReader(tmp);
            LineNumberReader reader = new LineNumberReader(in);
            try {
                String line = "";
                for(int i = 1;i <= numbers;i ++) {
                    line = reader.readLine();
                    if(i >= currNumber && i < currNumber + size) {
                        map.put(i, line);
                    }
                }
            } finally {
                reader.close();
                in.close();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return map.get(idx + 1);
    }


    /**
     * Returns all the strings.
     * Formatting is ignored.
     *
     * @return a list with all the strings
     * @deprecated use <code>getItemAt</code> instead
     */
    @Removal(version = "4.2")
    @Deprecated
    public List<String> getItems() {
        return strings;
    }

    @Override
    public RichTextString getItemAt(int idx) {
        return new XSSFRichTextString(getEntryAt(idx));
    }

    //// ContentHandler methods ////

    private StringBuilder characters;
    private boolean tIsOpen;
    private boolean inRPh;

    @Override
    public void startElement(String uri, String localName, String name,
                             Attributes attributes) throws SAXException {
        if (uri != null && ! uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        if ("sst".equals(localName)) {
            String count = attributes.getValue("count");
            if(count != null) this.count = Integer.parseInt(count);
            String uniqueCount = attributes.getValue("uniqueCount");
            if(uniqueCount != null) this.uniqueCount = Integer.parseInt(uniqueCount);
            try {
                tmp = Files.createTempFile("tmp-", ".tmp").toFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //    this.strings = new ArrayList<>(this.uniqueCount);
            characters = new StringBuilder(64);
            try {
                fos = new FileOutputStream(tmp,true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if ("si".equals(localName)) {
            characters.setLength(0);
        } else if ("t".equals(localName)) {
            tIsOpen = true;
        } else if ("rPh".equals(localName)) {
            inRPh = true;
            //append space...this assumes that rPh always comes after regular <t>
            if (includePhoneticRuns && characters.length() > 0) {
                characters.append(" ");
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (uri != null && ! uri.equals(NS_SPREADSHEETML)) {
            return;
        }

        if ("si".equals(localName)) {
         //   strings.add(characters.toString().intern());
            try {
                /**
                 * 这里就是修改的一部分，这里直接把字符串按行存入临时文件
                 */
                counts ++;
                fos.write((characters.toString() + "\n").getBytes());
                if(counts == this.uniqueCount) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if ("t".equals(localName)) {
            tIsOpen = false;
        } else if ("rPh".equals(localName)) {
            inRPh = false;
        }
    }

    /**
     * Captures characters only if a t(ext) element is open.
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (tIsOpen) {
            if (inRPh && includePhoneticRuns) {
                characters.append(ch, start, length);
            } else if (! inRPh){
                characters.append(ch, start, length);
            }
        }
    }

}
