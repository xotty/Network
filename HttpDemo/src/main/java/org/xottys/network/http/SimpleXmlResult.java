/**
 * Retrofit方法中用到的数据类，便于用simpleXML转换工厂直接封装或解析
 * <p>
 * <br/>Copyright (C), 2017-2018, Steve Chang
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name:Account
 * <br/>Date:July，2017
 * @author xottys@163.com
 * @version 1.0
 */
package org.xottys.network.http;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name = "response",strict = false)
public class SimpleXmlResult {

    @Element(name = "version")
    @Path("head")
    private String version;

    @Element(name = "dateTime")
    @Path("head")
    private String dateTime;

    @Element(name = "code")
    @Path("body")
    private String code;

    @Element(name = "info")
    @Path("body")
    private String info;

    //要有空参构造
    public SimpleXmlResult(){}

    //setter&getter
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}

