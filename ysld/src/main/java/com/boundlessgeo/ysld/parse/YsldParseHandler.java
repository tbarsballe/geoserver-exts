package com.boundlessgeo.ysld.parse;

public class YsldParseHandler extends YamlParseHandler {

    protected Factory factory;

    protected YsldParseHandler(Factory factory) {
        this.factory = factory;
    }
}
