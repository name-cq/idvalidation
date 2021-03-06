package com.apa70.idvalidation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.apa70.idvalidation.exception.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Collect {

    /** 判断数字的正则表达式 */
    private String numRegular="-?[1-9]\\d*";

    /**
     * 添加一个新的数据到某个目录
     * @param file 要添加的文件File对象
     * @param version 版本号
     * @param path 存储地址
     * @throws IOException
     */
    public void add(File file,int version, String path) throws IOException {
        InputStream inputStream=new FileInputStream(file);
        StringBuilder sbf = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                sbf.append(line);
            }

            this.add(sbf.toString(),version,path);
        }finally {
            inputStream.close();
        }
    }

    /**
     * 添加一个新的数据到某个目录
     * @param htmlString html的文本
     * @param version 版本号
     * @param path 存储地址
     * @throws IOException
     */
    public void add(String htmlString, int version, String path) throws IOException{
        //一些变量
        String administrativeCode = "";
        Map<String,Map<String,String>> codeMap=null;
        Map<String,String> codeDateMap=null;
        File administrativeCodeDataFile=null;
        String pathData=path+"/administrative-code-data/";
        path+="/administrative-code-data/code.json";
        Writer codeMapWriter=null;

        //判断是否有数据
        File pathFile=new File(path);
        if(pathFile.exists()) {
            codeMap = JSON.parseObject(getFileText(pathFile), new TypeReference<Map<String, Map<String, String>>>(){});
        }else {
            codeMap = new HashMap<>();
        }

        try {
            //使用Jsoup解析这些字符串
            int trsI=0;
            Elements tables = Jsoup.parse(htmlString).select("table");
            for(Element tableElement:tables){
                Elements trs=tableElement.select("tr");
                for(Element trElement:trs){
                    if(trsI<3){trsI++;continue;}
                    for(Element tdElement:trElement.select("td")) {
                        if(tdElement.text().equals(""))continue;
                        //判断正则判断是否为数字
                        if(Pattern.matches(numRegular,tdElement.text())){
                            //是数字，则为行政代码
                            administrativeCode=tdElement.text();
                        }else{
                            //不是则为行政名
                            if(administrativeCode.equals(""))continue;

                            if(codeMap.containsKey(administrativeCode)) {
                                codeDateMap = codeMap.get(administrativeCode);
                            }else {
                                codeDateMap = new HashMap<>();
                            }
                            codeDateMap.put(String.valueOf(version),tdElement.text());
                            codeMap.put(administrativeCode,codeDateMap);

                            administrativeCode="";
                            codeDateMap=null;
                        }
                    }
                }
            }
        }catch(NumberFormatException e){
            throw new TextCannotUnableAnalysisException("内容无法解析"+e.getMessage());
        }

        if(codeMap.size()<=0){
            throw new GetInfoException("获取信息失败！");
        }

        try {
            //判断administrative-code-data文件夹是否存在
            administrativeCodeDataFile=new File(pathData);
            if(!administrativeCodeDataFile.isDirectory()){
                //不存在创建一个
                boolean isSuccess=administrativeCodeDataFile.mkdir();
                if(!isSuccess)
                    throw new CreateFolderException("创建文件夹"+path+"失败！");
            }

            //判断文件是否存在
            if(!pathFile.exists()){
                //不存在创建一个
                boolean isSuccess=pathFile.createNewFile();
                if(!isSuccess)
                    throw new CreateFolderException("创建文件夹"+path+"失败！");
            }

            codeMapWriter=new BufferedWriter (new OutputStreamWriter (new FileOutputStream (path,false), StandardCharsets.UTF_8));
            codeMapWriter.write(JSON.toJSONString(codeMap));
        }finally{
            if(codeMapWriter!=null)
                codeMapWriter.close();
        }


    }

    public String getNumRegular() {
        return numRegular;
    }

    public void setNumRegular(String numRegular) {
        this.numRegular = numRegular;
    }

    private String getFileText(File file) throws IOException{
        BufferedReader reader = null;
        StringBuilder sbf = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
